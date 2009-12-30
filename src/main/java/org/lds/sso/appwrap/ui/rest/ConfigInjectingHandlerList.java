package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.User;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.HandlerList;

/**
 * Implementation of handler list that performs steps needed by all other
 * handlers and ui views. Examples are setting the current user object, token,
 * username, and config object in the request. 
 * 
 * NOTE: If the target is a JSP page then any objects needed in the request
 * must be injected here.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2008, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class ConfigInjectingHandlerList extends HandlerList {

	public ConfigInjectingHandlerList() {
	}

	@Override
	public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException, ServletException {
		if (target.equals("/")) {
			response.sendRedirect("/admin/listUsers.jsp");
            ((Request) request).setHandled(true);
            return;
		}
        if (target.equals("/admin/listUsers.jsp")) {
        	// inject values needed by this page
    		String username = request.getParameter("username"); 
    		if (username != null) {
    			Config cfg = Config.getInstance();
    			request.setAttribute("selectedUserName", username);
    			User usr = cfg.getUserManager().getUser(username);
    			request.setAttribute("selectedUser", usr);
    		}
        }
        if (target.equals("/admin/debug.jsp")) {
        	// inject values needed by this page
    		String m = request.getMethod(); 
			request.setAttribute("method", m);
        }

		Config cfg = Config.getInstance();
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
