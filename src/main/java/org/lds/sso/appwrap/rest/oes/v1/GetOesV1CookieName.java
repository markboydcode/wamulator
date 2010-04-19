package org.lds.sso.appwrap.rest.oes.v1;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.rest.RestHandlerBase;

/**
 * Handler for returning the configured name of the sso cookie as the only
 * content of the response body.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class GetOesV1CookieName extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(GetOesV1CookieName.class);

	public GetOesV1CookieName(String pathPrefix) {
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
					HttpServletResponse.SC_OK, cfg.getCookieName(), 
					props);
		}
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, cfg.getCookieName());
	}
}
