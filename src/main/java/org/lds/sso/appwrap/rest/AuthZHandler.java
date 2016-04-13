package org.lds.sso.appwrap.rest;

import org.eclipse.jetty.server.Request;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.identity.User;
import org.lds.sso.appwrap.io.LogUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handler that can answer if a user is granted a specific action on a specific
 * uri.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class AuthZHandler extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(AuthZHandler.class.getName());

	public AuthZHandler(String pathPrefix) {
		super(pathPrefix);
	}

	@SuppressWarnings("deprecation")
    @Override
	protected void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		/**
		 * Get the current config instance each time which allows for reconfig
		 * of config object without restarting the service.
		 */
		Config cfg = Config.getInstance();

		String uri = request.getParameter("uri");
		if (uri == null || uri.equals("")) {
			if (cfg.getTrafficRecorder().isRecordingRest()) {
				Map<String, String> props = new HashMap<String, String>();
				cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
						HttpServletResponse.SC_UNAUTHORIZED, "Missing required uri parameter",
						props);
			}
			super.sendResponse(cLog, response, HttpServletResponse.SC_UNAUTHORIZED, "Missing required uri parameter");
			return;
		}
		String action = request.getParameter("action") == null ? "GET" : request.getParameter("action");
		boolean isUnenforced = false;
        try { 
            isUnenforced = cfg.getTrafficManager().isUnenforced(action, uri);
        } catch (URISyntaxException e) {
        	LogUtils.severe(cLog, "Unable to parse uri {0} to determine if it is in the unenforced list.  Treating as an enforced URI.", e, uri);
        }

        if (isUnenforced) {
			if (cfg.getTrafficRecorder().isRecordingRest()) {
				Map<String, String> props = new HashMap<String, String>();
				props.put("uri", uri);
				cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
						HttpServletResponse.SC_OK, "boolean=true",
						props);
			}
			super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "boolean=true");
			return;
		}
		
		// so check for session and allowed access
		
		String cookieHdr = request.getHeader("Cookie");
		
		if (cookieHdr == null) {
			if (cfg.getTrafficRecorder().isRecordingRest()) {
				Map<String, String> props = new HashMap<String, String>();
				props.put("uri", uri);
				props.put("cookie", "missing");
				cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
						HttpServletResponse.SC_UNAUTHORIZED, "Token Expired, Invalid Session",
						props);
			}
			super.sendResponse(cLog, response, HttpServletResponse.SC_UNAUTHORIZED, "Token Expired, Invalid Session");
			return;
		}
		String sessionToken = cfg.getTokenFromCookie(cookieHdr);
		String username = cfg.getUsernameFromToken(sessionToken);
		User user = cfg.getUserManager().getUser(username);
		
		if (user == null || sessionToken == null || ! cfg.getSessionManager().isValidToken(sessionToken)) {
			if (cfg.getTrafficRecorder().isRecordingRest()) {
				Map<String, String> props = new HashMap<String, String>();
				props.put("uri", uri);
				props.put("cookie", sessionToken);
				cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
						HttpServletResponse.SC_UNAUTHORIZED, "Token Expired, Invalid Session",
						props);
			}
			super.sendResponse(cLog, response, HttpServletResponse.SC_UNAUTHORIZED, "Token Expired, Invalid Session");
			return;
		}
		String rawSubTk = request.getParameter("subjectid");
		
		if (rawSubTk == null) {
			if (cfg.getTrafficRecorder().isRecordingRest()) {
				Map<String, String> props = new HashMap<String, String>();
				props.put("uri", uri);
				props.put("cookie", sessionToken);
				props.put("subjectid", "missing");
				cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
						HttpServletResponse.SC_OK, "boolean=false",
						props);
			}
			super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "boolean=false");
			return;
		}
		String subTk = URLDecoder.decode(rawSubTk, "utf-8");
		boolean isValid = cfg.getSessionManager().isValidToken(subTk);
		
		if (! isValid) {
			if (cfg.getTrafficRecorder().isRecordingRest()) {
				Map<String, String> props = new HashMap<String, String>();
				props.put("uri", uri);
				props.put("cookie", sessionToken);
				props.put("subjectid", subTk);
				cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
						HttpServletResponse.SC_UNAUTHORIZED, "Subject Token Expired, Invalid Session",
						props);
			}
			super.sendResponse(cLog, response, HttpServletResponse.SC_UNAUTHORIZED, "Subject Token Expired, Invalid Session");
			return;
		}
		
        boolean allowed = false;
        try {
            allowed = cfg.getTrafficManager().isPermitted(action, uri, user);
        } catch (URISyntaxException e) {
        	LogUtils.severe(cLog, "Unable to parse uri {0} to determine if access is allowed.  Denying access.", e, uri);
        }
        
		if (cfg.getTrafficRecorder().isRecordingRest()) {
			Map<String, String> props = new HashMap<String, String>();
			props.put("uri", uri);
			props.put("action", action);
			props.put("cookie", sessionToken);
			props.put("subjectid", subTk);
			cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
					HttpServletResponse.SC_OK, "boolean=" + allowed,
					props);
		}
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "boolean=" 
				+ allowed);
	}
}
