package org.lds.sso.appwrap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.lds.sso.plugins.policy.conditions.evaluator.EvaluationContext;
import org.lds.sso.plugins.policy.conditions.evaluator.IEvaluator;
import org.lds.sso.plugins.policy.conditions.evaluator.LogicalSyntaxEvaluationEngine;

import com.sun.identity.policy.PolicyException;

public class SiteMatcher {
	private static final Logger cLog = Logger.getLogger(SiteMatcher.class);
	private String host;
	private int port;
	private Set<UnenforcedUri> unenforcedUrls = new TreeSet<UnenforcedUri>();
	private Set<AllowedUri> allowedUrls = new TreeSet<AllowedUri>();
	private Set<EndPoint> mappedEndPoints = new TreeSet<EndPoint>();

	protected Type type = Type.SITE;
	private String scheme;
	private LogicalSyntaxEvaluationEngine cEngine;
	protected Map<String, String> cSynMap;
	protected Map<AllowedUri, String> conditionsMap = new HashMap<AllowedUri, String>();

	public enum Type {
		SITE, SINGLE_UNENFORCED, SINGLE_RESTRICTED;
	}

	/**
	 * Create a matcher that allows multiple URLs to be configured within it.
	 * 
	 * @param host
	 * @param port
	 */
	public SiteMatcher(String scheme, String host, int port, TrafficManager trafficMgr) {
		this.setConditionEnv(trafficMgr.cEngine, trafficMgr.cSyntaxMap);
		this.scheme = scheme;
		this.host = host;
		this.port = port;
		this.type = Type.SITE;
	}

	/**
	 * Create a matcher that only matches on a single unenforced or restricted URL.
	 * 
	 * @param host
	 * @param port
	 * @param uu an instance of UnenforcedUri or AllowedUri
	 * @param condId the condition alias name if specified for an AllowedUri instance, can be null
	 * @param syntax the condition syntax if specified for an AllowedUri instance, can be null
	 */
	public SiteMatcher(String scheme, String host, int port, UnenforcedUri uu, String condId, String syntax, TrafficManager trafficMgr) {
		this.setConditionEnv(trafficMgr.cEngine, trafficMgr.cSyntaxMap);
		this.host = host;
		this.port = port;
		if (uu instanceof AllowedUri) {
			AllowedUri au = (AllowedUri) uu;
			this.type = Type.SINGLE_RESTRICTED;
			addAllowedUri(au, condId, syntax);
		}
		else {
			this.type = Type.SINGLE_UNENFORCED;
			addUnenforcedUri(uu);
		}
	}

	/**
	 * Create a matcher that only matches on a single restricted URL.
	 * 
	 * @param host
	 * @param port
	 * @param au the AllowedUri
	 * @param condId the condition alias name
	 * @param syntax the condition syntax
	 * @param uu
	 */
	public SiteMatcher(String host, int port, AllowedUri au, String condId, String syntax, TrafficManager trafficMgr) {
		this.setConditionEnv(trafficMgr.cEngine, trafficMgr.cSyntaxMap);
		this.host = host;
		this.port = port;
		addAllowedUri(au, condId, syntax);
		this.type = Type.SINGLE_RESTRICTED;
	}

