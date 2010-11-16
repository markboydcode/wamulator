package org.lds.sso.appwrap.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;

/**
 * Authenticates a user.
 * 
 * Requires query parameters of username and password in clear text and returns
 * an http body content of "token.id=the.id.of.the.token". If authentication fails 
 * it returns a response code of 401 and a message indicating an invalid username
 * or password was used.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class AuthNHandler extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(AuthNHandler.class);

	public AuthNHandler(String pathPrefix) {
		super(pathPrefix);
	}

	@SuppressWarnings("deprecation")
    @Override
	protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException {
		/**
		 * Get the current config instance each time which allows for reconfig
		 * of config object without restarting the service.
		 */
		Config cfg = Config.getInstance();

		String usr = request.getParameter("username");
		String pwd = request.getParameter("password");
		boolean isValidUser = cfg.getUserManager().isValidPassword(usr, pwd);
		
		if (isValidUser) {
			String token = cfg.getSessionManager().generateSessionToken(usr);

			if (cfg.getTrafficRecorder().isRecordingRest()) {
				Map<String,String> props = new HashMap<String,String>();
				props.put("username", usr);
				props.put("password", pwd);
				cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
						HttpServletResponse.SC_OK, " token.id=" + token, 
						props);
			}
			super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "token.id=" + token);
		}
		else {
			if (cfg.getTrafficRecorder().isRecordingRest()) {
				Map<String,String> props = new HashMap<String,String>();
				props.put("username", usr);
				props.put("password", pwd);
				cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
						HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.", 
						props);
			}
			super.sendResponse(cLog, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
		}
	}
}
