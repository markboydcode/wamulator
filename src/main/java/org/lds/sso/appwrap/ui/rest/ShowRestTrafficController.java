package org.lds.sso.appwrap.ui.rest;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.eclipse.jetty.server.Request;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.identity.User;
import org.lds.sso.appwrap.rest.RestHandlerBase;
import org.lds.sso.appwrap.ui.JspUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 * The controller for the rest traffic http console page. This controller uses exact path matching not prefix matching
 * meaning that the target (which does not contain query params) mush match exactly the request URI.
 *
 * Created by markboyd on 5/16/14.
 */
public class ShowRestTrafficController extends RestHandlerBase {

    private final Configuration fmCfg;

    public ShowRestTrafficController(String path, Configuration fmCfg) {
        super(path, Match.EQUALS);
        this.fmCfg = fmCfg;
    }

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=utf-8");

        Template t = fmCfg.getTemplate(Service.UI_TEMPLATES_PATH + "/rest-traffic.ftl");
        HashMap root = new HashMap();
        Config cfg = Config.getInstance();
        HeaderController.injectValues(root, cfg);

        root.put("config", cfg);
        root.put("jsputils", new JspUtils());
        root.put("page", "rest-traffic"); // tells tabs fragment which tab is active

        String cookieHdr = request.getHeader("Cookie");
        if (cookieHdr != null) {
            String token = cfg.getTokenFromCookie(cookieHdr);
            if (cfg.getSessionManager().isValidToken(token)) {
                String currentUsrNm = cfg.getUsernameFromToken(token);
                root.put("currentUserName", currentUsrNm);
                User usr = cfg.getUserManager().getUser(currentUsrNm);
                root.put("currentUser", usr);
                root.put("currentToken", token);
            }
        }

        // handle clicks on users in page:
        String usrNm = request.getParameter("username");
        if (usrNm != null && !"".equals(usrNm)) {
            User usr = cfg.getUserManager().getUser(usrNm);
            root.put("selectedUserName", usrNm);
            root.put("selectedUser", usr);
        }

        try {
            t.process(root, response.getWriter());
        } catch (TemplateException e) {
            e.printStackTrace();
        }
    }
}
