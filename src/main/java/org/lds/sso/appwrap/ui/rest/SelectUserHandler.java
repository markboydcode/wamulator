package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.rest.RestHandlerBase;
import org.mortbay.jetty.Request;

/**
 * Handles request to start a session for a user by setting a suitable
 * cookie.
 *  
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class SelectUserHandler extends RestHandlerBase {

	public SelectUserHandler(String pathPrefix) {
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
		String usr = uri.substring(uri.lastIndexOf('/')+1);
		String token = cfg.getSessionManager().generateSessionToken(usr);
		Cookie c = new Cookie(cfg.getCookieName(), token);
		c.setPath("/");
		c.setDomain(cfg.getCookieDomain());
		c.setMaxAge(-1);
		c.setVersion(1);
		response.addCookie(c);
		String referer = request.getHeader("referer");
		response.sendRedirect(referer);
	}

}
