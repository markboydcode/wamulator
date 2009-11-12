package org.lds.sso.appwrap.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;

/**
 * Handler for the rest api for returning the configured name of the appwrap cookies.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class GetCookieName extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(GetCookieName.class);

	public GetCookieName(String pathPrefix, Config cfg) {
		super(pathPrefix, cfg);
	}

	@Override
	protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException {
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, "string=" + cfg.getCookieName());
	}
}
