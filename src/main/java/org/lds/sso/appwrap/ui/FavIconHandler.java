package org.lds.sso.appwrap.ui;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.Utils;
import org.lds.sso.appwrap.rest.RestHandlerBase;

/**
 * Serves up an html page with the text "<simulator version> is alive!" in the 
 * body tag. This is done without JSPs and hence is safe to use in unit tests.
 * 
 * @author BoydMR
 *
 */
public class FavIconHandler extends RestHandlerBase {

    public static final String FAVICON_PATH = "/favicon.ico";
    
    public FavIconHandler(String pathPrefix) {
        super(pathPrefix);
    }

    @Override
    protected void doHandle(String target, HttpServletRequest request,
            HttpServletResponse response, int dispatch) throws IOException {
        InputStream in = this.getClass().getResourceAsStream("favicon.ico");

        try {
            if (in != null) {
                response.setContentType("image/x-icon; charset=UTF-8");
                response.addHeader("Cache-Control", "max-age=3600");
                ServletOutputStream out = response.getOutputStream();

                byte[] bytes = new byte[1024];
                int read = in.read(bytes);
                while (read != -1) {
                    out.write(bytes, 0, read);
                    read = in.read(bytes);
                }
                out.flush();
            }
            else {
                response.sendError(response.SC_NOT_FOUND);
            }
        }
        finally {
            Utils.quietlyClose(in);
        }
    }
}
