package org.lds.sso.appwrap.identity.ldap;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * A TrustManager instance that trusts any certificate.
 * 
 * @author BoydMR
 *
 */
public class TrustAllManager implements X509TrustManager {

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

}
