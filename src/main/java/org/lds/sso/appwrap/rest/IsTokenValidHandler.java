package org.lds.sso.appwrap.rest;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.Config;

/**
 * Handler that can answer if a token represents a currently active session or
 * not.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class IsTokenValidHandler extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(IsTokenValidHandler.class.getName());

	public IsTokenValidHandler(String pathPrefix) {
		super(pathPrefix);
	}

	@SuppressWarnings("deprecation")
    @Override
	protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException {
		/**
		 * Get the current config instance each time which allows for reconfig
		 * of config object without restarting the service.
		 */
		Config cfg = Config.getInstance();

		String rawT = request.getParameter("token");
		String token = URLDecoder.decode(rawT, "utf-8");
		// for this old opensso interface to maintain compatibility I'll scan
		// all cookie domains and return true if the token is valid in any of them.
		boolean is = cfg.getSessionManager().isValidToken(token);

		if (cfg.getTrafficRecorder().isRecordingRest()) {
			Map<String, String> props = new HashMap<String, String>();
			props.put("token", token);
			cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
					HttpServletResponse.SC_OK, "boolean=" + is,
					props);
		}

		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "boolean=" + is);
	}
}
