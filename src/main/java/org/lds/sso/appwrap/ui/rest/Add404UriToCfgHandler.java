package org.lds.sso.appwrap.ui.rest;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.rest.RestHandlerBase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;

/**
 * Handles request to add uris that incurred 404 by the proxy due to missing
 * application canonical definition to the unenforced uri set.
 *  
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class Add404UriToCfgHandler extends RestHandlerBase {

	public Add404UriToCfgHandler(String pathPrefix) {
		super(pathPrefix);
	}

	@Override
	protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException {
		/**
		 * Get the current config instance each time which allows for reconfig
		 * of config object without restarting the service.
		 */
		Config cfg = Config.getInstance();

		if (target.contains("/add-uri-to-unenforced")) {
			String problemUri = request.getParameter("uri");
			
			if (problemUri != null) {
				String decoded = URLDecoder.decode(problemUri, "utf-8");
				throw new RuntimeException("FIX- This method not implemented");
				//cfg.getTrafficManager().addUnenforcedUrl(decoded); must go to site not traffic man
			}
		}
		response.sendRedirect("/admin/traffic.jsp");
		return;
	}
}
