package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
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
import org.lds.sso.appwrap.rest.RestHandlerBase;
import org.lds.sso.plugins.authz.LegacyPropsInjector;
import org.mortbay.jetty.Request;

import com.iplanet.sso.SSOException;

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
    @Override
    protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
    throws IOException {
    //
    //		 Get the current config instance each time which allows for reconfig
    //		 of config object without restarting the service.
    //
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
     */

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
        String referer = request.getHeader("referer");
        int qmIdx = referer.indexOf("?");
        String minusQp = (qmIdx != -1 ? referer.substring(0, qmIdx) : referer);
        List<Pair> parms = getParms(referer, qmIdx);
        String redirectTarget = referer;
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

            if (ext == null || "".equals(ext)) {
                returnWithError("no-user-source-specified", minusQp, parms, response);
                return;
            }
            HttpClient client = new HttpClient();
            String uri = ext.replaceAll("\\{username\\}", name);
            HttpMethod method = new GetMethod(uri);
            method.setFollowRedirects(false);
            method.setRequestHeader("Accept", "application/xml");
            int status = client.executeMethod(method);
            String content = method.getResponseBodyAsString();

            if (status != 200) {
                cLog.error("Non 200 response code recieved from " + uri
                        + ", content returned: " + content);
                returnWithError("error-accessing-ext-source", minusQp, parms, response);
                return;
            }
            CodaServiceResponseParser parser = new CodaServiceResponseParser(content);
            Map<String, String> userAtts = parser.getValues();
            if (userAtts.get("good") != null) { // couldn't find user
                returnWithError("user-not-found", minusQp, parms, response);
                return;
            }
            // add user if found in cmis, or redirect back to sign-in if failed
            // ie: redirectTarget = referer plus error message via query param
            UserManager uman = cfg.getUserManager();
            uman.setUser(name, "n/a");
            userAtts.put(LegacyPropsInjector.CP_STATUS_PROPERTY, "200-coda user atts retrieved");
            for (Iterator<Map.Entry<String, String>> itr = userAtts.entrySet().iterator(); itr.hasNext();) {
                Map.Entry<String, String> ent = itr.next();
                try {
                    uman.addHeaderForLastUserAdded(ent.getKey(), ent.getValue());
                } catch (SSOException e) {
                    throw new RuntimeException("Unable to add from service " + uri + " header '" + name + "' from response '" + content + "'", e);
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

        response.sendRedirect(redirectTarget);
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
