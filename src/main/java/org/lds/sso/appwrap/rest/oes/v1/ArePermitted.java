package org.lds.sso.appwrap.rest.oes.v1;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.rest.RestHandlerBase;

/**
 * Handler for answering true or false for each of a set of passed-in
 * resources and actions and optional content parameters for a given user
 * session as represented by the passed-in token.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class ArePermitted extends RestHandlerBase {
	private static final Logger cLog = Logger.getLogger(ArePermitted.class);

	public ArePermitted(String pathPrefix) {
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
                cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
                        HttpServletResponse.SC_BAD_REQUEST, "no token specified", 
                        props);
            }
            super.sendResponse(cLog, response, HttpServletResponse.SC_BAD_REQUEST, "no token specified");
            return;
        }

        String resCnt = request.getParameter("res.cnt");
        
        if (resCnt == null || resCnt.equals("")) {
            if (cfg.getTrafficRecorder().isRecordingRest()) {
                Map<String,String> props = new HashMap<String,String>();
                cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
                        HttpServletResponse.SC_BAD_REQUEST, "no res.cnt specified", 
                        props);
            }
            super.sendResponse(cLog, response, HttpServletResponse.SC_BAD_REQUEST, "no res.cnt specified");
            return;
        }

		int resCount = -1;

		try {
			resCount = Integer.parseInt(resCnt);
		}
		catch(NumberFormatException nfe) {
			if (cfg.getTrafficRecorder().isRecordingRest()) {
				Map<String,String> props = new HashMap<String,String>();
				cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
						HttpServletResponse.SC_BAD_REQUEST, "res.cnt value '" + resCnt + "' is not an integer", 
						props);
			}
			super.sendResponse(cLog, response, HttpServletResponse.SC_BAD_REQUEST, "res.cnt value '" + resCnt + "' is not an integer");
			return;
		}
        StringWriter logReq = null;
        PrintWriter logReqWrt = null;
        
        if (cfg.getTrafficRecorder().isRecordingRest()) {
            // build request log chunk
            logReq = new StringWriter();
            logReqWrt = new PrintWriter(logReq);
            logReqWrt.print(" for request:\r\ntoken=" + token + "\r\n");
            logReqWrt.print("res.cnt=" + resCount + "\r\n");
        }
        
		StringWriter clientRes = new StringWriter();
		PrintWriter clientOut = new PrintWriter(clientRes);
        boolean isValid = cfg.getSessionManager().isValidToken(token);
        String username = cfg.getUsernameFromToken(token);
        User user = cfg.getUserManager().getUser(username);

		for (int t = 1; t<=resCount; t++) {
		    String parm = "res." + t;
            String res = request.getParameter(parm);
            String act = request.getParameter("act." + t);
            if (logReqWrt != null) {
                logReqWrt.println(parm + "=" + res);
                logReqWrt.println("act." + t + "=" + act);
            }
            // only process if we have both a resource and its action
			if (res != null && ! res.equals("") && act != null && ! act.equals("")) {
			    Map<String, String> ctx = getContextParams(cfg, request, t, logReqWrt);
			    boolean allowed = false;
			    if (isValid) { // only evaluate if token is still valid
			        allowed = cfg.getEntitlementsManager().isAllowed(act, res, user);
			    }
			    clientOut.println(parm + "=" + allowed);
			}
		}
		clientOut.flush();
		
		if (cfg.getTrafficRecorder().isRecordingRest()) {
		    logReqWrt.flush();
		    String logMsg = clientRes.toString() + "\r\n" + logReq.toString(); 
			Map<String,String> props = new HashMap<String,String>();
			cfg.getTrafficRecorder().recordRestHit(this.pathPrefix, 
					HttpServletResponse.SC_OK, logMsg, 
					props);
		}
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, clientRes.toString());
	}

	
	/**
	 * Converts context params for a specific resource into a map. Parameter 
	 * 'ctx.N.cnt' indicates the number of key/value pairs to be read. Each 
	 * key is then specified with 'ctx.N.M.key' and each value is specified with
	 * 'ctx.N.M.val' where M starts at one and runs to ctx.N.cnt. Returns null 
	 * if no ctx.N.cnt is seen, is not an integer, or is less than zero or if no
	 * parameters ended up being loaded. A key and value will only be injected
	 * into the map if both are included.
	 * 
	 * @param cfg
	 * @param request
	 * @param t
	 * @param logReqWrt 
	 * @return
	 */
    private Map<String, String> getContextParams(Config cfg, HttpServletRequest request,
            int t, PrintWriter logReqWrt) {
        String ctxCnt = request.getParameter("ctx." + t + ".cnt");
        
        if (ctxCnt == null || ctxCnt.equals("")) {
            return null;
        }
        
        int ctxCount = -1;

        try {
            ctxCount = Integer.parseInt(ctxCnt);
        }
        catch(NumberFormatException nfe) {
            if (logReqWrt != null) {
                logReqWrt.println("ctx." + t + ".cnt=" + ctxCnt);
            }
            return null; // ignore context parms if not an integer
        }
        if (ctxCount <= 0) {
            if (logReqWrt != null) {
                logReqWrt.println("ctx." + t + ".cnt=" + ctxCnt);
            }
            return null; 
        }
        Map<String,String> map = new HashMap<String,String>();
        
        for (int idx = 1; idx<=ctxCount; idx++) {
            String key = request.getParameter("ctx." + t + "." + idx + ".key");
            String val = request.getParameter("ctx." + t + "." + idx + ".val");
            if (logReqWrt != null) {
                logReqWrt.println("ctx." + t + "." + idx + ".key=" + key);
                logReqWrt.println("ctx." + t + "." + idx + ".val=" + val);
            }
            // only process if we have both a resource and its action
            if (key != null && ! key.equals("") && val != null && ! val.equals("")) {
                map.put(key, val);
            }
        }
        if (map.size() == 0) {
            map = null;
        }
        return map;
    }
}