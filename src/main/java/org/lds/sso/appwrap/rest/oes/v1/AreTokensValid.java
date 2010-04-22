package org.lds.sso.appwrap.rest.oes.v1;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.rest.RestHandlerBase;

/**
 * Handler for answering true or false for each of a set of passed-in
 * tokens indicating if they are active or not.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class AreTokensValid extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(AreTokensValid.class);

	public AreTokensValid(String pathPrefix) {
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
		
		String tokenCnt = request.getParameter("token.cnt");
		
		if (tokenCnt == null || tokenCnt.equals("")) {
			if (cfg.getTrafficRecorder().isRecordingRest()) {
				Map<String,String> props = new HashMap<String,String>();
				cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
						HttpServletResponse.SC_BAD_REQUEST, "no token.cnt specified", 
						props);
			}
			super.sendResponse(cLog, response, HttpServletResponse.SC_BAD_REQUEST, "no token.cnt specified");
			return;
		}

		int tokens = -1;

		try {
			tokens = Integer.parseInt(tokenCnt);
		}
		catch(NumberFormatException nfe) {
			if (cfg.getTrafficRecorder().isRecordingRest()) {
				Map<String,String> props = new HashMap<String,String>();
				cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
						HttpServletResponse.SC_BAD_REQUEST, "token.cnt value '" + tokenCnt + "' is not an integer", 
						props);
			}
			super.sendResponse(cLog, response, HttpServletResponse.SC_BAD_REQUEST, "token.cnt value '" + tokenCnt + "' is not an integer");
			return;
		}
		StringWriter bufr = new StringWriter();
		PrintWriter out = new PrintWriter(bufr);
        StringWriter logBfr = null;
        PrintWriter log = null;
        String logReq = null;

		if (cfg.getTrafficRecorder().isRecordingRest()) {
		    // build request log chunk
            logBfr = new StringWriter();
            log = new PrintWriter(logBfr);
            log.print(" for request:\r\ntoken.cnt=" + tokenCnt + "\r\n");
            for (int t = 1; t<=tokens; t++) {
                String parm = "token." + t;
                String token = request.getParameter(parm);
                log.print(parm + "=" + token + "\r\n");
            }
            log.flush();
            logReq = logBfr.toString();
            // now provide response logger
            logBfr = new StringWriter();
            log = new PrintWriter(logBfr);
        }
		
		for (int t = 1; t<=tokens; t++) {
			String token = request.getParameter("token." + t);
			if (token != null && ! token.equals("")) {
			    String resp = token + "=" + cfg.getSessionManager().isValidToken(token); 
				out.println(resp);
				if (log != null) {
				    log.print(resp + "\r\n");
				}
			}
		}
		out.flush();
		String expandedMsg = null;
		
		if (cfg.getTrafficRecorder().isRecordingRest()) {
			Map<String,String> props = new HashMap<String,String>();
			log.flush();
			expandedMsg = "response:\r\n" + logBfr.toString() + "\r\n" + logReq; 
			cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
					HttpServletResponse.SC_OK, expandedMsg, 
					props);
		}
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, 
		        (expandedMsg != null ? expandedMsg : bufr.toString()));
	}
}
