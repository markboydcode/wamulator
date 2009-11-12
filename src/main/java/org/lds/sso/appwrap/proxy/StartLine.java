package org.lds.sso.appwrap.proxy;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Object modeling an http request and response lines.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class StartLine implements RequestLine, ResponseLine{
	public String token1 = null;
	public String token2 = null;
	public String token3 = null;
	public String proxy_scheme = null;
	public String proxy_host = null;
	public int proxy_port = -1;
	public boolean isProxied = false;
	
	/**
	 * Constructor that parses the start line into its three tokens.
	 * 
	 * @param line
	 * @throws MalformedURLException 
	 */
	public StartLine(String line) throws MalformedURLException {
		String cleanLn = line.trim();
		int sIdx = cleanLn.indexOf(RequestHandler.SP);
		int eIdx = cleanLn.indexOf(RequestHandler.SP, sIdx+1);
		token1 = cleanLn.substring(0, sIdx);
		token2 = cleanLn.substring(sIdx + 1, eIdx);
		token3 = cleanLn.substring(eIdx + 1);
		if (token2.startsWith("http://") || token2.startsWith("https://")) {
			URL url = new URL(token2);
			isProxied = true;
			proxy_scheme = url.getProtocol();
			proxy_host = url.getHost();
			proxy_port = (url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
			token2 = url.getPath() + (url.getQuery() != null ?
					("?" + url.getQuery()) : "") ;
		}
	}
	
	public StartLine(String tk1, String tk2, String tk3) {
		this.token1 = tk1;
		this.token2 = tk2;
		this.token3 = tk3;
	}
	
	public String toString() {
		return token1 + RequestHandler.SP + token2 + RequestHandler.SP + token3;
	}

	// RequestLine methods
	
	public String getMethod() {
		return token1;
	}

	public String getUri() {
		return token2;
	}

	public String getHttpDecl() {
		return token3;
	}

	// ResponseLine methods
	
	public String getHttpVer() {
		return token1;
	}

	public String getMsg() {
		return token3;
	}

	public String getRespCode() {
		return token2;
	}
}
