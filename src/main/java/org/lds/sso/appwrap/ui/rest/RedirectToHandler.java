package org.lds.sso.appwrap.ui.rest;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles redirecting request for one location to a specified new location.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2014, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class RedirectToHandler extends AbstractHandler {

    private final String destinationPath;
    private final String path;

    public RedirectToHandler(String path, String destPath) {
        this.path = path;
        this.destinationPath = destPath;
    }

    @Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

        if (!baseRequest.isHandled()
                && target.toLowerCase().equals(this.path)) {
            response.sendRedirect(this.destinationPath);
            ((Request) request).setHandled(true);
        }
	}
}
