package org.lds.sso.appwrap;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.HashMap;
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
	 * The scheme of packages once inside the wamulator meaning only http or
	 * https indicating on which scheme the traffic entered or received.
	 * 
	 * @author BoydMR
	 * 
	 */
	public enum Scheme {
		/**
		 * Represents the http scheme over tcp.
		 */
		HTTP("http"),

		/**
	     * Represents the http scheme over tls over tcp.
	     */
		HTTPS("https");
		
		public final String moniker;
		private static Map<String, Scheme> schemes;

		Scheme(String moniker) {
			this.moniker = moniker;
			addMapping(moniker);
		}
		
		private void addMapping(String mon) {
			if (Scheme.schemes == null) {
				Scheme.schemes = new HashMap<String, Scheme>();
			}
			Scheme.schemes.put(mon, this);
		}
		
		public String getMoniker() {
			return moniker;
		}

		public static Scheme fromMoniker(String mon) {
			Scheme s = schemes.get(mon);
			if (s == null) {
				s = HTTP;
			}
			return s;
		}
	}

	/**
	 * The allowed values for inbound scheme related configuration such as
	 * by-site's scheme attribute and cctx-mapping's cscheme attribute. For
	 * incoming matching HTTP, HTTPS, and BOTH are valid config values meaning
	 * only incoming connections will respectively match an enpoint if the
	 * scheme is 'http', 'https', or either.
	 * 
	 * @author BoydMR
	 * 
	 */
	public enum InboundScheme {
		/**
		 * Represents the http scheme over tcp.
		 */
		HTTP("http"),

		/**
	     * Represents the http scheme over tls over tcp.
	     */
		HTTPS("https"),
		
		/**
		 * Indicates that either HTTP or HTTPS scheme will match for this
		 * endpoing if other aspects match.
		 */
		BOTH("both");
		
		public final String moniker;
		private static Map<String, InboundScheme> schemes;
		
		InboundScheme(String moniker) {
			this.moniker = moniker;
			addMapping(moniker);
		}
		
		private void addMapping(String mon) {
			if (InboundScheme.schemes == null) {
				InboundScheme.schemes = new HashMap<String, InboundScheme>();
			}
			InboundScheme.schemes.put(mon, this);
		}

		public String getMoniker() {
			return moniker;
		}
		
		public static InboundScheme fromMoniker(String mon) {
			InboundScheme s = schemes.get(mon);
			if (s == null) {
				s = HTTP;
			}
			return s;
		}
	}
	
	/**
	 * The allowed values for outbound scheme related configuration. For 
	 * outgoing connections HTTP and HTTPS respectively indicate 
	 * the scheme that should be used regardless of what was used on incoming
	 * connections while SAME indicates that the scheme used on the incoming
	 * connection should be used on the outgoing connection.
	 * 
	 * @author BoydMR
	 *
	 */
	public enum OutboundScheme {
		/**
		 * Represents the http scheme over tcp.
		 */
		HTTP("http"),

		/**
	     * Represents the http scheme over tls over tcp.
	     */
		HTTPS("https"),
		
		/**
		 * Indicates that whatever scheme was used for the incoming connection
		 * will also be used on the outgoing connection.
		 */
		SAME("same");
		
		public final String moniker;
		private static Map<String, OutboundScheme> schemes;

		OutboundScheme(String moniker) {
			this.moniker = moniker;
			addMapping(moniker);
		}
		
		private void addMapping(String mon) {
			if (OutboundScheme.schemes == null) {
				OutboundScheme.schemes = new HashMap<String, OutboundScheme>();
			}
			OutboundScheme.schemes.put(mon, this);
		}

		public String getMoniker() {
			return moniker;
		}
		
		public static OutboundScheme fromMoniker(String mon) {
			OutboundScheme s = schemes.get(mon);
			if (s == null) {
				s = HTTP;
			}
			return s;
		}
	}

    /**
     * The name of the cctx header to be injected with all requests.
     */
    public static final String CCTX_HEADER = "cctx";
    
    /**
     * The cctx header to be injected with all requests.
     */
	private Header cctxHeader = null;

	private String contextRoot = null;
	
	private String queryString = null;

	int endpointPort = -1;
	
	/**
	 * The tls port only if multiple ports are used. If the tscheme is http or
	 * https then only one port is needed and will be found in endpointPort. 
	 * This variable is only needed for when tscheme is 'same' indicating that
	 * the scheme used on the incoming connection should also be used on the 
	 * outgoing connection.
	 */
	int endpointTlsPort = -1;

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
     * The scheme that must be used by incoming connections to map to this
     * endpoint router. If 'both' then it will match on incoming connections for
     * http and https. If 'https' then it will only match on https connections. 
     * Any other value or not specifying will default to http.
     */
	private InboundScheme canonicalScheme;

    /**
     * The host of the by-site element within which this context mapping 
     * application endpoint resides to facilitate exposing a rest service specific
     * to the by-site element and injecting a corresponding policy-service-url.
     */
    private String canonicalHost;

    private String hostHdr;

    private String policyServiceGateway;
    
    // Strings that denote where this endpoint was defined
    // in the config files for logging/debugging.
    private String sourceName;
    private String policyPath;

    /**
     * The scheme that should be used when connecting to the remote endpoint.
     * Possible values are 'http' and 'https' but also 'same' which indicates
     * that whatever scheme was used on the incoming connection should be used
     * on the outgoing connection to the remote endpoint. The default is 'same'.
     */
    private OutboundScheme appScheme;

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
	
	protected List<String> purgedHeaders;

	public AppEndPoint(String canonicalHost, String cctx, String host, int port, 
	        boolean preserveHost, String hostHdr, String policyServiceGateway,
            String sourceName,  String policyPath) {
	    
		this(null, canonicalHost, cctx, host, port, OutboundScheme.HTTP, -1, 
	            preserveHost, hostHdr, policyServiceGateway, sourceName, policyPath);
	}
	
	public AppEndPoint(InboundScheme canonicalScheme, String canonicalHost, String cctx, 
	        String host, int port, OutboundScheme appScheme, int outgoingTlsPort,
	        boolean preserveHost, String hostHdr, String policyServiceGateway,
            String sourceName, String policyPath) {
		
		this(canonicalScheme, canonicalHost, cctx, null, host, port, appScheme, outgoingTlsPort,
		     preserveHost, hostHdr, policyServiceGateway, sourceName, policyPath);
	}
	
	public AppEndPoint(InboundScheme canonicalScheme, String canonicalHost, String cctx, 
	        String queryString, String host, int port, OutboundScheme appScheme, int outgoingTlsPort,
	        boolean preserveHost, String hostHdr, String policyServiceGateway,
            String sourceName, String policyPath) {
        
		this.endpointPort = port;
        this.endpointTlsPort = outgoingTlsPort;
        this.canonicalScheme = (canonicalScheme == null || 
        		canonicalScheme.getMoniker().toLowerCase().equals(InboundScheme.HTTPS.moniker) ? InboundScheme.HTTPS : InboundScheme.HTTP);
        this.canonicalHost = canonicalHost;
        this.contextRoot = cctx;
        if (cctx != null) {
        	String context = cctx;
        	if (cctx.endsWith("/")) {
        		context = cctx.substring(0,cctx.lastIndexOf('/'));
        	}
            this.cctxHeader = new Header(CCTX_HEADER, context);
        }
        this.queryString = queryString;
        this.host = host;
        if (appScheme != null && appScheme.moniker.toLowerCase().equals(OutboundScheme.HTTPS.moniker)) {
            this.appScheme = OutboundScheme.HTTPS;
        }
        else { // the default
            this.appScheme = OutboundScheme.HTTP;
        }
        this.preserveHostHeader = preserveHost;
        this.hostHdr = hostHdr;
        
        if (hostHdr != null && ! "".equals(hostHdr)) {
            this.preserveHostHeader = false;
        }
        this.policyServiceGateway = policyServiceGateway;
        this.sourceName = sourceName;
        this.policyPath = policyPath;
        updateId();
    }

	/**
	 * Constructor that allows us to override default x-forwarded-scheme
	 * header behavior.
	 * 
     * @param scheme
	 * @param host
	 * @param cctx
	 * @param thost
	 * @param tport
     * @param tscheme
     * @param outgoingTlsPort
	 * @param preserve
	 * @param hostHdr
	 * @param policyServiceGateway
	 * @param injectScheme
	 * @param schemeHeader
	 */
    public AppEndPoint(InboundScheme scheme, String host, String cctx, String thost, int tport, OutboundScheme tscheme,
    		 int outgoingTlsPort, boolean preserve, String hostHdr, String policyServiceGateway, boolean injectScheme,
			String schemeHeader, String sourceName, String policyPath) {
    	
    	this(scheme, host, cctx, null, thost, tport, tscheme, outgoingTlsPort, preserve, hostHdr, policyServiceGateway, 
        		injectScheme, schemeHeader, sourceName, policyPath);
    }
    
    public AppEndPoint(InboundScheme scheme, String host, String cctx, String queryString, String thost, int tport, 
    		OutboundScheme tscheme, int outgoingTlsPort, boolean preserve, String hostHdr, String policyServiceGateway, 
    		boolean injectScheme, String schemeHeader, String sourceName, String policyPath) {
    	
    	this(scheme, host, cctx, queryString, thost, tport, tscheme, outgoingTlsPort, preserve, hostHdr, policyServiceGateway, sourceName, policyPath);
    	this.injectSchemeHeader = injectScheme;
    	if (schemeHeader != null) {
        	this.schemeHeaderName = schemeHeader;
    	}
	}

	private void updateId() {
        this.id = this.canonicalHost + this.contextRoot + "->URI=" + host + ":" + endpointPort;
	}
    
	/**
	 * Answers true if SSL should be used when connecting to the back-end
	 * application represented by this end point.
	 * 
	 * @param incoming
	 * @return
	 */
    public boolean useHttpsScheme(Scheme incoming) {
        return this.appScheme == OutboundScheme.HTTPS || (this.appScheme == OutboundScheme.SAME && incoming == Scheme.HTTPS);  
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
	
	public String getContextRoot() {
		return contextRoot;
	}

	public void setContextRoot(String contextRoot) {
		this.contextRoot = contextRoot;
	}
	
	public String getQueryString() {
		return queryString;
	}
	
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	/**
	 * Returns the port to which to connect for the back-end application 
	 * represented by this end point which may be the tSslPort or the port
	 * depending on if it should be over SSL or not.
	 *  
	 * @param incomingScheme
	 * @return
	 */
	public int getEndpointPort(Scheme incomingScheme) {
		if (this.appScheme == OutboundScheme.SAME && incomingScheme == Scheme.HTTPS) {
			return endpointTlsPort;
		}
		return endpointPort;
	}

	/**
	 * Returns the endpointPort value which corresponds to tport on the corresponding
	 * cctx-mapping element which allows for late 'console-port' alias resolution.
	 */
	public int getEndpointHttpPort() {
		return this.endpointPort;
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
		if (contextRoot == null) { // no translation available
			return reqPkg.requestLine;
		}
		String uri = reqPkg.requestLine.getUri();
		if (!UriMatcher.matches(uri, contextRoot)) {
			int queryIndex = uri.indexOf("?");
			if (!UriMatcher.matches(uri.substring(queryIndex), queryString)) {
				return null;
			}
		}

        StartLine appReqLn = new StartLine(reqPkg.requestLine.getMethod(), reqPkg.requestLine.getUri(),
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
	
	public void setPurgedHeaders(List<String> purgedHdrs) {
		this.purgedHeaders = purgedHdrs;
	}

    /**
     * Injects the policy-service-url pointing to the one for the by-site element
     * that contained our context mapping end point possibly with adjusted
     * gateway host and port if specified like when server is behind a firewall
     * and can't get to rest service without a reverse proxy tunnel
     * @param cfg
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
	 * @param incomingScheme
	 */
	public void injectHeaders(Config cfg, HttpPackage reqPkg, User user, Scheme incomingScheme) {
		injectProfileHeaders(user, reqPkg);
		adjustHostHdr(reqPkg);
		injectSchemeHeader(incomingScheme, reqPkg);
		injectPolicyServiceUrlHdr(cfg, reqPkg);
		injectFixedHdrs(reqPkg);
		injectCctxFixedHdrs(reqPkg);
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
				String[] vals = user.getAttribute(attName);
				for(String val : vals) {
					try {
	                    val = MimeUtility.encodeText(val, "utf-8", null);
	                } catch (UnsupportedEncodingException e) {
	                    cLog.warning("Unsupported Encoding specified for header '"
	                            + attName + "'. Leaving as unencoded.");
	                }
					reqPkg.headerBfr.append(new Header(hdrName, val));
				}
			}
		}
	}

	/**
	 * Injects any fixed headers declared in config file.
	 * 
	 * @param reqPkg
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
	 * Purges any headers declared in config file with the purge-header
	 * directive.
	 * 
	 * @param reqPkg
	 */
	public void stripPurgedHeaders(HttpPackage reqPkg) {
		if (purgedHeaders == null) {
			return;
		}
		for(String hdr : purgedHeaders) {
			reqPkg.headerBfr.removeHeader(hdr);
		}
	}

	/**
	 * Injects cctx header.
	 * 
	 * @param reqPkg
	 */
	private void injectCctxFixedHdrs(HttpPackage reqPkg) {
		if (cctxHeader != null) {
			reqPkg.headerBfr.removeHeader(CCTX_HEADER);
			reqPkg.headerBfr.append(cctxHeader);
		}
	}

	/**
	 * Optionally injects a header to indicate by which scheme a request was 
	 * received.
	 * 
	 * @param incomingScheme
	 * @param reqPkg
	 */
	private void injectSchemeHeader(Scheme incomingScheme, HttpPackage reqPkg) {
        // first remove any injected header
        reqPkg.headerBfr.removeHeader(schemeHeaderName);
        
        // indicate via headers over which scheme the client request was received 
        if (injectSchemeHeader) {
        	reqPkg.headerBfr.append(new Header(schemeHeaderName, incomingScheme.moniker));
        	reqPkg.scheme = incomingScheme;
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
            String hostHdr = (getHostHeader() != null ?
                    getHostHeader() :
                        (getHost()
                                + (getEndpointPort(reqPkg.scheme) != 80
                                        ? (":" + getEndpointPort(reqPkg.scheme))
                                                : "")));
            if (hhdr != null && ! hhdr.getValue().equals(hostHdr)) {
                reqPkg.headerBfr.set(new Header("X-Forwarded-Host", hhdr.getValue()));
            }
            reqPkg.headerBfr.set(new Header(HeaderDef.Host, hostHdr));
        }
	}
    
    public String getOriginalName() {
        return "[cctx-mapping] [policy-source=" + sourceName + "] --> " + policyPath;
    }
}
