package org.lds.sso.appwrap.proxy.header;

import java.util.logging.Logger;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.TrafficManager;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.HttpPackageType;

/**
 * Watches for the wamulator's cookie on incoming requests so that traffic can
 * show as associated with a user or not.
 *  
 * @author BoydMR
 *
 */
public class RespCookieRewriteHndlr implements HeaderHandler {
    private static final Logger cLog = Logger.getLogger(RespCookieRewriteHndlr.class.getName());

    public void handle(String lcHeaderName, String headerValue,
            HttpPackage pkg, HandlerSet hSet, Config cfg) {
        String rawSetCookieHdrVal = headerValue.trim();
        TrafficManager mgr = cfg.getTrafficManager();
        String rewrite = rawSetCookieHdrVal;
        
        if (mgr.cookieRewritesExist()) {
            rewrite = mgr.rewriteCookiePath(rawSetCookieHdrVal);
        }

        if (rewrite.equals(rawSetCookieHdrVal)) {
            pkg.headerBfr.append(new Header(HeaderDef.SetCookie, rawSetCookieHdrVal));
        }
        else {
            pkg.headerBfr.append(new Header(HeaderDef.SetCookie, rewrite));
            // allow clients to see original via a set-cookie-WAS header
            pkg.headerBfr.append(new Header(HeaderDef.SetCookie.getName() + "-WAS",
            		rawSetCookieHdrVal));
            LogUtils.fine(cLog, "rewriting cookie from: {0} to: {1}", rawSetCookieHdrVal, rewrite);
        }
    }

    /**
     * Answers true if this is a response package and the header being handled
     * is "Set-Cookie" ignoring case.
     */
    public boolean appliesTo(String lcHeaderName, HttpPackageType reqType) {
        return reqType == HttpPackageType.RESPONSE
        && HeaderDef.SetCookie.getLcName().equals(lcHeaderName) ;
    }

}
