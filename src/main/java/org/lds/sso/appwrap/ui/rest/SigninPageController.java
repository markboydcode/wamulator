package org.lds.sso.appwrap.ui.rest;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.eclipse.jetty.server.Request;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.rest.RestHandlerBase;
import org.lds.sso.appwrap.ui.JspUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * The controller for the rendering the sign-in page. This controller uses exact path matching not prefix matching
 * meaning that the target (which does not contain query params) mush match exactly the request URI.
 *
 * Created by markboyd on 5/16/14.
 */
public class SigninPageController extends RestHandlerBase {

    private final Configuration fmCfg;
    private static final SimpleDateFormat FOUR_DIGIT_YEAR = new SimpleDateFormat("yyyy");

    public SigninPageController(String path, Configuration fmCfg) {
        super(path, Match.EQUALS);
        this.fmCfg = fmCfg;
    }

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Cache-Control", "must-revalidate");
        response.addHeader("Cache-Control", "private");
        response.addHeader("Pragma", "no-cache");

        Template t = fmCfg.getTemplate(Service.UI_TEMPLATES_PATH + "/signin.ftl");
        HashMap root = new HashMap();
        Config cfg = Config.getInstance();
        HeaderController.injectValues(root, cfg); // signin page footer uses consoleTitle

        // var: config
        // var: jsputils
        root.put("config", cfg);
        root.put("jsputils", new JspUtils());

        // var: gotoQueryParm
        // var: gotoUriEnc
        root.put("gotoQueryParm", "");
        root.put("gotoUriEnc", "");

        String gotoParm = request.getParameter("goto");

        if (gotoParm != null && !"".equals(gotoParm)) {
            root.put("gotoUriEnc", gotoParm);
            try {
                root.put("gotoQueryParm", "?goto=" + URLEncoder.encode(gotoParm, "utf-8"));
            }
            catch (UnsupportedEncodingException e) {
                // ignore since we know utf-8 is built in to jvm
            }
        }

        // var: formAction available to template
        root.put("formAction", "/admin/action/set-user");
        String loginAction = cfg.getLoginAction();

        if (loginAction != null && !"".equals(loginAction)) {
            root.put("formAction", loginAction);
        }

        // var: pageError
        root.put("pageError", "");
        String error = request.getParameter("page-error");

        if (error != null && !"".equals(error)) {
            root.put("pageError", error);
        }

        // var: currentToken
        String cookieHdr = request.getHeader("Cookie");
        if (cookieHdr != null) {
            String token = cfg.getTokenFromCookie(cookieHdr);
            if (cfg.getSessionManager().isValidToken(token)) {
                String currentUsrNm = cfg.getUsernameFromToken(token);
                //root.put("currentUserName", currentUsrNm);
                //User usr = cfg.getUserManager().getUser(currentUsrNm);
                //root.put("currentUser", usr);
                root.put("currentToken", token);
            }
        }

        // var: currentYear - for copyright notice
        root.put("currentYear", FOUR_DIGIT_YEAR.format(new java.util.Date()));
        try {
            t.process(root, response.getWriter());
        } catch (TemplateException e) {
            e.printStackTrace();
        }
    }
}
