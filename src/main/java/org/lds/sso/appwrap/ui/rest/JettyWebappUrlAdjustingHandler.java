package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.io.LogUtils;
import org.mortbay.jetty.handler.HandlerWrapper;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.Resource;

/**
 * Performs major hacking to fix an anomaly that occurs when jetty is run from
 * within a jar and must extract its webapp to be able to compile jsp pages.
 * Somehow an additional "webapp" directory gets introduced into the real
 * resource path and the targeted resource is never found unless all resources
 * in the configured web application directory are copied up one layer. 
 * 
 * This class detects that anomaly and adjusts the target accordingly so that 
 * the resources are resolved correctly.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class JettyWebappUrlAdjustingHandler extends HandlerWrapper {
	private static final Logger cLog = Logger.getLogger(JettyWebappUrlAdjustingHandler.class.getName());
	
	/**
	 * The jetty object representing our webapp.
	 */
	private WebAppContext wac = null;
	/**
	 * The url webapp context base path at which our web app is rooted. ie: the
	 * first sub path item after the host and port showing in the browser when
	 * hitting our application.
	 */
	private String ctxPath = null;
	/**
	 * The directory of our webapp relative to the application's filesystem 
	 * location. Jetty docs are not real clear so this is my interpretation. 
	 */
	private String webappDir = null;

	/**
	 * Indicates if we have determined that adjustment is needed for webapp
	 * destined urls or not.
	 */
	private boolean initialized = false;
	
	/**
	 * Indicates if adjustment is needed for all webapp destined targets.
	 */
	private boolean adjustTargets = false;
	
	public JettyWebappUrlAdjustingHandler(String ctx, String webappDir, WebAppContext wac) {
		this.wac = wac;
		this.ctxPath = ctx;
		this.webappDir = webappDir;
		this.addHandler(wac);
	}
	
	public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
			throws IOException, ServletException {
		
		// see if this is a URL headed for our admin webapp
		if (target.startsWith(ctxPath)) {
			// see if we have already determined if adjustment is needed
			if (! initialized) {
				LogUtils.fine(cLog, "Identifying Target Adjustment Hack Requirement");
				String relTarget = target.substring(ctxPath.length());
				Resource res = wac.getResource(relTarget);
				boolean foundRes = res.exists();
				
				String adjTarget = "/" + webappDir + relTarget;
				Resource adj = wac.getResource(adjTarget);
				boolean foundAdj = adj.exists();
				
				if (foundRes == false && foundAdj) {
					this.adjustTargets = true;
				}
				else {
					this.adjustTargets = false;
				}
				initialized = true;
				LogUtils.fine(cLog, "webapp bound target = {0}", target);
				LogUtils.fine(cLog, "{0}found {1} @ {2}", (foundRes ? "" : "NOT "), relTarget, res);
				LogUtils.fine(cLog, "{0}found {1} @ {2}", (foundAdj ? "" : "NOT "), adjTarget, adj);
				LogUtils.info(cLog, "Determined Target Adjustment is {0}REQUIRED. Webapp Targets will {1}be adjusted.",
						(adjustTargets ? "" : "NOT "), (adjustTargets ? "" : "NOT "));
			}
			if (adjustTargets) {
				String relTarget = target.substring(ctxPath.length());
				String adjTarget = "/" + webappDir + relTarget;
				target = ctxPath + adjTarget;
			}
			super.handle(target, request, response, dispatch);
		}
		// if we don't handle it the next handler in the containing list gets a turn.
		// so it is ok to just drop out of here if not headed for the webapp.
	}
}
