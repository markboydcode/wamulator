package org.lds.sso.appwrap.proxy.tls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.proxy.ListenerCoordinator;
import org.lds.sso.appwrap.proxy.RequestHandler;

/**
 * Version of ProxyListener for HTTP over TLS with auto-generated certificate
 * specific to the declared cert-host from the configuration file.
 * 
 * @author BoydMR
 *
 */
public class HttpsProxyListener implements Runnable
{
	private static final Logger cLog = Logger.getLogger(HttpsProxyListener.class.getName());

	private ServerSocket server;
	private volatile boolean started = false;
	private Config cfg;
    private ListenerCoordinator coordinator;

	public HttpsProxyListener (Config cfg, ListenerCoordinator coord) throws IOException
	{
        this.coordinator = coord;
		this.cfg  = cfg;

		ServerSocketFactory ssf = setUpTlsServerSocketFactory();

        if (cfg.getProxyHttpsPort() == 0) {
            server = ssf.createServerSocket();
            server.setReuseAddress(true);
            server.bind(null);
            cfg.setProxyHttpsPort(server.getLocalPort());
        }
        else {
            server = ssf.createServerSocket(cfg.getProxyHttpsPort());
        }
        server.setSoTimeout(1000);
	}

    /* return whether or not the socket is currently open
	 */
	public boolean isRunning ()
	{
		if (server == null || !started)
			return false;
		else
			return true;
	}


