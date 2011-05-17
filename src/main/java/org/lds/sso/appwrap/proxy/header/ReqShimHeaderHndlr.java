package org.lds.sso.appwrap.proxy.header;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.HttpPackageType;

/**
 * Watches for the wamulator's header that indicates if this request came from
 * the wamulator itself meaning that an infinite redirect loop has accidentally
 * been configured and we indicate that in the package for handling elsewhere. 
 *  
 * @author BoydMR
 *
 */
public class ReqShimHeaderHndlr implements HeaderHandler {

    public Header handle(String lcHeaderName, String headerValue,
            HttpPackage pkg, Config cfg) {
        pkg.redirectLoopDetected = true;
        return new Header(lcHeaderName, headerValue);
    }

    public boolean appliesTo(String lcHeaderName, HttpPackageType reqType) {
        return reqType == HttpPackageType.REQUEST
        && HttpPackage.SHIM_HANDLED_HDR_NM.equalsIgnoreCase(lcHeaderName) ;
    }

}
