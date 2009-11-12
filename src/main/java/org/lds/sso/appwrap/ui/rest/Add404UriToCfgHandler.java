package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.AllowedUri;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.rest.RestHandlerBase;
import org.mortbay.jetty.Request;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.sun.org.apache.xml.internal.utils.UnImplNode;

/**
 * Handles request to add uris that incurred 404 by the proxy due to missing
 * application canonical definition to either the user's permitted uris or to 
 * unenforced uri set. Expects the last path in the request uri to the the user's
 * username.
 *  
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class Add404UriToCfgHandler extends RestHandlerBase {

	public Add404UriToCfgHandler(String pathPrefix, Config cfg) {
		super(pathPrefix, cfg);
	}

	@Override
	protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException {
		if (target.contains("/add-uri-to-user/")) {
			String usr = target.substring(target.lastIndexOf('/')+1);
			User user = cfg.getUserManager().getUser(usr);
			String problemUri = request.getParameter("uri");
			String method = request.getParameter("method");
			
			if (user != null && problemUri != null && method != null) {
				URL url = new URL(problemUri);
				int port = url.getPort();
				if (port == -1 ) {
					port = 80;
				}
				String path = url.getPath() + (url.getQuery() == null ? "" : ("?" + url.getQuery()));
				AllowedUri au = new AllowedUri(url.getHost(), port, path, new String [] {"GET"});
				System.out.println(this.getClass().getName() + ": MUST ADD SUPPORT FOR PASSING METHOD IN TOO!!! Coercing to GET.");
				cfg.getUserManager().addPermission(usr, au);
			}
		}
		else if (target.contains("/add-uri-to-unenforced")) {
			String problemUri = request.getParameter("uri");
			
			if (problemUri != null) {
				String decoded = URLDecoder.decode(problemUri, "utf-8");
				throw new NotImplementedException();
				//cfg.getTrafficManager().addUnenforcedUrl(decoded); must go to site not traffic man
			}
		}
		response.sendRedirect("/admin/traffic.jsp");
		return;
	}
}
