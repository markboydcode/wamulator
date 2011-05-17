package org.lds.sso.appwrap.proxy.header;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.HttpPackageType;

/**
 * Watches for the http Host header and sets various values on the package as
 * well as passing the header onward.
 *  
 * @author BoydMR
 *
 */
public class ReqHostHeaderHndlr implements HeaderHandler {

    public Header handle(String lcHeaderName, String headerValue,
            HttpPackage pkg, Config cfg) {
        pkg.hostHdr = headerValue;
        Header h = new Header(HeaderDef.Host, headerValue);
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
        return h;
    }

    public boolean appliesTo(String lcHeaderName, HttpPackageType reqType) {
        return reqType == HttpPackageType.REQUEST
        && HeaderDef.Host.getLcName().equals(lcHeaderName) ;
    }

}
