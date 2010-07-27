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
import org.lds.sso.appwrap.proxy.RequestHandler;
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
public class EncodeLinks extends RestHandlerBase {
	static final Logger cLog = Logger.getLogger(EncodeLinks.class);

	public EncodeLinks(String pathPrefix) {
		super(pathPrefix);
	}

	@Override
	protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException {
		Config cfg = Config.getInstance();
        String res = request.getParameter("res");
        
        if (res == null || res.equals("")) {
            if (cfg.getTrafficRecorder().isRecordingRest()) {
                Map<String,String> props = new HashMap<String,String>();
                cfg.getTrafficRecorder().recordRestHit(this.pathPrefixRaw, 
                        HttpServletResponse.SC_BAD_REQUEST, "no res specified", 
                        props);
            }
            super.sendResponse(cLog, response, HttpServletResponse.SC_BAD_REQUEST, "no res specified");
            return;
        }
        String nRes = EncodeUtils.encode(EncodeUtils.clean(res));
        
        if (cfg.getTrafficRecorder().isRecordingRest()) {
            // build request log chunk
            StringWriter logReq = new StringWriter();
            PrintWriter logReqWrt = new PrintWriter(logReq);
            logReqWrt.print(nRes);
            logReqWrt.print("\r\n");
            logReqWrt.print("\r\n");
            logReqWrt.print(" for request:\r\n");// leave \r\n since println is op sys specific
            logReqWrt.print("res=");
            logReqWrt.print(res);
            logReqWrt.print("\r\n");
            Map<String,String> props = new HashMap<String,String>();
            cfg.getTrafficRecorder().recordRestHit(this.pathPrefixRaw, 
                    HttpServletResponse.SC_OK, logReq.toString(), 
                    props);
        }
		super.sendResponse(cLog, response, HttpServletResponse.SC_OK, nRes + RequestHandler.CRLF);
	}
}
