package org.lds.sso.appwrap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.sun.org.apache.xerces.internal.util.URI;
import com.sun.org.apache.xerces.internal.util.URI.MalformedURIException;

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
	private List<SiteMatcher> matchers = new ArrayList<SiteMatcher>();
	private SiteMatcher lastMatcherAdded = null;
	
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
		for(SiteMatcher m : matchers) {
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
	public boolean isPermitted(String scheme, String host, int port, String action, String path, String query) {
		for(SiteMatcher m : matchers) {
			if (m.isAllowed(scheme, host, port, action, path, query)) {
				return true;
			}
		}
		return false;
	}

	public void addMatcher(SiteMatcher m) {
		this.matchers.add(m);
		this.lastMatcherAdded  = m;
	}
	
	public SiteMatcher getLastMatcherAdded() {
		return lastMatcherAdded;
	}

	public SiteMatcher getSite(String scheme, String host, int port, String path, String query) {
		for(SiteMatcher rm : matchers) {
			if (rm.matches(scheme, host, port, path, query)) {
				return (SiteMatcher) rm; // need to fix this hardcoded cast
			}
		}
		return null;
	}

	public boolean isUnenforced(String fullUri) throws MalformedURIException {
		URI u = new URI(fullUri);
		int port = u.getPort() == -1 ? 80 : u.getPort();
		String query = u.getQueryString();
		if ("".equals(query)) {
			query = null;
		}
		return this.isUnenforced(u.getScheme(), u.getHost(), port, u.getPath(), query);
	}

	public boolean isPermitted(String action, String fullUri) throws MalformedURIException {
		URI u = new URI(fullUri);
		int port = u.getPort() == -1 ? 80 : u.getPort();
		String query = u.getQueryString();
		if ("".equals(query)) {
			query = null;
		}
		return this.isPermitted(u.getScheme(), u.getHost(), port, action, u.getPath(), query);
	}
}
