package org.lds.sso.appwrap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.lds.sso.plugins.policy.conditions.evaluator.LogicalSyntaxEvaluationEngine;
import org.lds.sso.appwrap.opensso.MyProvider;

import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * Manages in-memory registered ties between the canonical URL space and the
 * application URL space for applications running on a port on the local host
 * allowing the reverse proxy to rewrite the request URI line accordingly and
 * proxy the request to the application port. Also keeps track of unenforced
 * URLs and URLs permitted for authenticated users both of which only reside in
 * the canonical space. See TrafficType for definition.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class TrafficManager {
	protected static final LogicalSyntaxEvaluationEngine cEngine;
	
	protected static final Map<String, String> cSyntaxMap = new HashMap<String, String>();  

	private List<SiteMatcher> matchers = new ArrayList<SiteMatcher>();

	private SiteMatcher lastMatcherAdded = null;

	private Map<String,String> redirectRewrites = new HashMap<String,String>();

	/**
	 * Set up opensso's debug infrastructure to use custom implementation that
	 * wraps Log4j. Log4j logger created is
	 * org.lds.sso.plugins.policy.conditions
	 * .evaluator.LogicalSyntaxEvaluationEngine
	 */
	static {
		Properties p = new Properties();
		p.put("com.sun.identity.util.debug.provider", MyProvider.class.getName());
		SystemPropertiesManager.initializeProperties(p);
		cEngine = new LogicalSyntaxEvaluationEngine();
	}

	/**
	 * Determines if the passed-in url is an unenforeceUrl either starting with
	 * a configured url ending in an asterisk minus the asterisk or matching
	 * exactly a configured url not ending with an asterisk.
	 * 
	 * @param uri
	 * @param string
	 * @param i
	 * @return
	 */
	public boolean isUnenforced(String scheme, String host, int port, String path, String query) {
		for (SiteMatcher m : matchers) {
			if (m.isUnenforced(scheme, host, port, path, query)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines if the passed-in url is an unenforeceUrl either starting with
	 * a configured url ending in an asterisk minus the asterisk or matching
	 * exactly a configured url not ending with an asterisk.
	 * 
	 * @param uri
	 * @return
	 */
	public boolean isPermitted(String scheme, String host, int port, String action, String path, String query, User user) {
		for (SiteMatcher m : matchers) {
			if (m.isAllowed(scheme, host, port, action, path, query, user)) {
				return true;
			}
		}
		return false;
	}

	public void addMatcher(SiteMatcher m) {
		this.matchers.add(m);
		this.lastMatcherAdded = m;
	}

	public SiteMatcher getLastMatcherAdded() {
		return lastMatcherAdded;
	}

	public SiteMatcher getSite(String scheme, String host, int port, String path, String query) {
		for (SiteMatcher rm : matchers) {
			if (rm.matches(scheme, host, port, path, query)) {
				return rm;
			}
		}
		return null;
	}

	public boolean isUnenforced(String fullUri) throws URISyntaxException {
		URI u = new URI(fullUri);
		int port = u.getPort() == -1 ? 80 : u.getPort();
		String query = u.getQuery();
		if ("".equals(query)) {
			query = null;
		}
		return this.isUnenforced(u.getScheme(), u.getHost(), port, u.getPath(), query);
	}

	public boolean isPermitted(String action, String fullUri, User user) throws URISyntaxException {
		URI u = new URI(fullUri);
		int port = u.getPort() == -1 ? 80 : u.getPort();
		String query = u.getQuery();
		if ("".equals(query)) {
			query = null;
		}
		return this.isPermitted(u.getScheme(), u.getHost(), port, action, u.getPath(), query, user);
	}

	public void addRedirectRewrite(String from, String to) {
		this.redirectRewrites.put(from, to);
	}

	/**
	 * Takes the passed in absolute URL gleaned from a Location header to see
	 * if it matches any configured rewrite directives. Returns null if it does
	 * not match any directive and hence need not be rewritten. Returns the 
	 * a rewritten absolute URL if a match is found.
	 * 
	 * @param redirect
	 * @return
	 */
	public String rewriteRedirect(String redirect) {
		for (Map.Entry<String, String> ent : redirectRewrites.entrySet() ) {
			if (redirect.startsWith(ent.getKey())) {
				return ent.getValue() + redirect.substring(ent.getKey().length());
			}
		}
		return null; // match not found, don't rewrite
	}
}
