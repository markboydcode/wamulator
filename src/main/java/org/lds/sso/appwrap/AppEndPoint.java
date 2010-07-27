package org.lds.sso.appwrap;

import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.RequestLine;
import org.lds.sso.appwrap.proxy.StartLine;

/**
 * Represents the mapping from canonical space URLs to application space URLs
 * for an application that can be hit through the reverse proxy not necessarily 
 * on a port on the same box as the proxy.
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

    /**
     * If true the host header of requests will be passed as-is. When false the
     * host header will be rewritten to the value of the endpoint host + colon +
     * endpoint port.
     */
    private boolean preserveHostHeader;

    /**
     * A number that indicates the order of declaration in the configuation file
     * in xml document order within a by-site element for use in comparable 
     * operations allowing endpoint with hierarchically nested canonical urls
     * to coexist but allow for the nested one to be declared first and match
     * without being lost by the endpoint with the containing URL.
     */
    private Integer declarationOrder = null;

    /**
     * The host of the by-site element within which this context mapping 
     * application endpoint resides to facilitate exposing a rest service specific
     * to the by-site element and injecting a corresponding policy-service-url.
     */
    private String canonicalHost;

	public AppEndPoint(String canonicalHost, String canonicalCtx, String appCtx, String host, int port, boolean preserveHost) {
		this.endpointPort = port;
        this.canonicalHost = canonicalHost;
        this.canonicalContextRoot = canonicalCtx;
		this.applicationContextRoot = appCtx;
		this.host = host;
		this.preserveHostHeader = preserveHost;
		updateId();
	}
	
	private void updateId() {
        this.id = this.canonicalHost + this.canonicalContextRoot + "->URI=" + host + ":" + endpointPort + applicationContextRoot;
	}

	public String getCanonicalHost() {
	    return this.canonicalHost;
	}
	
	public String getId() {
		return id;
	}
	
    public void setDeclarationOrder(int index) {
        this.declarationOrder = new Integer(index);
    }
    
	public Integer getDeclarationOrder() {
	    return declarationOrder;
	}
	
	public int compareTo(EndPoint o) {
		return declarationOrder.compareTo(o.getDeclarationOrder());
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

	public boolean preserveHostHeader() {
	    return this.preserveHostHeader;
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

    /**
     * Updates the configured port with the passed-in value if the endpointPort
     * is zero indicating that its value was specified as the macro
     * {{console-port}} and the console-port was specified as being "auto"
     * configured meaning it is bound at start-up to an available port.
     * 
     * @return
     */
	public void setEndpointPort(int endpointPort) {
	    if (this.endpointPort == 0) {
	        this.endpointPort = endpointPort;
	        updateId();
	    }
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
		return appReqLn;
	}

	public String getHost() {
		return host;
	}
}
