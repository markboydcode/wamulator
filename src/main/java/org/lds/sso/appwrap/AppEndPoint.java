package org.lds.sso.appwrap;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.mail.internet.MimeUtility;

import org.lds.sso.appwrap.conditions.evaluator.GlobalHeaderNames;
import org.lds.sso.appwrap.identity.User;
import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.RequestLine;
import org.lds.sso.appwrap.proxy.StartLine;
import org.lds.sso.appwrap.proxy.header.Header;
import org.lds.sso.appwrap.proxy.header.HeaderDef;

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
	private static final Logger cLog = Logger.getLogger(AppEndPoint.class.getName());

	/**
	 * Represents the http scheme over tcp.
	 */
    public static final String HTTP_SCHEME = "http";
    
    /**
     * Represents the http scheme over tls over tcp.
     */
    public static final String HTTPS_SCHEME = "https";

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

    private String hostHdr;

    private String policyServiceGateway;

    private String scheme;

	/**
	 * Returns the name of the header that should be injected to convey that 
	 * the request was received over http or https. Defaults to X-Forwarded-Scheme.
	 *  
	 * @return
	 */
	private String schemeHeaderName = "X-Forwarded-Scheme";

	/**
	 * Indicates if a scheme header should be injected for this application 
	 * such as X-Forwarded-Scheme. Defaults to true.
	 * 
	 * @return
	 */
	private boolean injectSchemeHeader = true;

	protected Map<String, List<String>> fixedHeaders;

	protected Map<String, String> profileHeaders;

	public AppEndPoint(String canonicalHost, String canonicalCtx, String appCtx, 
	        String host, int port, 
	        boolean preserveHost, String hostHdr, String policyServiceGateway) {
	    this(canonicalHost, canonicalCtx, appCtx, host, port, 
	            HTTP_SCHEME, // default to http scheme for backward compatibility 
	            preserveHost, hostHdr, policyServiceGateway);
	}
	
	public AppEndPoint(String canonicalHost, String canonicalCtx, String appCtx, 
	        String host, int port, String scheme, 
	        boolean preserveHost, String hostHdr,
            String policyServiceGateway) {
        this.endpointPort = port;
        this.canonicalHost = canonicalHost;
        this.canonicalContextRoot = canonicalCtx;
        this.applicationContextRoot = appCtx;
        this.host = host;
        if (scheme != null && scheme.toLowerCase().equals(AppEndPoint.HTTPS_SCHEME)) {
            this.scheme = AppEndPoint.HTTPS_SCHEME;
        }
        else { // the default
            this.scheme = AppEndPoint.HTTP_SCHEME;
        }
        this.preserveHostHeader = preserveHost;
        this.hostHdr = hostHdr;
        
        if (hostHdr != null && ! "".equals(hostHdr)) {
            this.preserveHostHeader = false;
        }
        this.policyServiceGateway = policyServiceGateway;
        updateId();
    }

	/**
	 * Constructor that allows us to override default x-forwarded-scheme
	 * header behavior.
	 * 
	 * @param host
	 * @param cctx
	 * @param tpath
	 * @param thost
	 * @param tport
	 * @param scheme
	 * @param preserve
	 * @param hostHdr
	 * @param policyServiceGateway
	 * @param injectScheme
	 * @param schemeHeader
	 */
    public AppEndPoint(String host, String cctx, String tpath, String thost, int tport, String scheme,
			boolean preserve, String hostHdr, String policyServiceGateway, boolean injectScheme,
			String schemeHeader) {
    	this(host, cctx, tpath, thost, tport, scheme, preserve, hostHdr, policyServiceGateway);
    	this.injectSchemeHeader = injectScheme;
    	if (schemeHeader != null) {
        	this.schemeHeaderName = schemeHeader;
    	}
	}

	private void updateId() {
        this.id = this.canonicalHost + this.canonicalContextRoot + "->URI=" + host + ":" + endpointPort + applicationContextRoot;
	}
    
    public boolean useHttpsScheme() {
        return this.scheme.equals(HTTPS_SCHEME);
    }

    public String getCanonicalHost() {
        return this.canonicalHost;
    }
    
    public String getPolicyServiceGateway() {
        return this.policyServiceGateway;
    }
    
	public String getHostHeader() {
	    return this.hostHdr;
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
	 * @throws MalformedURLException 
	 */
	public RequestLine getAppRequestUri(HttpPackage reqPkg) throws MalformedURLException {
		if (canonicalContextRoot == null) { // no translation available
			return reqPkg.requestLine;
		}
		if (!reqPkg.requestLine.getUri().toLowerCase().startsWith(canonicalContextRoot.toLowerCase())) {
			return null;
		}
		cLog.fine("REWRITE: Re-writing request URL, replacing canonical context root: " + canonicalContextRoot +
					" with application context root: " + applicationContextRoot);

        StartLine appReqLn = new StartLine(reqPkg.requestLine.getMethod(),
                applicationContextRoot
                        + reqPkg.requestLine.getUri().substring(
                                canonicalContextRoot.length()),
                reqPkg.requestLine.getHttpDecl());
		return appReqLn;
	}

	public String getHost() {
		return host;
	}

	public void setFixedHeaders(Map<String, List<String>> fixedHdrs) {
		this.fixedHeaders = fixedHdrs;
	}

	public void setProfileHeaders(Map<String, String> profileHdrs) {
		this.profileHeaders = profileHdrs;
	}

    /**
     * Injects the policy-service-url pointing to the one for the by-site element
     * that contained our context mapping end point possibly with adjusted
     * gateway host and port if specified like when server is behind a firewall
     * and can't get to rest service without a reverse proxy tunnel
     * @param cfg 
     *
     * @param appEndpoint
     * @param reqPkg
     */
    private void injectPolicyServiceUrlHdr(Config cfg, HttpPackage reqPkg) {
        // first remove any injected header
        Header hdr = new Header(GlobalHeaderNames.SERVICE_URL, "");
        reqPkg.headerBfr.removeExtensionHeader(GlobalHeaderNames.SERVICE_URL);

        String hdrBase = "http://";
        if (getPolicyServiceGateway() == null) {
            hdrBase += getCanonicalHost() + ":" + cfg.getConsolePort();
        }
        else {
            hdrBase += this.getPolicyServiceGateway();
        }
        switch(cfg.getRestVersion()) {
        case OPENSSO:
            hdr.setValue(hdrBase + cfg.getRestVersion().getRestUrlBase());
            break;
        case CD_OESv1:
            hdr.setValue(hdrBase + cfg.getRestVersion().getRestUrlBase() + getCanonicalHost() + "/");
        }
        reqPkg.headerBfr.append(hdr);
    }

	/**
	 * Injects/adjusts headers specific for the endpoint.
	 * 
	 * @param cfg
	 * @param reqPkg
	 * @param user
	 * @param isSecure
	 */
	public void injectHeaders(Config cfg, HttpPackage reqPkg, User user, boolean isSecure) {
		injectProfileHeaders(user, reqPkg);
		adjustHostHdr(reqPkg);
		injectSchemeHeader(isSecure, reqPkg);
		injectPolicyServiceUrlHdr(cfg, reqPkg);
		injectFixedHdrs(reqPkg);
	}
	
	/**
	 * Injects headers with values from the a User's attributes.
	 * 
	 * @param user
	 * @param reqPkg
	 */
	private void injectProfileHeaders(User user, HttpPackage reqPkg) {
		if (profileHeaders == null) {
			return;
		}

		for(Entry<String, String> ent : this.profileHeaders.entrySet()) {
			String hdrName = ent.getKey();
			String attName = ent.getValue();
			// first scrub existing to prevent injection
			reqPkg.headerBfr.removeHeader(hdrName);
			// now inject
			if (user != null) {
				NvPair[] atts = user.getAttribute(attName);
				for(NvPair pair : atts) {
					String val = pair.getValue();
					try {
	                    val = MimeUtility.encodeText(val, "utf-8", null);
	                } catch (UnsupportedEncodingException e) {
	                    cLog.warning("Unsupported Encoding specified for header '"
	                            + pair.getName() + "'. Leaving as unencoded.");
	                }
					reqPkg.headerBfr.append(new Header(hdrName, val));
				}
			}
		}
	}

	/**
	 * Injects any fixed headers declared in config file.
	 * 
	 * @param reqP
	 */
	private void injectFixedHdrs(HttpPackage reqPkg) {
		if (fixedHeaders == null) {
			return;
		}
		for(Entry<String, List<String>> hdr : fixedHeaders.entrySet()) {
			// first scrub existing to prevent injection
			reqPkg.headerBfr.removeHeader(hdr.getKey());
			// now inject
			for(String val : hdr.getValue()) {
				reqPkg.headerBfr.append(new Header(hdr.getKey(), val));
			}
		}
	}

	/**
	 * Optionally injects a header to indicate by which scheme a request was 
	 * received.
	 * 
	 * @param isSecure
	 * @param reqPkg
	 */
	private void injectSchemeHeader(boolean isSecure, HttpPackage reqPkg) {
        // first remove any injected header
        reqPkg.headerBfr.removeHeader(schemeHeaderName);
        
        // indicate via headers over which scheme the client request was received 
        if (injectSchemeHeader) {
            if (isSecure) {
                reqPkg.headerBfr.append(new Header(schemeHeaderName, "https"));
                reqPkg.scheme = "https";
            }
            else { 
                reqPkg.headerBfr.append(new Header(schemeHeaderName, "http"));
                reqPkg.scheme = "http";
            }
        }
	}

	/**
	 * Adjusts the Host header and injects X-Forwarded-Host if needed.
	 * 
	 * @param reqPkg
	 */
	private void adjustHostHdr(HttpPackage reqPkg) {
        if (! this.preserveHostHeader) {
            Header hhdr = reqPkg.headerBfr.getHeader(HeaderDef.Host);
            String h = reqPkg.hostHdr;
            String hostHdr = (getHostHeader() != null ?
                    getHostHeader() :
                        (getHost()
                                + (getEndpointPort() != 80
                                        ? (":" + getEndpointPort())
                                                : "")));
            if (hhdr != null && ! hhdr.getValue().equals(hostHdr)) {
                reqPkg.headerBfr.set(new Header("X-Forwarded-Host", hhdr.getValue()));
            }
            reqPkg.headerBfr.set(new Header(HeaderDef.Host, hostHdr));
        }
	}
}
