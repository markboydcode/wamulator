package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.rest.RestHandlerBase;

/**
 * Handles requests to start/stop traffic recording, and clear traffic.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2008, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class TrafficRecordingHandler extends RestHandlerBase {

	public TrafficRecordingHandler(String pathPrefix) {
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

		// see if recording change is requested
		String cmd = target.substring(target.lastIndexOf('/')+1);

		if ("start".equals(cmd)) {
			cfg.getTrafficRecorder().setRecording(true);
		}
		else if ("stop".equals(cmd)) {
			cfg.getTrafficRecorder().setRecording(false);
		}
		else if ("clear".equals(cmd)) {
			cfg.getTrafficRecorder().getHits().clear();
		}
		response.sendRedirect("/admin/traffic.jsp");
	}
}
