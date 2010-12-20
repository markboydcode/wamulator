package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.SessionManager;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.rest.RestHandlerBase;
import org.mortbay.jetty.Handler;

public class TerminateSimulatorHandler extends RestHandlerBase implements Handler {
	private static final Logger cLog = Logger.getLogger(SessionManager.class.getName());

	public TerminateSimulatorHandler(String pathPrefix) {
		super(pathPrefix);
	}

	@Override
	protected void doHandle(String target, HttpServletRequest request,
			HttpServletResponse response, int dispatch) throws IOException {
		LogUtils.info(cLog, "Stopping simulator...");
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "string=stopping");
		try {
			Thread.sleep(1000); // give the writer a change to send the response, and then kill the app
		} catch (InterruptedException e) {
			// ignore
		}
		System.exit(0);
	}

}
