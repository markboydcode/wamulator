package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.SiteMatcher;
import org.lds.sso.appwrap.TrafficManager;
import org.lds.sso.appwrap.identity.SessionManager;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.HandlerWrapper;

/**
 * Handles requests to the sign-in page from a cdsso configured domain and
 * if a session is already had in this master domain redirects to that calling 
 * domain to set up the session in that domain as well.
 *  
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class SignInPageCdssoHandler extends HandlerWrapper {
    private String[] pagePatterns;
    
    public static final String CDSSO_PARAM_NAME = "cdsso-token";

	public SignInPageCdssoHandler(String[] pagePatterns, Handler wrapped) {
	    this.addHandler(wrapped);
	    this.pagePatterns = pagePatterns;
	}

	public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException, ServletException {
		/**
		 * Get the current config instance each time which allows for reconfig
		 * of config object without restarting the service.
		 */
		Config cfg = Config.getInstance();
		String uri = request.getRequestURI();

		for (String page : pagePatterns) {
		    if (uri.contains(page)) {
		        if (hasValidSession(request) && isFromCdssoDomain(request)) {
		            sendCdssoResponse(request, response);
		            ((Request) request).setHandled(true);
		            return;
		        }
	            else {
	                break; // let it ride
	            }
		    }
		}
        super.handle(target, request, response, dispatch);
	}

	private void sendCdssoResponse(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String gt = request.getParameter("goto");
        if (gt.contains("?")) {
            gt += "&" + CDSSO_PARAM_NAME + "=" + request.getAttribute("token");
        }
        else {
            gt += "?" + CDSSO_PARAM_NAME + "=" + request.getAttribute("token");
        }
        response.sendRedirect(gt);
    }

    /**
	 * Returns true if the request includes the goto url parameter with a
	 * domain that matches a cdsso domain configured in the sso-cookie
	 * directive.
	 * 
	 * @param request
	 * @return
	 */
	protected boolean isFromCdssoDomain(HttpServletRequest request) {
	    String gt = request.getParameter("goto");
	    if (gt == null) {
	        return false;
	    }
        if (gt.startsWith("http://")) {
            gt = gt.substring("http://".length());
        }
        if (gt.startsWith("https://")) { // for when we support if ever
            gt = gt.substring("https://".length());
        }
        int slash = gt.indexOf("/");
        if (slash != -1) {
            gt = gt.substring(0, slash);
        }
        int colon = gt.indexOf(":");
        String host = gt;
        
        if (colon != -1) {
            host = gt.substring(0, colon);
        }
        Config cfg = Config.getInstance();
        SessionManager smgr = cfg.getSessionManager();
        try {
            String cd = smgr.getCookieDomainForHost(host);
            return true;
        }
        catch(IllegalArgumentException i) {
            return false;
        }
    }

    /**
	 * Returns true if the request includes an unexpired sso cookie.
	 * 
	 * @param request
	 * @return
	 */
    protected boolean hasValidSession(HttpServletRequest request) {
        Cookie[] cs = request.getCookies();
        if (cs == null) {
            return false;
        }
        Config cfg = Config.getInstance();
        
        for (Cookie c : cs) {
            if (c.getName().equals(cfg.getCookieName())) {
                SessionManager smgr = cfg.getSessionManager();
                TrafficManager tmgr = cfg.getTrafficManager();
                SiteMatcher site = tmgr.getMasterSite();
                String host = site.getHost();
                
                if (smgr.isValidToken(c.getValue(), host)) {
                    // cache token for sendCdssoResponse method
                    request.setAttribute("token", c.getValue());
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        return false;
    }

}
