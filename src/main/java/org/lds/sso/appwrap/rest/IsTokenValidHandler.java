package org.lds.sso.appwrap.rest;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;

/**
 * Handler that can answer if a token represents a currently active session or
 * not.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class IsTokenValidHandler extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(IsTokenValidHandler.class);

	public IsTokenValidHandler(String pathPrefix, Config cfg) {
		super(pathPrefix, cfg);
	}

	@Override
	protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException {
		String rawT = request.getParameter("token");
		String token = URLDecoder.decode(rawT, "utf-8");
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "boolean=" + cfg.getSessionManager().isValidToken(token));
	}
}
