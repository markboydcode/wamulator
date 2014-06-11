package org.lds.sso.appwrap.rest;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.lds.sso.appwrap.proxy.RequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

/**
 * The abstract base handler for all rest handlers providing the path mapping 
 * capability executing the doHandle abstract method only if the indicated path
 * prefix matches the start of the URL currently being targeted and the request
 * has not yet been handled.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public abstract class RestHandlerBase extends AbstractHandler {

    /**
     * The strategy to apply when testing incoming URLs against our prefix.
     */
    private Match strategy;

    /**
     * Match strategies.
     */
    public enum Match {
        STARTS_WITH,
        STARTS_WITH_CIS,
        EQUALS
    };

	protected String pathPrefix = null;
	protected String pathPrefixRaw = null;

    /**
     * Instantiate a RestHandlerBase with matching strategy of testing to see if the target starts with the
     * irrespective of case. (ie: case insensitive string match)
     *
     * @param pathPrefix
     */
	public RestHandlerBase(String pathPrefix) {
        validatePrefix(pathPrefix);
		this.pathPrefix = pathPrefix.toLowerCase();
		this.pathPrefixRaw = pathPrefix;
        this.strategy = Match.STARTS_WITH_CIS;
	}

    /**
     * Instantiate a RestHandlerBase with a specific matching strategy.
     *
     * @param pathPrefix
     */
    public RestHandlerBase(String pathPrefix, Match matchStrategy) {
        validatePrefix(pathPrefix);
        this.pathPrefixRaw = pathPrefix;
        this.strategy = matchStrategy;

        switch(strategy) {
            case STARTS_WITH_CIS:
                pathPrefix = pathPrefixRaw.toLowerCase();
                break;
            case STARTS_WITH:
            case EQUALS:
                pathPrefix = pathPrefixRaw;
                break;
        }
    }

    private void validatePrefix(String prefix) {
        if (prefix == null || prefix.equals("") || !prefix.startsWith("/")) {
            throw new IllegalArgumentException("The path prefix can not be null or an empty string and must start with '/'.");
        }
    }

	/**
	 * Implements the path filtering feature of this base class delegating to 
	 * doHandle if the target starts with the configured pathPrefix and then 
	 * marking the request as having been handled.

     * @param target The targer of the request either as a URI or a name.
     * @param baseRequest The original unwrapped Jetty request object.
     * @param request The request object or a wrapper of the original request.
     * @param response The response object or a wrapper of the original response.
	 */
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (!baseRequest.isHandled()) {
            boolean doIt = false;
            switch(strategy) {
                case STARTS_WITH_CIS:
                    doIt = target.toLowerCase().startsWith(this.pathPrefix);
                    break;
                case STARTS_WITH:
                    doIt = target.startsWith(this.pathPrefixRaw);
                    break;
                case EQUALS:
                    doIt = target.equals(this.pathPrefixRaw);
            }
            if (doIt) {
                doHandle(target, baseRequest, request, response);
                baseRequest.setHandled(true);
            }
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
		response.setContentType("text/plain");
		response.setContentLength(resp.length());
		response.setHeader("Content-Language", "en-US");
		response.setHeader("Date", RequestHandler.getCurrentDateHeader());
        response.setStatus(code);
		PrintWriter out = response.getWriter();
		out.println(resp);
		out.flush();
	}

	/**
	 * Must be implemented by sub-classes to provide the functionality of the
	 * handler.
	 * 
	 * @param target The targer of the request either as a URI or a name.
     * @param baseRequest The original unwrapped Jetty request object.
	 * @param request The request object or a wrapper of the original request.
	 * @param response The response object or a wrapper of the original response.
	 * @throws IOException
	 */
	protected abstract void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException;
}
