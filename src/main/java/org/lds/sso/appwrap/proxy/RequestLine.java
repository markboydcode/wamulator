package org.lds.sso.appwrap.proxy;

/**
 * Represents questions that can be asked of the http package's start line 
 * when that package is and http request.
 * 
 * @author BoydMR
 *
 */
public interface RequestLine {
    /**
     * Returns the http method of the request which is the first token of the
     * start line.
     * @return
     */
	public String getMethod();
	/**
	 * Returns middle token of the request's start line which may be 
	 * a URI relative to a Host header or a full URL in the case of a call 
	 * knowningly made by a browser to a proxy.
	 * 
	 * @return
	 */
	public String getUri();
	/**
	 * Returns the last token of the request's start line which is the http
	 * version declaration like http/1.1 or http/1.0.
	 * 
	 * @return
	 */
	public String getHttpDecl();
	/**
	 * For a request with an absolute Request-URI returns the scheme of that
	 * request; otherwise returns null.
	 *  
	 * @return
	 */
    public String getAbsReqUri_scheme();
    /**
     * For a request with an absolute Request-URI returns the host of that
     * request; otherwise returns null.
     *  
     * @return
     */
    public String getAbsReqUri_host();
    /**
     * For a request with an absolute Request-URI returns the port of that
     * request; otherwise returns -1.
     *  
     * @return
     */
    public int getAbsReqUri_port();

    /**
     * Returns true if the Request-URI is fully qualified and does not specify
     * a port but leaves the port to be the default for the scheme; null otherwise.
     * 
     * @return
     */
    public boolean getAbsReqUriUsesDefaultPort();

    /**
     * Always returns the Request-URI's path regardless of whether the 
     * Request-URI were absolute or relative.
     * 
     * @return
     */
    public String getReqPath();
    /**
     * Always returns the Request-URI's query regardless of whether the 
     * Request-URI were absolute or relative which may be null if no query was
     * included.
     * 
     * @return
     */
    public String getReqQuery();
    /**
     * Always returns the Request-URI's fragment regardless of whether the 
     * Request-URI were absolute or relative which may be null if no fragment was
     * included.
     * 
     * @return
     */
    public String getReqFragment();
    /**
     * Returns the request's version of http to which the package conforms. 
     * This is just the "x.y" portion of the start line's last token which has
     * the form "http/x.y".
     */
    public String getHttpVer();
}
