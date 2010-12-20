package org.lds.sso.appwrap.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.Config;

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
	protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
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
