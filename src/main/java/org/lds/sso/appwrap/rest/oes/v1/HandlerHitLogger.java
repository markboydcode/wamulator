package org.lds.sso.appwrap.rest.oes.v1;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.rest.RestHandlerBase;
import org.mortbay.jetty.Request;

/**
 * Handler to wrap another handler and log and error message whenever a request
 * for that handler is received.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class HandlerHitLogger extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(HandlerHitLogger.class.getName());
    private RestHandlerBase wrapped;
    private String logMessage;

	public HandlerHitLogger(RestHandlerBase wrapped, String logMessage) {
	    super("/unused/path");
		this.wrapped = wrapped;
		this.logMessage = logMessage;
	}

	   /**
     * Implements the path filtering feature of this base class delegating to 
     * doHandle if the target starts with the configured pathPrefix and then 
     * marking the request as having been handled.
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
            throws IOException, ServletException {
        if (!((Request) request).isHandled()
                && target.toLowerCase().startsWith(wrapped.getPathPrefix())) {
            LogUtils.severe(cLog, logMessage);
            wrapped.handle(target, request, response, dispatch);
        }
    }

    @Override
    protected void doHandle(String target, HttpServletRequest request,
            HttpServletResponse response, int dispatch) throws IOException {
        // unused but mandatory 
    }
}
