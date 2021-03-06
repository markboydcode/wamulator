package org.lds.sso.appwrap.proxy.header;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.HttpPackageType;

/**
 * Watches for Content-Length header and sets length in the package as well as
 * allowing the header to travel onward.
 * 
 * @author BoydMR
 *
 */
public class ContentLengthHandler implements HeaderHandler {

    public void handle(String lcHeaderName, String headerValue,
            HttpPackage pkg, HandlerSet hSet, Config cfg) {
        pkg.contentLength = Integer.parseInt(headerValue.trim());
 
        // inject so it will pass onward
        pkg.headerBfr.append(new Header(HeaderDef.ContentLength, headerValue));
    }

    /**
     * Answers true for either request or response for a header with name
     * "Content-Length" ignoring case.
     */
    public boolean appliesTo(String lcHeaderName, HttpPackageType reqType) {
        return HeaderDef.ContentLength.getLcName().equals(lcHeaderName);
    }

}
