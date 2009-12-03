package org.lds.sso.appwrap.rest;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URISyntaxException;

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

	public AuthZHandler(String pathPrefix) {
		super(pathPrefix);
	}

	@Override
	protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException {
		/**
		 * Get the current config instance each time which allows for reconfig
		 * of config object without restarting the service.
		 */
		Config cfg = Config.getInstance();

		String uri = request.getParameter("uri");
		if (uri == null || uri.equals("")) {
			super.sendResponse(cLog, response, HttpServletResponse.SC_UNAUTHORIZED, "Missing required uri parameter");
			return;
		}

        boolean isUnenforced = false;
        try {
            isUnenforced = cfg.getTrafficManager().isUnenforced(uri);
        } catch (URISyntaxException e) {
        	cLog.error("Unable to parse uri " + uri 
        		+ " to determine if it is in the unenforced list." 
        		+ " Treating as an enforced URI.", e);
        }

        if (isUnenforced) {
			super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "boolean=true");
			return;
		}
		
		// so check for session and allowed access
		
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
		
		String action = request.getParameter("action") == null ? "GET" : request.getParameter("action");

        boolean allowed = false;
        try {
            allowed = cfg.getTrafficManager().isPermitted(action, uri);
        } catch (URISyntaxException e) {
        	cLog.error("Unable to parse uri " + uri 
            		+ " to determine if access is allowed." 
            		+ " Denying access.", e);
        }
        
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "boolean=" 
				+ allowed);
	}
}
