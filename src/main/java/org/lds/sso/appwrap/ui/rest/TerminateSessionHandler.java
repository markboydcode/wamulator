package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.SessionManager;
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
    private static final Logger cLog = Logger.getLogger(TerminateSessionHandler.class);

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
		Cookie[] cookies = request.getCookies();
		for(Cookie ck : cookies) {
		    if (ck.getName().equals(cfg.getCookieName())) {
		        String currToken = ck.getValue();
                if (currToken.equals(token)) {
                    // deleting current session so clear out cookie
                    SessionManager smgr = cfg.getSessionManager();
                    String cookieDomain = null;
                    try {
                        cookieDomain = smgr.getCookieDomainForHost(request.getServerName());
                    }
                    catch( IllegalArgumentException e) {
                        cLog.info("Unable to clear cookie since can't find configured "
                            + "cookie domain for host '" + request.getServerName() + "'");
                    }
                    if (cookieDomain != null) {
                        cfg.getSessionManager().terminateSession(token, cookieDomain);
                        Cookie c = new Cookie(cfg.getCookieName(), token);
                        c.setPath("/");
                        c.setDomain(cookieDomain);
                        c.setMaxAge(0); // clears the cookie
                        c.setVersion(1);
                        response.addCookie(c);
                    }
                }
                break;
		    }
		}
		String referer = request.getHeader("referer");
		if (referer == null || referer.equals("")) {
		    referer = cfg.getLoginPage();
		}
		response.sendRedirect(referer);
	}

}
