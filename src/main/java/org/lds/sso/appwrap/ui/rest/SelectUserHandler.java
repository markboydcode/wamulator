package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.UserManager;
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
		boolean authenticated = false;
		String referer = request.getHeader("referer");
		String redirectTarget = referer;
		String gotoUri = request.getParameter("goto");
		
		if (gotoUri != null && ! gotoUri.equals("")) {
			 redirectTarget = gotoUri;
		}

		String name = request.getParameter("coda-user");
		String usr = null;
		
		if (name != null && ! "".equals(name)) {
			// add user if found in cmis, or redirect back to sign-in if failed
			// ie: redirectTarget = referer plus error message via query param
			UserManager uman = cfg.getUserManager();
			uman.setUser(name, "n/a");
			// add user, load headers for user
			// uman.addHeaderForLastUserAdded(header, value);
			System.out.println("got here");
			if (referer.contains("?")) {
				referer += "&err=user-not-found";
			}
			else {
				referer += "?err=user-not-found";
			}
			redirectTarget = referer;
		}
		else {
			String uri = request.getRequestURI();
			usr = uri.substring(uri.lastIndexOf('/')+1);
			authenticated = true;
		}

		if (authenticated) {
			String token = cfg.getSessionManager().generateSessionToken(usr);
			Cookie c = new Cookie(cfg.getCookieName(), token);
			c.setPath("/");
			c.setDomain(cfg.getCookieDomain());
			c.setMaxAge(-1);
			c.setVersion(1);
			response.addCookie(c);
		}
		
		response.sendRedirect(redirectTarget);
	}

}