	public boolean isAllowed(String scheme, String host, int port, String action, String path, String query, User user) {
		if (this.port == port && this.host.equals(host)) {
			for(AllowedUri au : allowedUrls) {
				if (au.matches(scheme, host, port, path, query) && au.allowed(action)) {
					String condId = conditionsMap.get(au);
					if (condId == null) { // no condition needs to be met
						return true;
					}
					else { // must further meet conditions for access
						String syntax = cSynMap.get(condId);
						IEvaluator evaluator = null;
						try {
							evaluator = cEngine.getEvaluator(syntax);
						}
						catch (PolicyException e) {
							cLog.error("Disallowing access to " 
									+ au + " since unable to obtain evaluator for condition alias "
									+ condId + " with syntax " + syntax + ". ", e);
							return false;
						}
						EvaluationContext ctx = new EvaluationContext(user, new HashMap<String,String>());
						try {
							return evaluator.isConditionSatisfied(ctx);
						}
						catch (PolicyException e) {
							cLog.error("Error occurred for " + au + " for user " + user.getUsername()
									+ " denying access.", e);
						}
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Finds a registered mapping to an endpoint or null if none is
	 * found matching the passed in canonical url.
	 * @param canUrl
	 * @return
	 */
	public EndPoint getEndpointForCanonicalUrl(String canUrl) {
		for(EndPoint ep : mappedEndPoints) {
			// TODO: will have to delegate into endpoint to evaluate when we
			// add support for backward references and env macros for fileendpoints.
			if (canUrl.startsWith(ep.getCanonicalContextRoot())) {
				return ep;
			}
		}
		return null;
	}

	/**
	 * Returns true if this site matches on the host, port, and uri.
	 */
	public boolean matches(String scheme, String host, int port, String path, String query) {
		switch(type) {
		case SINGLE_RESTRICTED:
			if (allowedUrls.iterator().next().matches(scheme, host, port, path, query)) {
				return true;
			}
			return false;
		case SINGLE_UNENFORCED:
			if (unenforcedUrls.iterator().next().matches(scheme, host, port, path, query)) {
				return true;
			}
			return false;
		case SITE:
			if (!this.host.equals(host) || this.port != port) {
				return false;
			}
			return true; 
		}
		// should never get here.
		return false;
	}
	
	public void addMapping(String canonicalContext, String targetHost, int targetPort, String targetPathCtx) {
		AppEndPoint ep = new AppEndPoint(canonicalContext, targetPathCtx, targetHost, targetPort);
		mappedEndPoints.add(ep);
	}

	public void addUnenforcedUri(UnenforcedUri uu) {
		if (type == Type.SITE) {
			unenforcedUrls.add(uu);
		}
		else if (type == Type.SINGLE_UNENFORCED) {
			if (this.unenforcedUrls.size() == 0) {
				unenforcedUrls.add(uu);
			}
			else {
				throw new IllegalArgumentException("Attempting to add additional unenforced uri " 
						+ uu + " to single-unenforced-resource site " + host + ":" + port);
			}
		}
		else if (type == Type.SINGLE_RESTRICTED) {
			throw new IllegalArgumentException("Attempting to add unenforced uri " 
					+ uu + " to single-restricted-resource site " + host + ":" + port);
		}
	}
	
	/**
	 * Determines if the passed-in url is an unenforeceUrl either starting with
	 * a configured url ending in an asterisk minus the asterisk or matching
	 * exactly a configured url not ending with an asterisk.
	 * @param query 
	 * 
	 * @param uri
	 * @return
	 */
	public boolean isUnenforced(String scheme, String host, int port, String path, String query) {
		for (UnenforcedUri uu : unenforcedUrls) {
			if (uu.matches(scheme, host, port, path, query)) {
				return true;
			}
		}
		return false;
	}

	public void addAllowedUri(AllowedUri au, String condId, String condSyntax) {
		if (type == Type.SITE) {
			this.allowedUrls.add(au);
		}
		else if (type == Type.SINGLE_RESTRICTED) {
			if (this.allowedUrls.size() == 0) {
				this.allowedUrls.add(au);
			}
			else {
				throw new IllegalArgumentException("Attempting to add additional restricted uri " 
						+ au + " to single-restricted-resource site " + host + ":" + port);
			}
		}
		else if (type == Type.SINGLE_UNENFORCED) {
			throw new IllegalArgumentException("Attempting to add retricted uri " 
					+ au + " to single-unenforced-resource site " + host + ":" + port);
		}
		if (condId != null && condSyntax != null) {
			cSynMap.put(condId, condSyntax);
			conditionsMap.put(au, condId);
		}
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getScheme() {
		return scheme;
	}

	public void setConditionEnv(LogicalSyntaxEvaluationEngine engine, Map<String,String>syntaxMap) {
		cEngine = engine;
		cSynMap = syntaxMap;
	}

	public void addFileMapping(String cctx, String file, String type) {
		EndPoint ep = new LocalFileEndPoint(cctx, file, type);
		mappedEndPoints.add(ep);
	}
}
