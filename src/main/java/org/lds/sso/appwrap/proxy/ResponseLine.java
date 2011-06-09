package org.lds.sso.appwrap.proxy;

/**
 * Represents questions that can be asked of the http package's start line 
 * when that package is and http response.
 * 
 * @author BoydMR
 *
 */
public interface ResponseLine {
    /**
     * Returns the response's version of http to which the package conforms. 
     * This is just the "x.y" portion of the start line's first token which has
     * the form "http/x.y".
     */
	public String getHttpVer();
	/**
	 * Returns the response's response code which is the middle token of the
	 * start line.
	 * @return
	 */
	public String getRespCode();
	/**
	 * Returns the response's message which is any content beyond the middle 
	 * token of the start line.
	 * @return
	 */
	public String getMsg();
	   /**
     * Returns the first token of the response's start line which is the http
     * version declaration like http/1.1 or http/1.0.
     * 
     * @return
     */
    public String getHttpDecl();

}
