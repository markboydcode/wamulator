package org.lds.sso.appwrap.rest;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.AllowedUri;
import org.lds.sso.appwrap.Config;

/**
 * Handler that can answer if a user is granted a specific action on a specific
 * uri.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class AuthZHandler extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(AuthZHandler.class);

	public AuthZHandler(String pathPrefix, Config cfg) {
		super(pathPrefix, cfg);
	}

	@Override
	protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException {
		String cookieHdr = request.getHeader("Cookie");
		
		if (cookieHdr == null) {
			super.sendResponse(cLog, response, HttpServletResponse.SC_UNAUTHORIZED, "Token Expired, Invalid Session");
			return;
		}
		String sessionToken = cfg.getTokenFromCookie(cookieHdr);
		
		if (sessionToken == null || ! cfg.getSessionManager().isValidToken(sessionToken)) {
			super.sendResponse(cLog, response, HttpServletResponse.SC_UNAUTHORIZED, "Token Expired, Invalid Session");
			return;
		}
		String rawSubTk = request.getParameter("subjectid");
		
		if (rawSubTk == null) {
			super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "boolean=false");
			return;
		}
		String subTk = URLDecoder.decode(rawSubTk, "utf-8");
		boolean isValid = cfg.getSessionManager().isValidToken(subTk);
		
		if (! isValid) {
			super.sendResponse(cLog, response, HttpServletResponse.SC_UNAUTHORIZED, "Subject Token Expired, Invalid Session");
			return;
		}
		String action = request.getParameter("action");
		
		if (action == null) {
			action = "GET";
		}
		else if (! action.equals("GET") && ! action.equals("POST")) {
			super.sendResponse(cLog, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid action name: " + action);
			return;
		}
		String uri = request.getParameter("uri");
		if (uri == null || uri.equals("")) {
			super.sendResponse(cLog, response, HttpServletResponse.SC_UNAUTHORIZED, "Missing required uri parameter");
			return;
		}
		String username = cfg.getUsernameFromToken(subTk);
		URL url = new URL(uri);
		int port = url.getPort();
		if (port == -1) {
			port = 80;
		}
		String path = url.getPath() + (url.getQuery() == null ? "" : ("?" + url.getQuery()));
		boolean is = cfg.getTrafficManager().isUnenforced(uri)
		 	|| cfg.getUserManager().isPermitted(username, action, url.getHost(), port, path); 
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "boolean=" 
				+ is);
	}
}
