package org.lds.sso.appwrap.ui.rest;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.eclipse.jetty.server.Request;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.rest.RestHandlerBase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 * The controller for the sso traffic http console page. This controller uses exact path matching not prefix matching
 * meaning that the target (which does not contain query params) mush match exactly the request URI.
 *
 * Created by markboyd on 5/16/14.
 */
public class ShowTrafficController extends RestHandlerBase {

    private final Configuration fmCfg;

    public ShowTrafficController(String path, Configuration fmCfg) {
        super(path, Match.EQUALS);
        this.fmCfg = fmCfg;
    }

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=utf-8");

        Template t = fmCfg.getTemplate(Service.UI_TEMPLATES_PATH + "/traffic.ftl");
        HashMap root = new HashMap();
        Config cfg = Config.getInstance();
        HeaderController.injectValues(root, cfg);

        root.put("config", cfg);
        root.put("page", "sso-traffic"); // tells tabs fragment which tab is active

        try {
            t.process(root, response.getWriter());
        } catch (TemplateException e) {
            e.printStackTrace();
        }
    }
}
