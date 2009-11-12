package org.lds.sso.appwrap;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.HandlerList;

/**
 * Implementation of handler list that performs steps needed by all other
 * handlers and ui views. Examples are setting the current user object, token,
 * username, and config object in the request.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2008, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class ConfigInjectingHandlerList extends HandlerList {

	private Config cfg = null;

	public ConfigInjectingHandlerList(Config cfg) {
		this.cfg = cfg;
	}

	@Override
	public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException, ServletException {
		String cookieHdr = request.getHeader("Cookie");
		if (cookieHdr != null) {
			String token = cfg.getTokenFromCookie(cookieHdr);
			if (cfg.getSessionManager().isValidToken(token)) {
				String currentUsrNm = cfg.getUsernameFromToken(token);
				request.setAttribute("currentUserName", currentUsrNm);
				User usr = cfg.getUserManager().getUser(currentUsrNm);
				request.setAttribute("currentUser", usr);
				request.setAttribute("currentToken", token);
			}
		}
		request.setAttribute("config", cfg);
		super.handle(target, request, response, dispatch);
	}
}
