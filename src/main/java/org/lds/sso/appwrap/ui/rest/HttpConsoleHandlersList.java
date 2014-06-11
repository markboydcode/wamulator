package org.lds.sso.appwrap.ui.rest;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Implementation of handler list that only lets requests into its contained list
 * if the URL starts with the console's URL base. Once a request passes into
 * this handler it won't pass to any sibling handlers since this class handles
 * unhandled requests with its own 404 handling.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2014, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class HttpConsoleHandlersList extends HandlerList {

    private final String prefix;

    public HttpConsoleHandlersList(String urlPrefix) {
        if (urlPrefix == null || urlPrefix.equals("") || !urlPrefix.startsWith("/")) {
            throw new IllegalArgumentException("The path prefix can not be null or an empty string and must start with '/'.");
        }
        this.prefix = urlPrefix;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
        if (!baseRequest.isHandled()
                && target.toLowerCase().startsWith(this.prefix)) {
            super.handle(target, baseRequest, request, response);
            if (!baseRequest.isHandled()) {
                response.setContentType("text/html; charset=utf-8");
                response.setHeader("cache-control", "no-cache, no-store");
                PrintWriter w = response.getWriter();
                w.println("<html>" +
                        "<head><title>Error 404</title></head>" +
                        "<body><h2>Http Error: 404</h2>" +
                        "<pre>WAMulator Http Console Resource not found: </pre>" +
                        "<block><i>" + request.getRequestURI() + "</i></block>" +
                        "</body>" +
                        "</html>");
                response.setStatus(404);
                w.flush();
                baseRequest.setHandled(true);
            }
        }
	}
}
