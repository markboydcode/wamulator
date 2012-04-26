package org.lds.sso.appwrap.proxy;

import java.io.ByteArrayOutputStream;

import org.lds.sso.appwrap.AppEndPoint.Scheme;
import org.lds.sso.appwrap.SiteMatcher;
import org.lds.sso.appwrap.proxy.header.HeaderBuffer;

public class HttpPackage {
    // some non-standard header names
    public static final String CONN_ID_NM = "X-ConnId";
    public static final String CONN_ID_HDR = CONN_ID_NM + ": ";
    
    public static final String SHIM_HANDLED_HDR_NM = "X-Wmltr";
    public static final String SHIM_HANDLED_HDR = SHIM_HANDLED_HDR_NM + ":";
    
    public static final String SHIM_STRIPPED_HEADERS = "X-Stripped-Empty-Headers";
    
    // Erroneous proxy-connection header not part of rfc2616 but used by netscape
    // originally and copied by microsoft and mozilla and httpclient, etc.
    public static final String PROXY_CONNECTION_HDR_NM = "Proxy-Connection";
    public static final String PROXY_CONNECTION_HDR = PROXY_CONNECTION_HDR_NM + ":";

    public static final int HTTP_1_0 = 0;
    public static final int HTTP_1_1 = 1;
    
	public HeaderBuffer headerBfr = new HeaderBuffer();
	public ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
	public int responseCode = 200;
	public int contentLength = -1; // minus one conveys content-length hdr not read yet or not found
	public ResponseLine responseLine = null;
	public RequestLine requestLine = null;
	public boolean isResponse = false;
	public HttpPackageType type = null;
	public String cookiesHdr = null;
	public String hostHdr = null;
	public int port = 80;
	public boolean hasNonDefaultPort = false;
	public String host = null;
	public TrafficType trafficType = TrafficType.UNCLASSIFIED;
	public SiteMatcher site = null;
	public boolean redirectLoopDetected = false;
	public boolean rapidRepeatRequestDetected = false;
	public String repeatRequestErrMsg = null;
	public String path = null;
	public String query = null;
	public Scheme scheme = null;
    public boolean signMeInDetected = false;
    public boolean signMeOutDetected = false;
    /**
     * Indicates if a socket timeout occurred while reading the input stream
     * for this package.
     */
    public boolean socketTimeout = false;
    /**
     * Indicates if we found the cookie that matched the simulator session cookie.
     */
    public boolean cookieFound = false;
    /**
     * Holds start line before stripping signin/out query param if included.
     */
    public RequestLine origRequestList;
    
    /**
     * Used to convey the token for cdsso from within the response generation
     * method to its calling code.
     */
    public String tokenForCdsso;
    
    /**
     * Indicates the version of http for a given package. 0 means http 1.0 while
     * 1 means http 1.1. Defaults to 1.
     */
    public int httpVer = 1;
    
    /**
     * Indicates connection over which the http message in this package was 
     * received should be considered persistent or not. Http 1.1 defaults to 
     * true. Http 1.0 defaults to false. See sections 14.10 and 19.6.2 of 
     * RFC-2616.
     */
    public boolean isPersistent = true;
}
