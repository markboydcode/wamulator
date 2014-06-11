package org.lds.sso.appwrap.ui.rest;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.lds.sso.appwrap.identity.SessionManager;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.rest.RestHandlerBase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class TerminateSimulatorHandler extends RestHandlerBase implements Handler {
	private static final Logger cLog = Logger.getLogger(SessionManager.class.getName());

	public TerminateSimulatorHandler(String pathPrefix) {
		super(pathPrefix);
	}

	@Override
	protected void doHandle(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		LogUtils.info(cLog, "Stopping simulator...");
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "string=stopping");
		try {
			Thread.sleep(1000); // give the writer a chance to send the response, and then kill the app
		} catch (InterruptedException e) {
			// ignore
		}
		System.exit(0);
	}

}
