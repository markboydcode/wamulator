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
     * consist of injecting a Header object into the package's HeaderBuffer so
     * that the header or some derivative may ultimately continue onward in the
     * http message. Or an instance of {@link SecondPhaseAction} can be injected
     * into the {@link HandlerSet} so take action after all headers have been
     * loaded into the package's {@link HeaderBuffer}. Or action can take the
     * form of altering the package in some way.
     * 
     * @param lcHeaderName
     *            lowercase header name value without terminating colon
     * @param headerValue
     *            the value of the header
     * @param pkg
     *            the {@link HttpPackage} for the message
     * @param hSet
     *            the {@link HandlerSet} for capturing delayed actions
     * @param cfg
     *            the {@link Config} configuration for the simulator
     */
    public void handle(String lcHeaderName, String headerValue, HttpPackage pkg, 
            HandlerSet hSet, Config cfg);
    
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
