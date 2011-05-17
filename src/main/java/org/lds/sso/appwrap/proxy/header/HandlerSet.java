package org.lds.sso.appwrap.proxy.header;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The holder of header handlers and any contingent handlers for a packet
 * passing through the proxy.
 * 
 * @author BoydMR
 * 
 */
public class HandlerSet {
    
    /**
     * Hidden constructor so that we are the only ones that can create it.
     */
    private HandlerSet() {
    }
    
    /**
     * The set of configured header handles that does not change from request
     * to request.
     */
    public List<HeaderHandler> handlers;
    
    /**
     * A set of header handlering actions that are specific to a request or
     * response passing through the proxy but that cannot be applied until after
     * all headers have been loaded from the stream. For example, the connection
     * header is meant for application on a single http connection between two
     * endpoint such as the proxy and the client or between the proxy and the
     * server. As such it must be removed before the http packet continues
     * onward. But its tokens specify other headers on whom the proxy is
     * supposed to act often removing them from the payload as well. The need of
     * removing such a header is fulfilled via instances of
     * {@link SecondPhaseAction} that are executed after all headers have been
     * loaded.
     */
    public List<SecondPhaseAction> secondPhaseActions = new ArrayList<SecondPhaseAction>(); 
    
    /**
     * Returns a set suitable for application to an http request passing through
     * the proxy.
     * @return
     */
    public static final HandlerSet newInstance() {
        HandlerSet set = new HandlerSet();
        set.handlers = SetsHolder.handlers;
        return set;
    }
    
    ///////////////// defined set of header handlers /////////////////////
    /**
     * Implementation of Lazy Holder Idiom allowing oneshot loading without
     * synchronization overhead due to class loader synchronization being used.
     */
    private static final class SetsHolder {
        public static List<HeaderHandler> handlers = (List<HeaderHandler>) loadSet();

        private static List<HeaderHandler> loadSet() {
            List<HeaderHandler> l = new ArrayList<HeaderHandler>();
            // fyi: order is irrelevant, I just like grouping them together
            // request ONLY handlers
            l.add(new ReqSessionCookieHandler());
            l.add(new ReqShimHeaderHndlr());
            l.add(new ReqHostHeaderHndlr());
            
            // request AND response handlers
            l.add(new ContentLengthHandler());

            // response ONLY handlers
            l.add(new RespCookieRewriteHndlr());
            l.add(new RespLocationRewriteHndlr());
            
            return Collections.unmodifiableList(l);
        } 
    }
}
