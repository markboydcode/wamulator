package org.lds.sso.appwrap.rest;

import org.eclipse.jetty.server.Request;
import org.lds.sso.appwrap.Config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handler for the openSSO rest api for returning the configured name of the 
 * appwrap cookies. The response body consists of the octet stream "string="
 * followed by the sso cookie's name.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class GetCookieName extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(GetCookieName.class.getName());

	public GetCookieName(String pathPrefix) {
		super(pathPrefix);
	}

	@Override
	protected void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		/**
		 * Get the current config instance each time which allows for reconfig
		 * of config object without restarting the service.
		 */
		Config cfg = Config.getInstance();

		if (cfg.getTrafficRecorder().isRecordingRest()) {
			Map<String,String> props = new HashMap<String,String>();
			cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
					HttpServletResponse.SC_OK, "string=" + cfg.getCookieName(), 
					props);
		}
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "string=" + cfg.getCookieName());
	}
}
