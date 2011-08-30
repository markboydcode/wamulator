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
 * Handler for answering true or false for each of a set of passed-in
 * tokens indicating if they are active or not.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2011, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class UserNameForToken extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(UserNameForToken.class.getName());

	public UserNameForToken(String pathPrefix) {
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
		
		String token = request.getParameter("token");
		
		if (token == null || token.equals("")) {
			if (cfg.getTrafficRecorder().isRecordingRest()) {
				Map<String,String> props = new HashMap<String,String>();
				cfg.getTrafficRecorder().recordRestHit(this.pathPrefixRaw, 
						HttpServletResponse.SC_BAD_REQUEST, "no token specified", 
						props);
			}
			super.sendResponse(cLog, response, HttpServletResponse.SC_BAD_REQUEST, "no token specified");
			return;
		}
        String logReq = null;

		if (cfg.getTrafficRecorder().isRecordingRest()) {
		    // build request log chunk
	        StringWriter log = null;
	        PrintWriter logOut = null;
            log = new StringWriter();
            logOut = new PrintWriter(log);
            logOut.print(" for request:\r\ntoken=" + token + "\r\n");
            logOut.flush();
            logReq = log.toString();
        }
        StringWriter clientResp = new StringWriter();
        PrintWriter clientOut = new PrintWriter(clientResp);
        int httpCode = HttpServletResponse.SC_OK;

        boolean isValid = cfg.getSessionManager().isValidToken(token, request.getServerName());

        if (! isValid) { // means both session expired and bad token, treat as former
        	/*
{

    username: null
    loggedIn: false
    unavailabilityCause: null
    requestSuccessful: false
    serviceAvailable: true
}
        	 */
        	httpCode = HttpServletResponse.SC_FORBIDDEN; 
        	clientOut.print("{");
        	clientOut.print(" \"username\" : null,");
        	clientOut.print(" \"loggedIn\" : false,");
        	clientOut.print(" \"unavailabilityCause\" : null,");
        	clientOut.print(" \"requestSuccessful\" : false,");
        	clientOut.print(" \"serviceAvailable\" : true ");
        	clientOut.print("}");
        	clientOut.flush();
        	clientOut.close();
        }
        else {
        	/*
{
    username: "boydmr"
    loggedIn: true
    unavailabilityCause: null
    requestSuccessful: true
    serviceAvailable: true
}
        	 */
            String user = cfg.getUsernameFromToken(token);
        	httpCode = HttpServletResponse.SC_OK; 
        	clientOut.print("{");
        	clientOut.print(" \"username\" : \"" + user + "\",");
        	clientOut.print(" \"loggedIn\" : true,");
        	clientOut.print(" \"unavailabilityCause\" : null,");
        	clientOut.print(" \"requestSuccessful\" : true,");
        	clientOut.print(" \"serviceAvailable\" : true ");
        	clientOut.print("}");
        	clientOut.flush();
        	clientOut.close();
        }
		String clientResponse = clientResp.toString();
		
		if (cfg.getTrafficRecorder().isRecordingRest()) {
			Map<String,String> props = new HashMap<String,String>();
			String expandedMsg = "response:\r\n" + clientResponse + "\r\n" + logReq; 
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
