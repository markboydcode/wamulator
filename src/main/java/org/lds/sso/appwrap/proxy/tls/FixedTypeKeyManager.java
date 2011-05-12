package org.lds.sso.appwrap.proxy.tls;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509KeyManager;

public class FixedTypeKeyManager implements X509KeyManager {

    private String algType;
    private String keyAlias;
    private String[] keyAliases;
    private PrivateKey privateKey;
    private X509Certificate[] certChain;

    /**
     * Creates the key manager with the algorithm type that was specified to 
     * the KeyPairGenerator when the keys were created that are being returned
     * from this X509KeyManager. A KeyManager is used by the TLS Socket layer
     * during the TLS handshake to see what algorithms monikers passed from the
     * connecting client match the algorithms of our available keys with which 
     * we can participate in an encrypted connection.  
     * 
     * @param algorithmType
     */
    public FixedTypeKeyManager(String algorithmType, PrivateKey privateKey,
            X509Certificate[] publicCertChain) {
       this.algType = algorithmType;
       this.keyAlias = "" + System.currentTimeMillis();
       this.keyAliases = new String[] {this.keyAlias};
       this.privateKey = privateKey;
       this.certChain = publicCertChain;
    }
    
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        if (algType.equals(keyType)) {
            return keyAliases;
        }
        return null;
    }
    
    public PrivateKey getPrivateKey(String alias) {
        if (keyAlias.equals(alias)) {
            return this.privateKey;
        }
        return null;
    }
    
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return null;
    }
    
    public X509Certificate[] getCertificateChain(String alias) {
        if (keyAlias.equals(alias)) {
            return this.certChain;
        }
        return null;
    }
    
    public String chooseServerAlias(String keyType, Principal[] issuers,
            Socket socket) {
        if (algType.equals(keyType)) {
            return keyAlias;
        }
        return null;
    }
    
    public String chooseClientAlias(String[] keyType, Principal[] issuers,
            Socket socket) {
        return null;
    }
}
