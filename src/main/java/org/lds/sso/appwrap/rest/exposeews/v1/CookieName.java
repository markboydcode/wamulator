package org.lds.sso.appwrap.rest.exposeews.v1;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.proxy.RequestHandler;
import org.lds.sso.appwrap.rest.RestHandlerBase;

/**
 * Handler for answering the name of the cookie used by the wamulator.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2011, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class CookieName extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(CookieName.class.getName());

	public CookieName(String pathPrefix) {
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
		
        StringWriter clientResp = new StringWriter();
        PrintWriter clientOut = new PrintWriter(clientResp);
        int httpCode = HttpServletResponse.SC_OK;

        	/*
{
    username: "boydmr"
    loggedIn: true
    unavailabilityCause: null
    requestSuccessful: true
    serviceAvailable: true
}
        	 */
        String cookieName = cfg.getCookieName();
    	httpCode = HttpServletResponse.SC_OK; 
    	clientOut.print("{");
    	clientOut.print(" \"cookieName\" : \"" + cfg.getCookieName() + "\",");
    	clientOut.print(" \"unavailabilityCause\" : null,");
    	clientOut.print(" \"requestSuccessful\" : true,");
    	clientOut.print(" \"serviceAvailable\" : true ");
    	clientOut.print("}");
    	clientOut.flush();
    	clientOut.close();
		String clientResponse = clientResp.toString();
		
		if (cfg.getTrafficRecorder().isRecordingRest()) {
			Map<String,String> props = new HashMap<String,String>();
			String expandedMsg = "response:\r\n" + clientResponse; 
			cfg.getTrafficRecorder().recordRestHit(this.pathPrefixRaw, 
					HttpServletResponse.SC_OK, expandedMsg, 
					props);
		}
		//super.sendResponse(cLog, response, HttpServletResponse.SC_OK, clientResponse);
		response.setContentType("application/json;charset=UTF-8");
		response.setContentLength(clientResponse.length());
		response.setHeader("Content-Language", "en-US");
		response.setHeader("Date", RequestHandler.getCurrentDateHeader());
        response.setStatus(httpCode);
		PrintWriter out = response.getWriter();
		out.println(clientResponse);
		out.flush();
	}
}
