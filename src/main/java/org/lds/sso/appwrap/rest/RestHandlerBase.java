package org.lds.sso.appwrap.rest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.proxy.RequestHandler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;
import org.omg.CORBA.portable.ResponseHandler;

/**
 * The abstract base handler for all rest handlers providing the path mapping 
 * capability executing the doHandle abstract method only if the indicated path
 * prefix matches the start of the URL currently being targeted.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public abstract class RestHandlerBase extends AbstractHandler {

	protected String pathPrefix = null;
	protected Config cfg = null;
	
	public RestHandlerBase(String pathPrefix, Config cfg) {
        if (pathPrefix == null || pathPrefix.equals("") || !pathPrefix.startsWith("/")) {
            throw new IllegalArgumentException("The path prefix can not be null or an empty string and must start with '/'.");
        }
		this.pathPrefix = pathPrefix.toLowerCase();
		this.cfg = cfg;
	}
	
	/**
	 * Implements the path filtering feature of this base class delegating to 
	 * doHandle if the target starts with the configured pathPrefix and then 
	 * marking the request as having been handled.
	 */
	public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException, ServletException {
        if (!((Request) request).isHandled()
                && target.toLowerCase().startsWith(this.pathPrefix)) {
        	doHandle(target, request, response, dispatch);
            ((Request) request).setHandled(true);
        }
	}
	
	/**
	 * Convenience method for sending the response code and message and logging
	 * the action.
	 * 
	 * @param logger
	 * @param response
	 * @param code
	 * @param resp
	 * @throws IOException
	 */
	protected void sendResponse(Logger logger, HttpServletResponse response, int code, String resp) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug(pathPrefix + " returning " + code + " '" + resp + "'");
		}
		response.setContentType("text/plain");
		response.setContentLength(resp.length());
		response.setHeader("Content-Language", "en-US");
		response.setHeader("Date", RequestHandler.getCurrentDateHeader());
		response.getWriter().println(resp);
		response.setStatus(code);

	}

	/**
	 * Must be implemented by sub-classes to provide the functionality of the
	 * handler.
	 * 
	 * @param target
	 * @param request
	 * @param response
	 * @param dispatch
	 * @throws IOException 
	 */
	protected abstract void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException;
}
