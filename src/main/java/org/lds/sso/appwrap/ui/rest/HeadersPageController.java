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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * The controller for the headers debug page that serves back the set of headers received by the back-end page.
 *
 * Created by markboyd on 5/16/14.
 */
public class HeadersPageController extends RestHandlerBase {

    private final Configuration fmCfg;

    public HeadersPageController(String path, Configuration fmCfg) {
        super(path, Match.EQUALS);
        this.fmCfg = fmCfg;
    }

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=utf-8");

        Template t = fmCfg.getTemplate(Service.UI_TEMPLATES_PATH + "/headers.ftl");
        HashMap root = new HashMap();
        Config cfg = Config.getInstance();

        root.put("config", cfg);
        root.put("jsputils", new JspUtils());
        root.put("method", request.getMethod());

        TreeMap<String, ArrayList<String>> headers = new TreeMap<String, ArrayList<String>>();
        Enumeration<String> names = request.getHeaderNames();

        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Enumeration<String> vals = request.getHeaders(name);
            ArrayList<String> values = new ArrayList<String>();

            while (vals.hasMoreElements()) {
                values.add(vals.nextElement());
            }
            headers.put(name, values);
        }
        root.put("headers", headers);
        root.put("params", new TreeMap<String, String[]>(request.getParameterMap()));

        try {
            t.process(root, response.getWriter());
        } catch (TemplateException e) {
            e.printStackTrace();
        }
    }
}
