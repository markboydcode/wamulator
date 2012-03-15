package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.identity.SessionManager;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.rest.RestHandlerBase;

/**
 * Handles request to start a session for a user by setting a suitable
 * cookie.
 *  
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class SelectSessionHandler extends RestHandlerBase {
    private static final Logger cLog = Logger.getLogger(SelectSessionHandler.class.getName());

	public SelectSessionHandler(String pathPrefix) {
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
		Cookie c = new Cookie(cfg.getCookieName(), token);
		c.setPath("/");
		SessionManager smgr = cfg.getSessionManager();
		String cookieDomain = null;
		try {
		    cookieDomain = smgr.getCookieDomainForHost(request.getServerName());
		}
		catch( IllegalArgumentException e) {
            LogUtils.info(cLog, "Unable to select session since can't find configured cookie domain for host '{0}'",
            		request.getServerName());
		}
		if (cookieDomain != null) {
	        smgr.generateSessionToken(token, cookieDomain);
	        c.setDomain(cookieDomain);
	        c.setMaxAge(-1);
	        c.setVersion(1);
	        response.addCookie(c);
	        smgr.markSessionAsActiveInDomain(token, cookieDomain);
		}
		String referer = request.getHeader("referer");
		response.sendRedirect(referer);
	}

}
