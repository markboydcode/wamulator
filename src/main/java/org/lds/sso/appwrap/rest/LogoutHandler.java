package org.lds.sso.appwrap.rest;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;

/**
 * Handles termination of a currently active session.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class LogoutHandler extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(LogoutHandler.class);

	public LogoutHandler(String pathPrefix) {
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

		/*
		 * Actually, to support real characteristics of opensso rest api we 
		 * should first look for the token from the cookie and if found use
		 * that token. If not found then fallback on the subjectid parameter.
		 */
		String rawT = request.getParameter("subjectid");
		if (rawT != null && ! "".equals(rawT) ) {
			String token = URLDecoder.decode(rawT, "utf-8");
			cfg.getSessionManager().terminateSession(token);
			super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "session was terminated");
		}
	}
}
