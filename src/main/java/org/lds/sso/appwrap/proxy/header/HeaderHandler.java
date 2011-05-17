package org.lds.sso.appwrap.proxy.header;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.HttpPackageType;

/**
 * Implementations determine in what form if any a specific header is passed through
 * the proxy or has some other effect.
 */
public interface HeaderHandler {

    /**
     * Implementation of some way of handling a specific header line during the
     * first pass over the raw headers in a request or response. Such action can
     * consist of returning a Header object to be included in the package as it
     * passes into the second pass scan for contingent handling of headers and
     * if left unmolested to pass onward with the package. Or action can take
     * the form of altering the package in some way and returning null meaning
     * that the header will not pass back onto the wire and onward to the
     * destination.
     * 
     * @param line
     * @param pkg
     */
    public Header handle(String lcHeaderName, String headerValue, HttpPackage pkg, Config cfg);
    
    /**
     * Returns true if a handler is for the indicated header name and its handle
     * method should be called regardless of whether or not that call will 
     * result in the return of a Header object. ie: some headers may have impact
     * on the process but not return a Header object to be passed onward. Thus
     * we can't rely upon that return value as an indicator of whether we have
     * found the handler for a given header line or not.
     *   
     * @param lcHeaderName
     * @param reqType
     * @return
     */
    public boolean appliesTo(String lcHeaderName, HttpPackageType reqType);
}
