package org.lds.sso.appwrap.ui.rest;

import org.eclipse.jetty.server.Request;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.rest.RestHandlerBase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
	protected void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
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
			response.sendRedirect(cfg.getWamulatorConsoleUrlBase() + "/traffic");
		}
		else if ("stop".equals(cmd)) {
			cfg.getTrafficRecorder().setRecording(false);
			response.sendRedirect(cfg.getWamulatorConsoleUrlBase() + "/traffic");
		}
		else if ("clear".equals(cmd)) {
			cfg.getTrafficRecorder().getHits().clear();
			response.sendRedirect(cfg.getWamulatorConsoleUrlBase() + "/traffic");
		}
		else if ("start-rest".equals(cmd)) {
			cfg.getTrafficRecorder().setRecordingRest(true);
			response.sendRedirect(cfg.getWamulatorConsoleUrlBase() + "/rest-traffic");
		}
		else if ("stop-rest".equals(cmd)) {
			cfg.getTrafficRecorder().setRecordingRest(false);
			response.sendRedirect(cfg.getWamulatorConsoleUrlBase() + "/rest-traffic");
		}
		else if ("clear-rest".equals(cmd)) {
			cfg.getTrafficRecorder().getRestHits().clear();
			response.sendRedirect(cfg.getWamulatorConsoleUrlBase() + "/rest-traffic");
		}
		else {
			response.sendRedirect(cfg.getWamulatorConsoleUrlBase() + "/traffic");
		}
	}
}
