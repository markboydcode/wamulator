package org.lds.sso.appwrap.proxy.header;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.HttpPackageType;

/**
 * Watches for the wamulator's cookie on incoming requests so that traffic can
 * show as associated with a user or not.
 *  
 * @author BoydMR
 *
 */
public class ReqSessionCookieHandler implements HeaderHandler {

    public Header handle(String lcHeaderName, String headerValue,
            HttpPackage pkg, Config cfg) {
        String cookieVal = headerValue.trim();
        // if multiple cookies passed make sure we get the one for our sessions
        if (!pkg.cookieFound && cookieVal.contains(cfg.getCookieName()+ "=")) {
            pkg.cookiesHdr = cookieVal;
            pkg.cookieFound = true;
        }
        return new Header(HeaderDef.Cookie, cookieVal);
    }

    public boolean appliesTo(String lcHeaderName, HttpPackageType reqType) {
        return reqType == HttpPackageType.REQUEST
        && HeaderDef.Cookie.getLcName().equals(lcHeaderName) ;
    }

}
