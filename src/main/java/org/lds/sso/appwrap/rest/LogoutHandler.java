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

	public LogoutHandler(String pathPrefix, Config cfg) {
		super(pathPrefix, cfg);
	}

	@Override
	protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException {
		String rawT = request.getParameter("token");
		String token = URLDecoder.decode(rawT, "utf-8");
		cfg.getSessionManager().terminateSession(token);
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "session was terminated");
	}
}