	/* closeSocket will close the open ServerSocket; use this
	 * to halt a running jProxy thread
	 */
	public void stop ()
	{
		started = false;
		if(server != null) {
			while(!server.isClosed()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) { /* Do Nothing */ }
			}
			
		}
	}


	public void run()
	{
		try {
            // wait for coordinator to clean out old files
            coordinator.waitForFileCleanout();
            started = true;

            // loop forever listening for client connections

			while (started)
			{
				try {
					Socket client = server.accept();
					client.setSoTimeout(cfg.getProxyInboundSoTimeout());
                    String connId = coordinator.getConnectionId();
					RequestHandler h = new RequestHandler(client, cfg, connId, true);
					Thread t = new Thread(h, "RequestHandler " + connId);
					t.setDaemon(true);
					t.start();
				} catch(SocketTimeoutException e) {
					cLog.finest("Accept timed out.  let's try again.");
				}
			}
		} catch ( IOException e ) {
			if(!e.getMessage().contains("socket closed")) {
				LogUtils.warning(cLog, "HttpS Proxy Listener error.  Increase logging level to see full error.  If you're seeing this message during shutdown you can probably ignore it.");
				LogUtils.fine(cLog, "HttpS Proxy Listener error: ", e);
			} else {
				LogUtils.severe(cLog, "Unexpected error:", e);
			}
		} finally {
			try {
				if(server != null) {
					server.close();
				}
			} catch(Exception e) { /* Do nothing */ }
			started = false;
		}
	}

	/**
	 * Class for holding a private key and its related certificate which 
	 * contains the corresponding public key but packaged up to portray 
	 * identity.
	 * 
	 * @author BoydMR
	 *
	 */
    private static class KeyAndCert {
        public PrivateKey privateKey = null;
        public X509Certificate cert = null;
    }

   /**
     * Sets up our ServerSocket to user Transport Layer Security (TLS) using
     * an auto-generated, stored, private key and certificate. This must be 
     * done at runtime since we don't know what host should be identified by
     * the certificate until the configuration file has been parsed.
     */
    private ServerSocketFactory setUpTlsServerSocketFactory() {
        // Need to install bounceycastle for PEM file format reading/writing
        // and certificate generation.
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
        {
            cLog.log(Level.INFO, "No provider found for '" 
                    + BouncyCastleProvider.PROVIDER_NAME + "'. Installing...");
            Security.addProvider(new BouncyCastleProvider());
        }

        try {
            /*
             * first see if we already have a public & private key pair
             * stored locally. Private key file has name pattern of
             * proxy-tls's cert-host value with asterisks replaced by
             * '__' and the whole appended with '-cert.pem'.
             * ex: '*.lds.org' --> '__.lds.org-cert.pem'
             * ex: 'local.mormon.org' --> 'local.mormon.org-cert.pem'
             * 
             */
            
            // first load ca cert and private key
            KeyAndCert caKac = loadCaKeyAndCert();
            
            // now see if we already have generated cert/prvt key for site
            // and load, else generate new and store
            String certHost = cfg.getProxyHttpsCertHost();
            String base = certHost.replaceAll("\\*", "__");
            String stCertFlNm = base + "-cert.pem";
            String stPrKyFlNm = base + "-priv.pem";
            KeyAndCert siteKac = loadSiteKeyAndCertFromFile(stCertFlNm, stPrKyFlNm);
            
            if (siteKac == null) {
                siteKac = generateAndStoreSiteKeyAndCert(certHost, caKac, stCertFlNm, stPrKyFlNm);
            }
            // now put the private key and the public certificate into the form
            // needed by the ssl/tls layer for acquiring them during the TLS
            // handshake; namely an X509KeyManager.
            X509Certificate[] siteCertChain = new X509Certificate[] {siteKac.cert};
            PrivateKey sitePrvKy = siteKac.privateKey;
            FixedTypeKeyManager xkmgr = new FixedTypeKeyManager("RSA", sitePrvKy, siteCertChain);
            
            X509KeyManager[] kmgrs = new X509KeyManager[] {xkmgr};
            // now create a trust manager for answering if we trust the client
            // which we do for all clients since we aren't using mutual auth.
            TrustManager[] tMgrAry = new TrustManager[] { new TrustAllManager() };
            // now set up SSLContext for TLS, get related factory, then serversocket
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(kmgrs, tMgrAry, null);
            return sslcontext.getServerSocketFactory();
        }
        catch (Exception e) { // lots to catch here
            throw new RuntimeException("Unable to set up TLS Server Socket Factory.", e);
        }
    }

    /**
     * Generates a host specific certificate for the passed-in site public and private
     * keys and signed by the passed-in CA certificate.
     * 
     * @param host
     * @param caPrvKy
     * @param caCert
     * @param pubKy
     * @param prvKy
     * @return
     * @throws CertificateEncodingException 
     * @throws InvalidKeyException
     * @throws IllegalStateException
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws IOException
     */
    private KeyAndCert genHostCert(String host, KeyAndCert caKac, 
            RSAPublicKey pubKy, RSAPrivateKey prvKy) {
        KeyAndCert kac = new KeyAndCert();
        kac.privateKey = prvKy;

        X509Name xname = new X509Name("CN=" + host
                + ", OU=" + Config.CERT_ORG_UNIT + ", O=LDS Church, ST=UT, C=US");
        Calendar cal = Calendar.getInstance();
        
        // set cert to expire in 5 years. we auto regenerate if we ever 
        // encounter an expired one.
        cal.add(Calendar.YEAR, 5); 
        X509V3CertificateGenerator g = new X509V3CertificateGenerator();

        g.setSerialNumber(java.math.BigInteger.valueOf(java.lang.System
                .currentTimeMillis()));
        g.setIssuerDN(caKac.cert.getIssuerX500Principal());
        g.setSubjectDN(xname);
        g.setPublicKey(pubKy);
        g.setNotBefore(new Date());
        g.setNotAfter(cal.getTime());
        g.setSignatureAlgorithm("SHA1WITHRSA");
        
        try {
            kac.cert = g.generate(caKac.privateKey, BouncyCastleProvider.PROVIDER_NAME);
        }
        catch(Exception e) {
            throw new IllegalStateException("Unable to generate site "
                    + "certificate for " + host, e);
        }
        return kac;
    }

    /**
     * Generates a public/private key pair, then generates a certificate signed
     * by the passed-in CA, then stores both locally so that they can be found
     * next time we start up.
     * 
     * @param proxyHttpsCertHost
     * @param caKac
     * @param stCertFlNm
     * @param stPrKyFlNm
     * @return 
     */
    private KeyAndCert generateAndStoreSiteKeyAndCert(String certHost,
            KeyAndCert caKac, String stCertFlNm, String stPrKyFlNm) {
        // first generate a public & private key pair using RSA
        KeyPairGenerator rsaKPGen;
        try {
            rsaKPGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to generate site " 
                    + "public/private key pair.", e);
        }
        rsaKPGen.initialize(1024);

        KeyPair clientCertKeypair = rsaKPGen.generateKeyPair();
        RSAPublicKey pubKy = (RSAPublicKey) clientCertKeypair.getPublic();
        RSAPrivateKey prvKy = (RSAPrivateKey) clientCertKeypair.getPrivate();

        // now generate our site certificate
        KeyAndCert siteKac = new KeyAndCert();
        siteKac = genHostCert(certHost, caKac, pubKy, prvKy);
        storeToFile(siteKac, stCertFlNm, stPrKyFlNm);
        // store to file
        return siteKac;
    }

    /**
     * Stores the certificate and private key in PEM format into the indicated
     * files.
     * 
     * @param siteKac
     * @param stCertFlNm
     * @param stPrKyFlNm
     */
    private void storeToFile(KeyAndCert siteKac, String stCertFlNm,
            String stPrKyFlNm) {
        File file = new File(stCertFlNm);
        cLog.info("storing site certificate in " + file.getAbsolutePath());
        try {
            FileWriter out = new FileWriter(file);
            PEMWriter writer = new PEMWriter(out);
            writer.writeObject(siteKac.cert);
            writer.flush();
            writer.close();
        }
        catch(IOException ioe) {
            cLog.log(Level.WARNING, "Unable to store site certificate into " 
                    + file.getAbsolutePath(), ioe);
        }

        file = new File(stPrKyFlNm);
        cLog.info("storing site private key in " + file.getAbsolutePath());
        try {
            FileWriter out = new FileWriter(file);
            PEMWriter writer = new PEMWriter(out);
            writer.writeObject(siteKac.privateKey);
            writer.writeObject(siteKac.cert);
            writer.flush();
            writer.close();
        }
        catch(IOException ioe) {
            cLog.log(Level.WARNING, "Unable to store site private key into " 
                    + file.getAbsolutePath(), ioe);
        }
    }

    /**
     * Load our CA certificate and private key as found in src/main/resources
     * ca-cert.pem and ca-private-key.pem files in PEM format.
     * 
     * @return
     */
    private KeyAndCert loadCaKeyAndCert() {
        KeyAndCert kac = new KeyAndCert();
        URL url = this.getClass().getClassLoader().getResource("ca-cert.pem");

        if (url == null) {
            throw new IllegalStateException("Missing classpath resource ca-cert.pem file.");
        }
        try {
            InputStream certIn = (InputStream) url.getContent();
            byte[] bytes = new byte[certIn.available()];
            certIn.read(bytes);
            certIn.close();
            String caCertPem = new String(bytes);
            PEMReader reader = new PEMReader(new StringReader(caCertPem));
            kac.cert = (X509Certificate) reader.readObject();
        } catch(IOException ioe) {
            throw new IllegalStateException("Unable to load classpath resource ca-cert.pem file.");
        }
        try {
        url = this.getClass().getClassLoader().getResource("ca-private-key.pem");

        if (url == null) {
            throw new IllegalStateException("Missing classpath resource ca-private-key.pem file.");
        }
            InputStream prvKyIn = (InputStream) url.getContent();
            byte[] bytes = new byte[prvKyIn.available()];
            prvKyIn.read(bytes);
            prvKyIn.close();
            String caPrvKyPem = new String(bytes);
            PEMReader reader = new PEMReader(new StringReader(caPrvKyPem));
            KeyPair caKp = (KeyPair) reader.readObject();
            kac.privateKey = caKp.getPrivate();
        } catch(IOException ioe) {
            throw new IllegalStateException("Unable to load classpath resource ca-private-key.pem file.");
        }
        return kac;
    }
    
    /**
     * Read the specified PEM formatted files from the current directory
     * and load their certificate and private key into the KeyAndCert
     * structure and return it or null if one or the other were not
     * available.
     * 
     * @param stCertFlNm
     * @param stPrKyFlNm
     * @return
     * @throws IOException
     */
    private KeyAndCert loadSiteKeyAndCertFromFile(String stCertFlNm,
            String stPrKyFlNm) throws IOException {
        KeyAndCert kac = new KeyAndCert();
        File fl = new File(stPrKyFlNm );
        
        if (fl.exists()) {
            try {
                FileInputStream prvKyIn = new FileInputStream(fl);
                byte[] bytes = new byte[prvKyIn.available()];
                prvKyIn.read(bytes);
                prvKyIn.close();
                String prvKyPem = new String(bytes);
                PEMReader reader = new PEMReader(new StringReader(prvKyPem));
                KeyPair kp = (KeyPair) reader.readObject();
                kac.privateKey = kp.getPrivate();
            }
            catch(IOException ioe) {
                cLog.log(Level.WARNING, "Unable to load site private key "
                        + fl.getAbsolutePath(), ioe);
            }
        }
        File f2 = new File(stCertFlNm );
        
        if (f2.exists()) {
            try {
                FileInputStream certIn = new FileInputStream(f2);
                byte[] bytes = new byte[certIn.available()];
                certIn.read(bytes);
                certIn.close();
                String certPem = new String(bytes);
                PEMReader reader = new PEMReader(new StringReader(certPem));
                kac.cert = (X509Certificate) reader.readObject();
                try {
                    kac.cert.checkValidity();
                }
                catch(CertificateExpiredException c) {
                    cLog.log(Level.WARNING, "Site certificate in " 
                            + f2.getAbsolutePath() + " expired. Re-creating...");
                    kac.cert = null; // force re-creation
                }
                catch(CertificateNotYetValidException c) {
                    cLog.log(Level.WARNING, "Weird! Site certificate in " 
                            + f2.getAbsolutePath() + " not yet valid. Re-creating...");
                    kac.cert = null; // force re-creation
                }
            }
            catch(IOException ioe) {
                cLog.log(Level.WARNING, "Unable to load site certificate "
                        + f2.getAbsolutePath(), ioe);
            }
        }
        return (kac.cert != null && kac.privateKey != null ? kac : null);
    }
}


