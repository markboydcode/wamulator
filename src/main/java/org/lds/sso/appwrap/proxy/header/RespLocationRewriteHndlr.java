package org.lds.sso.appwrap.proxy.header;

import java.util.logging.Logger;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.TrafficManager;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.HttpPackageType;

/**
 * Rewrites response Location redirect headers if specified by configuration and 
 * injects a Location-WAS header with the original value to assist in 
 * troubleshooting.
 *  
 * @author BoydMR
 *
 */
public class RespLocationRewriteHndlr implements HeaderHandler {
    private static final Logger cLog = Logger.getLogger(RespLocationRewriteHndlr.class.getName());

    public void handle(String lcHeaderName, String headerValue,
            HttpPackage pkg, HandlerSet hSet, Config cfg) {
        TrafficManager mgr = cfg.getTrafficManager();
        String rewrite = mgr.rewriteRedirect(headerValue);
        
        if (rewrite != null) {
            // rewrite matched, replace
            LogUtils.fine(cLog, "rewriting redirect from: {0} to: {1}", headerValue, rewrite);
            pkg.headerBfr.append(new Header(HeaderDef.Location, rewrite));
            pkg.headerBfr.append(new Header(HeaderDef.Location.getName() + "-WAS",
                    headerValue));
        }
        else {
            pkg.headerBfr.append(new Header(HeaderDef.Location, headerValue));
        }
    }

    /**
     * Answers true if the package is a response package and the header name 
     * passed in is "Location" ignoring case.
     */
    public boolean appliesTo(String lcHeaderName, HttpPackageType reqType) {
        return reqType == HttpPackageType.RESPONSE 
        && HeaderDef.Location.getLcName().equals(lcHeaderName);
    }

}
