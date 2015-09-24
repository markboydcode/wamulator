package org.lds.sso.appwrap.certs;

import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.lds.sso.appwrap.Config;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Code used to generate the CA Certificate for the ldapWS service. Typical console 
 * output is:
 * <pre>
 * generating CA keypair
 * CA public key info:
 *   algorithm = RSA
 *   X.509
 * CA private key info:
 *   algorithm = RSA
 *   PKCS#8
 * generating CA certificate
 * storing CA certificate
 * done
 * </pre>
 * 
 * @author BoydMR
 *
 */
public class CaCertificateGenerator {
    
    public static void main(String[] args) throws CertificateEncodingException,
            InvalidKeyException, IllegalStateException,
            NoSuchAlgorithmException, SignatureException, IOException {
        System.out.println("generating CA keypair");
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        keygen.initialize(2048);
        KeyPair keypair = keygen.generateKeyPair();

        System.out.println("CA public key info:");
        PublicKey pubk = keypair.getPublic();
        System.out.println("  algorithm = " + pubk.getAlgorithm());
        System.out.println("  " + pubk.getFormat());
        
        System.out.println("CA private key info:");
        PrivateKey prvk = keypair.getPrivate();
        System.out.println("  algorithm = " + prvk.getAlgorithm());
        System.out.println("  " + prvk.getFormat());
        
        System.out.println("generating CA certificate");
        
        int days = 20 * 365;
        X500Principal caId = new X500Principal("CN=WAMulator-CA, OU="
                + Config.CERT_ORG_UNIT + ", O=LDS Church");
        
        X509V3CertificateGenerator gen = new X509V3CertificateGenerator();
        gen.addExtension(X509Extensions.BasicConstraints, true,
                new BasicConstraints(0));
        gen.addExtension(X509Extensions.KeyUsage, true,
                new KeyUsage(KeyUsage.keyCertSign));
        gen.addExtension(X509Extensions.ExtendedKeyUsage, true,
                new ExtendedKeyUsage(KeyPurposeId.anyExtendedKeyUsage));
        
        gen.setIssuerDN(caId);
        gen.setNotBefore(new Date());
        gen.setNotAfter(new Date(System.currentTimeMillis()
                + TimeUnit.MILLISECONDS.convert(days, TimeUnit.DAYS)));
        gen.setSerialNumber(new BigInteger("" + System.currentTimeMillis()));
        gen.setSignatureAlgorithm("SHA512withRSA");
        gen.setSubjectDN(caId);
        gen.setPublicKey(keypair.getPublic());
        X509Certificate cert = gen.generate(keypair.getPrivate());
        
        System.out.println("storing CA certificate");
        FileWriter out = new FileWriter(new File("ca-x509-cert.pem"));
        PEMWriter writer = new PEMWriter(out);
        writer.writeObject(cert);
        writer.flush();
        writer.close();
        
        out = new FileWriter(new File("ca-pkcs8-key.pem"));
        writer = new PEMWriter(out);
        writer.writeObject(keypair.getPrivate());
        writer.writeObject(cert);
        writer.flush();
        writer.close();
    }
}
