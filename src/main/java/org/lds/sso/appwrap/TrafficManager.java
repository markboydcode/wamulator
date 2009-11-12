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
	private List<UrlResourceMatcher> matchers = new ArrayList<UrlResourceMatcher>();
	private UrlResourceMatcher lastMatcherAdded = null;
	
	/**
	 * Determines if the passed-in url is an unenforeceUrl either starting with
	 * a configured url ending in an asterisk minus the asterisk or matching
	 * exactly a configured url not ending with an asterisk.
	 * 
	 * @param uri
	 * @return
	 */
	public boolean isUnenforced(String uri) {
		for(UrlResourceMatcher m : matchers) {
			if (m.isUnenforced(uri)) {
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
	public boolean isPermittedForAuthdUsers(String action, String uri) {
		for(UrlResourceMatcher m : matchers) {
			if (m.isAllowed(action, uri)) {
				return true;
			}
		}
		return false;
	}

	public void addMatcher(SiteMatcher m) {
		this.matchers.add(m);
		this.lastMatcherAdded  = m;
	}
	
	public UrlResourceMatcher getLastMatcherAdded() {
		return lastMatcherAdded;
	}

	public SiteMatcher getSite(String host, int port, String uri) {
		for(UrlResourceMatcher rm : matchers) {
			if (rm.matches(host, port, uri)) {
				return (SiteMatcher) rm; // need to fix this hardcoded cast
			}
		}
		return null;
	}
}
