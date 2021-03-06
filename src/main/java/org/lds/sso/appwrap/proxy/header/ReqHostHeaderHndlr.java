package org.lds.sso.appwrap.proxy.header;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.HttpPackageType;
import org.lds.sso.appwrap.proxy.StartLine;

/**
 * Watches for the http Host header and sets various values on the package as
 * well as passing the header onward.
 *  
 * @author BoydMR
 *
 */
public class ReqHostHeaderHndlr implements HeaderHandler {

    public void handle(String lcHeaderName, String headerValue,
            HttpPackage pkg, HandlerSet hSet, Config cfg) {
        pkg.hostHdr = headerValue;

        // inject so it will pass onward
        pkg.headerBfr.append(new Header(HeaderDef.Host, headerValue));
        StartLine sl = (StartLine) pkg.requestLine;
        
        /*
         * If start line had absolute req-uri then these values will have already
         * been set during processing of the start line for a request. Otherwise
         * we need to obtain them from the host header here.
         */
        if (! sl.isAbsReqURI()) {
            int colon = headerValue.indexOf(':');
            String host = null;

            if (colon == -1) {
                pkg.host = headerValue; // no port so is internet default: 80 
            }
            else {
                pkg.host = headerValue.substring(0, colon);
                String sPort = headerValue.substring(colon + 1);
                pkg.port = Integer.parseInt(sPort);
                pkg.hasNonDefaultPort = true;
            }
        }
    }

    public boolean appliesTo(String lcHeaderName, HttpPackageType reqType) {
        return reqType == HttpPackageType.REQUEST
        && HeaderDef.Host.getLcName().equals(lcHeaderName) ;
    }

}
