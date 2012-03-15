package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.identity.SessionManager;
import org.lds.sso.appwrap.rest.RestHandlerBase;

/**
 * Handles request to start a session for a user by setting a suitable
 * cookie.
 *  
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class TerminateSessionHandler extends RestHandlerBase {
    private static final Logger cLog = Logger.getLogger(TerminateSessionHandler.class.getName());

	public TerminateSessionHandler(String pathPrefix) {
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

		String uri = request.getRequestURI();
		String token = uri.substring(uri.lastIndexOf('/')+1);
		if (StringUtils.isNotEmpty(token)) {
	        SessionManager smgr = cfg.getSessionManager();
	        smgr.terminateSessionsForToken(token);
		}
		String referer = request.getHeader("referer");
		if (referer == null || referer.equals("")) {
		    referer = cfg.getLoginPage();
		}
		response.sendRedirect(referer);
	}

}
