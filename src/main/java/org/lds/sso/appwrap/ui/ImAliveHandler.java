package org.lds.sso.appwrap.ui;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.rest.RestHandlerBase;

/**
 * Serves up an html page with the text "<simulator version> is alive!" in the 
 * body tag. This is done without JSPs and hence is safe to use in unit tests.
 * 
 * @author BoydMR
 *
 */
public class ImAliveHandler extends RestHandlerBase {

    public static final String IS_ALIVE = " is Alive!";
    public static final String IS_ALIVE_PATH = "/is-alive";
    
    public ImAliveHandler(String pathPrefix) {
        super(pathPrefix);
    }

    @Override
    protected void doHandle(String target, HttpServletRequest request,
            HttpServletResponse response, int dispatch) throws IOException {
        Config cfg = Config.getInstance();
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.print("<html><head><title>");
        out.print(cfg.getServerName());
        out.print("</title></head><body><h1>");
        out.print(cfg.getServerName());
        out.print(IS_ALIVE);
        out.print("</h1></body></html>");
    }
}
