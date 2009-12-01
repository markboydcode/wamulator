package org.lds.sso.appwrap;

import java.util.Set;
import java.util.TreeSet;

public class SiteMatcher {

	private String host;
	private int port;
	private Set<UnenforcedUri> unenforcedUrls = new TreeSet<UnenforcedUri>();
	private Set<AllowedUri> allowedUrls = new TreeSet<AllowedUri>();
	private Set<AppEndPoint> mappedEndPoints = new TreeSet<AppEndPoint>();

	protected Type type = Type.SITE;
	private String scheme;

	public enum Type {
		SITE, SINGLE_UNENFORCED, SINGLE_RESTRICTED;
	}

	/**
	 * Create a matcher that maches on all URLs for a host and port.
	 * 
	 * @param host
	 * @param port
	 */
	public SiteMatcher(String scheme, String host, int port) {
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
	 * @param uu
	 */
	public SiteMatcher(String scheme, String host, int port, UnenforcedUri uu) {
		this.host = host;
		this.port = port;
		if (uu instanceof AllowedUri) {
			AllowedUri au = (AllowedUri) uu;
			this.type = Type.SINGLE_RESTRICTED;
			addAllowedUri(au);
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
	 * @param uu
	 */
	public SiteMatcher(String host, int port, AllowedUri au) {
		this.host = host;
		this.port = port;
		addAllowedUri(au);
		this.type = Type.SINGLE_RESTRICTED;
	}

	public boolean isAllowed(String scheme, String host, int port, String action, String path, String query) {
		if (this.port == port && this.host.equals(host)) {
			for(AllowedUri au : allowedUrls) {
				if (au.matches(scheme, host, port, path, query) && au.allowed(action)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Finds a registered mapping to an application endpoint or null if none is
	 * found matching the passed in canonical url.
	 * @param canUrl
	 * @return
	 */
	public AppEndPoint getAppEndpointForCanonicalUrl(String canUrl) {
		for(AppEndPoint ep : mappedEndPoints) {
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

	public void addAllowedUri(AllowedUri au) {
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
}
