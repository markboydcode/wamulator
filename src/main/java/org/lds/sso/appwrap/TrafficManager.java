package org.lds.sso.appwrap;

import org.lds.sso.appwrap.AppEndPoint.InboundScheme;
import org.lds.sso.appwrap.AppEndPoint.Scheme;
import org.lds.sso.appwrap.conditions.evaluator.LogicalSyntaxEvaluationEngine;
import org.lds.sso.appwrap.identity.SessionManager;
import org.lds.sso.appwrap.identity.User;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	protected LogicalSyntaxEvaluationEngine cEngine = null;
	
	protected Map<String, String> cSyntaxMap = null;  

	protected List<SiteMatcher> matchers = new ArrayList<SiteMatcher>();

	private SiteMatcher lastMatcherAdded = null;

    private SiteMatcher masterSite;

	public TrafficManager(LogicalSyntaxEvaluationEngine eng,
            Map<String, String> syntax) {
	    this.cEngine = eng;
	    this.cSyntaxMap = syntax;
    }

    /**
     * Propagate into SiteMatchers a change in proxy port allowing for late
     * binding when "auto" is used for the proxy port.
     *  
     * @param proxyPort
     */
    public void proxyPortChanged(int proxyPort) {
        for (SiteMatcher m : matchers) {
            m.proxyPortChanged(proxyPort);
        }
    }
    
    /**
     * Propagate into SiteMatchers a change in console port allowing for late
     * binding when "auto" is used for the console port.
     *  
     * @param consolePort
     */
    public void consolePortChanged(int consolePort) {
        for (SiteMatcher m : matchers) {
            m.consolePortChanged(consolePort);
        }
    }

    /**
     * Determines if the passed-in url is an unenforced Url.
     * 
     */
    public boolean isUnenforced(Scheme scheme, String host, int port,
            String path, String query) {
        SiteMatcher m = getSite(scheme, host, port);
        if (m != null && m.isUnenforced(scheme, host, port, path, query)) {
            return true;
        }
        return false;
    }

    /**
     * Determines if the passed-in url is an enforced Url.
     * 
     */
    public boolean isEnforced(Scheme scheme, String host, int port,
            String path, String query) {
        SiteMatcher m = getSite(scheme, host, port);
        if (m != null && m.isEnforced(scheme, host, port, path, query)) {
            return true;
        }
        return false;
    }

	/**
	 * Determines if the passed-in url is permitted by the user.
	 * 
	 */
	public boolean isPermitted(Scheme scheme, String host, int port, String action, String path, String query, User user) {
	    SiteMatcher m = getSite(scheme, host, port);
	    if (m != null && m.isAllowed(scheme, host, port, action, path, query, user)) {
            return true;
	    }
		return false;
	}

	/**
	 * Adds a SiteMatcher which is the embodiment of each by-site directive in 
	 * the configuration file.
	 * 
	 * @param m
	 */
	public void addMatcher(SiteMatcher m) {
		this.matchers.add(m);
		this.lastMatcherAdded = m;
		if (this.masterSite == null) {
		   String host = m.getHost();
		   SessionManager smgr = Config.getInstance().getSessionManager();
		   String md = smgr.getMasterCookieDomain();
		   if (md.startsWith(".")) {
		       if (host.endsWith(md)) {
		           masterSite = m;
		       }
		   }
		   else {
		       if (host.equals(md)) {
                   masterSite = m;
		       }
		   }
		}
	}

	public SiteMatcher getLastMatcherAdded() {
		return lastMatcherAdded;
	}

	/**
	 * Finds a SiteMatcher that has the same configuration of InboundScheme 
	 * (which could be of type BOTH), host and port.
	 * 
	 * @param scheme
	 * @param host
	 * @param port
	 * @return
	 */
	public SiteMatcher getSite(InboundScheme scheme, String host, int port) {
		for (SiteMatcher rm : matchers) {
			if (rm.isSame(scheme, host, port)) {
				return rm;
			}
		}
		return null;
	}

	/**
	 * Finds a SiteMatcher for matching against URLs which only have a scheme of
	 * http or https.
	 * 
	 * @param scheme
	 * @param host
	 * @param port
	 * @return
	 */
	public SiteMatcher getSite(Scheme scheme, String host, int port) {
		for (SiteMatcher rm : matchers) {
			if (rm.matches(scheme, host, port)) {
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
        return this.isUnenforced(Scheme.fromMoniker(u.getScheme()), u.getHost(), port, u.getPath(), query);
    }

    public boolean isEnforced(String fullUri) throws URISyntaxException {
        URI u = new URI(fullUri);
        int port = u.getPort() == -1 ? 80 : u.getPort();
        String query = u.getQuery();
        if ("".equals(query)) {
            query = null;
        }
        return this.isEnforced(Scheme.fromMoniker(u.getScheme()), u.getHost(), port, u.getPath(), query);
    }

	public boolean isPermitted(String action, String fullUri, User user) throws URISyntaxException {
		URI u = new URI(fullUri);
		int port = u.getPort() == -1 ? 80 : u.getPort();
		String query = u.getQuery();
		if ("".equals(query)) {
			query = null;
		}
		return this.isPermitted(Scheme.fromMoniker(u.getScheme()), u.getHost(), port, action, u.getPath(), query, user);
	}

    public List<SiteMatcher> getSites() {
        return this.matchers;
    }
    
    /**
     * Gets the site that is configured to be the master site meaning the site
     * in which the single authority authentication sign-in page is exposed and
     * to which other cdsso domains will redirect for authentication.
     * 
     * @return
     */
    public SiteMatcher getMasterSite() {
        return this.masterSite;
    }
}
