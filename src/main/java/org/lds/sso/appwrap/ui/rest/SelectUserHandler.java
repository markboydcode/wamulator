package org.lds.sso.appwrap.ui.rest;

import java.io.IOException; 
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.UserManager;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.lds.sso.appwrap.proxy.RequestHandler;
import org.lds.sso.appwrap.rest.RestHandlerBase;
import org.lds.sso.appwrap.User;

/**
 * Handles request to start a session for a user by setting a suitable
 * cookie.
 *  
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class SelectUserHandler extends RestHandlerBase {

    private static final Logger cLog = Logger.getLogger(SelectUserHandler.class);

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

        String name = request.getParameter("username");
        String usr = null;

        if (name != null && !"".equals(name)) {
            // coda user service being accessed for attributes
            String ext = cfg.getExternalUserSource();
            Map<String, String> userAtts = null;
            String uri = null;
            String content = null;
            if (ext == null || "".equals(ext)) {
            	// drop down into regular user manager search.
            	// TODO ask Scott why we require coda config. This breaks
            	// anyone running without coda
                // returnWithError("no-user-source-specified", minusQp, parms, response);
                // return;
            } else {
                HttpClient client = new HttpClient();
                uri = ext.replaceAll("\\{username\\}", name);
                HttpMethod method = new GetMethod(uri);
                method.setFollowRedirects(false);
                method.setRequestHeader("Accept", "application/xml");
                int status = client.executeMethod(method);
                content = method.getResponseBodyAsString();

                if (status != 200) {
                    cLog.error("Non 200 response code recieved from " + uri
                            + ", content returned: " + content);
                    returnWithError("error-accessing-ext-source", minusQp, parms, response);
                    return;
                }
                CodaServiceResponseParser parser = new CodaServiceResponseParser(content);
                userAtts = parser.getValues();
            }
            UserManager uman = cfg.getUserManager();
            if (userAtts == null // coda not used 
             || userAtts.get("good") != null) { // coda couldn't find user
                User user = uman.getUser(name);
                if(user == null) { //couldn't find user in config either
                    returnWithError("user-not-found", minusQp, parms, response);
                    return;
                }
            } else {
                // add user if found in cmis, or redirect back to sign-in if failed
                // ie: redirectTarget = referer plus error message via query param
                uman.setUser(name, "n/a");
                userAtts.put("policy-coda-status", "200-coda user atts retrieved");
                for (Iterator<Map.Entry<String, String>> itr = userAtts.entrySet().iterator(); itr.hasNext();) {
                    Map.Entry<String, String> ent = itr.next();
                    uman.addHeaderForLastUserAdded(ent.getKey(), ent.getValue());
                }
            }

            usr = name;
            authenticated = true;
        } else {
            String uri = request.getRequestURI();
            usr = uri.substring(uri.lastIndexOf('/') + 1);
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
