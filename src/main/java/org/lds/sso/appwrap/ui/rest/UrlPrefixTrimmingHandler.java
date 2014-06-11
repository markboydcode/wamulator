package org.lds.sso.appwrap.ui.rest;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handler that trims the incoming http console root from the target in the http request when passing to the
 * native ResourceHandler implementation so that static resource paths in physical space are relative to
 * the URL without having to have containing directories matching the console prefix parts.
 *
 * Created by markboyd on 5/16/14.
 */
public class UrlPrefixTrimmingHandler extends AbstractHandler {

    private final String prefix;
    private final ResourceHandler resourceHandler;

    public UrlPrefixTrimmingHandler(String prefix, ResourceHandler resourceHandler) {
        this.prefix = prefix;
        this.resourceHandler = resourceHandler;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        if (baseRequest.isHandled()) {
            return;
        }
        // strip prefix then restore after static handling
        String consoleRelativePath = target.substring(prefix.length());
        baseRequest.setPathInfo(consoleRelativePath);
        this.resourceHandler.handle(consoleRelativePath, baseRequest, request, response);
        baseRequest.setPathInfo(target);
    }
}
