package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.identity.ExternalUserSource;
import org.lds.sso.appwrap.identity.ExternalUserSource.Response;
import org.lds.sso.appwrap.identity.SessionManager;
import org.lds.sso.appwrap.identity.UserManager;
import org.lds.sso.appwrap.proxy.RequestHandler;
import org.lds.sso.appwrap.rest.RestHandlerBase;

/**
 * Handles request to start a session for a user by setting a suitable
 * cookie.
 *  
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class SelectUserHandler extends RestHandlerBase {

    private static final Logger cLog = Logger.getLogger(SelectUserHandler.class.getName());
    private static final String OB_FORM_LOGIN_HASH_COOKIE_NAME = "ObFormLoginHashCookie";
	
    public SelectUserHandler(String pathPrefix) {
        super(pathPrefix);
    }
    /*

    coda service is located at:

    http://tech.lds.org/coda/services/1/member/osso/pholder

    if accepts header includes application/json then response is:

    {
    birthdate: "1980-03-31"
    cn: "pholder"
    email: ""
    gender: "M"
    givenName: "Perry"
    individualId: "0083419004078"
    ldsAccountId: "1"
    ldsMrn: "0083419004078"
    positions: "P57:W555005:S555001:A555000"
    preferredLanguage: "en"
    preferredName: "Perry Holder"
    sn: "Holder"
    status: "200"
    units: "W555005:S555001:A555000"
    }

    Otherwise it is:

    <org.lds.community.data.ws.dto.OssoMemberDto>
    <birthdate>1980-03-31</birthdate>
    <cn>pholder</cn>
    <gender>M</gender>
    <givenName>Perry</givenName>
    <individualId>0083419004078</individualId>
    <ldsAccountId>1</ldsAccountId>

    <ldsMrn>0083419004078</ldsMrn>
    <positions>P57:W555005:S555001:A555000</positions>
    <preferredLanguage>en</preferredLanguage>
    <preferredName>Perry Holder</preferredName>
    <sn>Holder</sn>
    <status>200</status>

    <units>W555005:S555001:A555000</units>
    </org.lds.community.data.ws.dto.OssoMemberDto>

    If a user is specified that doesn't exist like pholderhjkjk in
    http://tech.lds.org/coda/services/1/member/osso/pholderhjkjk

    <org.lds.community.data.ws.dto.ResponseStatus>
    <good>false</good>
    <message>pholderhjkjk</message>
    </org.lds.community.data.ws.dto.ResponseStatus>




     */
    @Override
    protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
            throws IOException {
        //
        //		Get the current config instance each time which allows for reconfig
        //		of config object without restarting the service.

        Config cfg = Config.getInstance();
        boolean authenticated = false;
        String redirectTarget = null;
        String minusQp = redirectTarget;
        List<Pair> parms = getParms(redirectTarget, -1);

        // keep in mind for automated sign-in we may not have goto or referer.        
        String referer = request.getHeader("referer");
        if (referer != null) {
        	referer = URLDecoder.decode(referer, "utf-8");
        	int qmIdx = referer.indexOf("?");
        	minusQp = (qmIdx != -1 ? referer.substring(0, qmIdx) : referer);
        	parms = getParms(referer, qmIdx);
        	redirectTarget = referer;
        }

        String gotoUri = request.getParameter("goto");
        if (gotoUri != null && !gotoUri.equals("")) {
            gotoUri = URLDecoder.decode(gotoUri, "utf-8");
            redirectTarget = gotoUri;
        }
		
/* Javascript in JSP sets a cookie for cases where HTTP fragments are in the URL
 * So we need to check for this cookie and if it exists append it's avlue to the
 * redirectTarget.  Angular JS, an MVVM framework uses these fragments.  Probably
 * the right way to do this is to actually make the Cookie classes already here
 * more generic so they are handling any cookies rather than only handling the
 * wamulator specific cookie.  In fact, they should probably all be parsed and
 * stored in a hash map early on and then accessed easily by name here.  I spent
 * some time looking for where that should go but became frustrated at all the
 * classes that have generic "handler" type names but then are implemented so
 * specifically that they only handle one thing or one case so this less
 * elegant solution is what you get :)
 */
		Cookie[] requestCookies = request.getCookies();
		if (requestCookies != null) {
			for (Cookie c : requestCookies) {
				if (c.getName().equals(OB_FORM_LOGIN_HASH_COOKIE_NAME) && c.getValue() != null) {
					redirectTarget += c.getValue();
					break;
				}
			}
		}


/*
 * WAMULAT-57 fix in progress
 * add in once we have updated the selectUser.jsp page to allow selection of 
 * user links to populate form and submit via ajax.
        String method = request.getMethod(); 
        if (! method.equalsIgnoreCase("POST")) {
            returnWithError("error-must-use-http-post", minusQp, parms, response);
            return;
        }
*/
        String name = request.getParameter("username");
        UserManager uman = cfg.getUserManager();
        
        if (StringUtils.isEmpty(name)) {
        	// backwards compatibility allowing username to be last path on uri
            String uri = request.getRequestURI();
            name = uri.substring(uri.lastIndexOf('/') + 1);
        }

        String password = request.getParameter("password");
    	Response extResult = authenticateToSources(cfg, name, password);
    	switch(extResult) {
    	case ErrorAccessingSource: 
            returnWithError("error-accessing-ext-source", minusQp, parms, response);
            return;
    	case UserNotFound:
            returnWithError("user-not-found", minusQp, parms, response);
            return;
    	case UnableToAuthenticate:
            returnWithError("failed-authentication", minusQp, parms, response);
            return;
    	case UserInfoLoaded:
    		authenticated = true;
    	}

        if (authenticated) {
        	Cookie c = generateSession(cfg, name, request.getServerName());
            if (c != null) {
                response.addCookie(c);
            }
        }

        // handle case without referer and without goto by sending page w/cookie
        if (redirectTarget == null) {
            String content = RequestHandler.getResponseBody(
					"Authenticated Successfully",
					"User Authenticated Successfully but no referer header nor " +
					"a goto query parameter were specified and no login page " +
					"is configured. Hence this page is returned.", null, null);
            Writer out = response.getWriter();
            out.write(content);
            out.flush();
        }
        else {
            response.sendRedirect(redirectTarget);
        }
    }

	/**
	 * Scans through all registered sources only returning when we have
	 * exhausted the list or received a loaded, or failedAuthN. These last two
	 * indicate that we found a user in that store so send a message to the
	 * screen accordingly or a message relative to the last store's response.
	 * Logs should show failures in previous stores if any.
	 */
    public static Response authenticateToSources(Config cfg, String username, String pwd) throws IOException {
    	List<ExternalUserSource> sources = cfg.getExternalUserSources();
    	Response extResult = null;
    	int srcIdx = 0;
    	while(srcIdx < sources.size() 
    			&& (extResult == null 
    			|| extResult == Response.ErrorAccessingSource 
    			|| extResult == Response.UserNotFound)) {
    		ExternalUserSource src = sources.get(srcIdx);
        	extResult = src.loadExternalUser(username, pwd);
        	srcIdx++;
    	}
    	return extResult;
    }

    /**
     * Generate a user session and corresponding cookie. Returns the cookie if
     * able to generate or null if unable to for the request's specified host.
     * 
     * @param cfg
     * @param username
     * @param serverName
     * @return
     */
    public static Cookie generateSession(Config cfg, String username, String serverName) {
        SessionManager smgr = cfg.getSessionManager();
        String cookieDomain = null;
        Cookie c = null;
        try {
            cookieDomain = smgr.getCookieDomainForHost(serverName);
        }
        catch( IllegalArgumentException e) {
            cLog.info("Unable to set cookie to complete authentication since can't find configured "
                + "cookie domain for incoming request's host '" + serverName + "'");
        }
        if (cookieDomain != null) {
            String token = smgr.generateSessionToken(username, serverName);
            c = new Cookie(cfg.getCookieName(), token);
            c.setPath("/");
            if (! cookieDomain.equals("localhost")) {
                c.setDomain(cookieDomain);
            }
            c.setMaxAge(-1);
            c.setVersion(1);
        }
        return c;
    }
    
    /**
     * Crafts a URL matching the referer but with the page-error query param
     * replaced or appended and redirects back to the page.
     *
     * @param message
     * @param minusQp
     * @param parms
     * @param response
     * @throws IOException
     */
    private void returnWithError(String message, String minusQp, List<Pair> parms, HttpServletResponse response) throws IOException {
        for (Iterator<Pair> i = parms.iterator(); i.hasNext();) {
            Pair pair = i.next();
            if (pair.key.equals("page-error")) {
                i.remove();
            }
        }
        parms.add(new Pair("page-error", message));
        String target = buildRedirect(minusQp, parms);
        response.sendRedirect(target);
    }

    private String buildRedirect(String minusQp, List<Pair> parms) {
        StringBuffer sb = new StringBuffer();
        sb.append(minusQp).append("?");

        for (Iterator<Pair> i = parms.iterator(); i.hasNext();) {
            Pair p = i.next();
            try {
                sb.append(p.key).append("=").append(URLEncoder.encode(p.value, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                // should never get here since utf-8 is mandatory for JVMs
                throw new RuntimeException("Can't craft redirect URL", e);
            }
            if (i.hasNext()) {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    /**
     * Creates a List of {@link Pair} one for each query parameter for the referer.
     *
     * @param referer
     * @param qmIdx
     * @return
     */
    private List<Pair> getParms(String referer, int qmIdx) {
        List<Pair> list = new ArrayList<Pair>();
        if (qmIdx != -1) {
            String qparms = referer.substring(qmIdx + 1);
            String[] parms = qparms.split("\\&");
            for (String pair : parms) {
                String[] toks = pair.split("\\=");
                list.add(new Pair(toks[0], (toks.length > 0 ? toks[1] : "")));
            }
        }
        return list;
    }

    /**
     * Holds one key and value pair.
     *
     * @author Mark Boyd
     * @copyright: Copyright, 2010, The Church of Jesus Christ of Latter Day Saints
     *
     */
    private static class Pair {

        String key = null;
        String value = null;

        public Pair(String key, String val) {
            this.key = key;
            this.value = val;
        }
    }

    /**
     * Appends the error code appropriately based upon pre-existence of
     * query params or not.
     *
     * @param referer
     * @param errCode
     * @return
     */
    private String appendErrCode(String referer, String errCode) {
        if (referer.contains("?")) {
            return referer += "&err=" + errCode;
        } else {
            return referer += "?err=" + errCode;
        }
    }
}
