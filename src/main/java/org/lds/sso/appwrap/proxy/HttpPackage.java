package org.lds.sso.appwrap.proxy;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.TreeMap;

import org.lds.sso.appwrap.SiteMatcher;

public class HttpPackage {
	public static final String COOKIE_HDR = "cookie:";
	public static final String HOST_HDR = "host:";
	public static final String CONTENT_LNG = "content-length:";
	public static final String CONTENT_TYPE = "Content-Type:";
    public static final String CONN_ID = "X-connId";
    public static final String CONN_ID_HDR = CONN_ID + ": ";
    public static final String SHIM_HANDLED = "X-shim";
    public static final String SHIM_HANDLED_HDR = SHIM_HANDLED + ":";
    public static final String SHIM_STRIPPED_HEADERS = "X-stripped-empty-headers";
	public static final String LOCATION_HDR = "location:";
	public static final String SET_COOKIE_HDR = "set-cookie:"; // need set-cookie2?
	
	public HeaderBuffer headerBfr = new HeaderBuffer();
	public ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
	public int responseCode = 200;
	public int contentLength = 0;
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
	public String scheme = null;
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
}
