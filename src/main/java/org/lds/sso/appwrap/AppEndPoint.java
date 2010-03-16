package org.lds.sso.appwrap;

import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.RequestLine;
import org.lds.sso.appwrap.proxy.StartLine;

/**
 * Represents the mapping from canonical space URLs to application space URLs
 * for an application that can be hit through the reverse proxy on a port on the
 * same box as the proxy.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class AppEndPoint implements EndPoint {
	private String canonicalContextRoot = null;

	private String applicationContextRoot = null;

	int endpointPort = -1;

	String host = null;

	private String id = null;

	public AppEndPoint(String canonicalCtx, String appCtx, String host, int port) {
		this.endpointPort = port;
		this.canonicalContextRoot = canonicalCtx;
		this.applicationContextRoot = appCtx;
		this.host = host;
		this.id = canonicalCtx + "->URI=" + host + ":" + port + appCtx;
	}

	public String getId() {
		return id;
	}
	
	public int compareTo(EndPoint o) {
		return id.compareTo(o.getId());
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof AppEndPoint)) {
			return false;
		}
		AppEndPoint ep = (AppEndPoint) obj;
		return id.equals(ep.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public String getCanonicalContextRoot() {
		return canonicalContextRoot;
	}

	public void setCanonicalContextRoot(String canonicalContextRoot) {
		this.canonicalContextRoot = canonicalContextRoot;
	}

	public String getApplicationContextRoot() {
		return applicationContextRoot;
	}

	public void setApplicationContextRoot(String applicationContextRoot) {
		this.applicationContextRoot = applicationContextRoot;
	}

	public int getEndpointPort() {
		return endpointPort;
	}

	public void setEndpointPort(int endpointPort) {
		this.endpointPort = endpointPort;
	}

	/**
	 * If canonical space rewriting is disabled then the value returned is the
	 * same as the one passed in, otherwise this method scans registered
	 * rewrites to see if this URL starts with any of their canonical prefixes
	 * and if so replaces the matching canonical portion with the app space
	 * replacement and appends a query string of cctx with the value of the
	 * replaced portion.
	 * 
	 * For example, suppose that a rewrite prefix were registered for:
	 * 
	 * canonical space URL prefix: /mls/mbr application space URL prefix:
	 * /mls-membership
	 * 
	 * Then a canonical URL of:
	 * 
	 * /mls/mbr/some/page.jsf?a=1&b=2
	 * 
	 * would be transformed into the application space URL of:
	 * 
	 * /mls-membership/some/page.jsf?a=1&b=2&cctx=/mls/mbr
	 * 
	 * And the canonical URL of:
	 * 
	 * /mls/mbr/some/page.jsf
	 * 
	 * would be transformed into the application space URL of:
	 * 
	 * /mls-membership/some/page.jsf?cctx=/mls/mbr
	 * 
	 * @param reqPkg
	 * @return
	 */
	public RequestLine getAppRequestUri(HttpPackage reqPkg) {
		if (canonicalContextRoot == null) { // no translation available
			return reqPkg.requestLine;
		}
		if (!reqPkg.requestLine.getUri().startsWith(canonicalContextRoot)) {
			return null;
		}

		StartLine appReqLn = new StartLine(reqPkg.requestLine.getMethod(), applicationContextRoot
				+ reqPkg.requestLine.getUri().substring(canonicalContextRoot.length()), reqPkg.requestLine
				.getHttpDecl());
		// reqPkg.headerBfr.append("cctx: " + canonicalContextRoot);

		/*
		 * versions prior to clientlib 1.1.4 used query param not header if
		 * (appReqLn.token2.indexOf('?') == -1) { // no query string, append one
		 * appReqLn.token2 += "?" + Config.CANONICAL_CTX_QPARAM_NAME + "=" +
		 * canonicalContextRoot; } else { // query string found, append to
		 * existing appReqLn.token2 += "&" + Config.CANONICAL_CTX_QPARAM_NAME +
		 * "=" + canonicalContextRoot; }
		 */
		return appReqLn;
	}

	public String getHost() {
		return host;
	}
}
