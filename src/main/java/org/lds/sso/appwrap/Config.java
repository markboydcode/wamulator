package org.lds.sso.appwrap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.proxy.RequestHandler;

public class Config {
	private static final Logger cLog = Logger.getLogger(Config.class);

	/**
	 * The default port on which appwrap will listen for requests to SSO
	 * protected applications sitting behind appwrap.
	 */
	public static final int DEFAULT_APP_PORT = 8080;

	/**
	 * The default port on which appwrap will listen for admin UI and sso policy
	 * server rest requests.
	 */
	public static final int DEFAULT_ADMIN_PORT = 8081;

	/**
	 * Allows some classes to get at the config instance without any means of
	 * passing the instance in.
	 */
	private static Config instance;

	/**
	 * The port on which appwrap will listen for requests to SSO protected
	 * applications sitting behind appwrap.
	 */
	private int httpAppPort = DEFAULT_APP_PORT;

	private int httpAdminPort = DEFAULT_ADMIN_PORT;

	private UserManager userMgr = new UserManager();

	private SessionManager sessionMgr = null;

	/**
	 * Global headers (if any) injected for every request.
	 */
	private Map<String, String> headers = new TreeMap<String, String>();

	/**
	 * The domain of the cookie set by app-wrap
	 */
	private String cookieDomain = ".lds.org";

	private TrafficManager appMgr = new TrafficManager();

	private TrafficRecorder trafficRcrdr = new TrafficRecorder();

	/**
	 * The name of the cookie set by app wrap. Defaults to "app-wrap-token".
	 */
	private String cookieName = "app-wrap-token";

	private String loginPageUrl = null;

	private long minReqOccRepeatMillis = 200;

	public static final String SERVER_NAME = "App Wrap Reverse Proxy";

	public static final String CANONICAL_CTX_QPARAM_NAME = "cctx";

	/**
	 * Discards existing Config instance and creates a new one to be accessible
	 * from {@link Config#instance}.
	 */
	public Config() {
		loadSessionManager();
		instance = this;
	}

	public static Config getInstance() {
		if (instance == null) {
			new Config();
		}
		return instance;
	}
	
	/**
	 * Loads the SessionManager allowing overriding if needed by subclasses.
	 */
	protected void loadSessionManager() {
		sessionMgr = new SessionManager();
	}

	/**
	 * Returns the name of the cookie used by app wrap.
	 * @return
	 */
	public String getCookieName() {
		return cookieName;
	}

	/**
	 * Gets an object for managing users configured to allow access to protected
	 * applications.
	 * 
	 * @return
	 */
	public UserManager getUserManager() {
		return userMgr;
	}

	public SessionManager getSessionManager() {
		return sessionMgr;
	}

	public TrafficRecorder getTrafficRecorder() {
		return trafficRcrdr;
	}

	/**
	 * Returns the token passed in the value of the cookie if any or null if no
	 * such cookie is included or the cookie header is null.
	 * 
	 * @param cookieHdr
	 * @return
	 */
	public String getTokenFromCookie(String cookieHdr) {
		if (cookieHdr == null) {
			return null;
		}
		String cookieName = getCookieName() + "=";

		int cIdx = cookieHdr.indexOf(cookieName);

		if (cIdx == -1) {
			return null;
		}
		// header looks like:
		// Cookie: JSESSIONID=FA5497D112C24381F06293E48151E663; amlbcookie=01;
		// AMAuthCookie="AQIC5wM2LY4SfcynYE2P7XXg5qbAVZMlDWSJnLS+8Cx/TXI=@AAJTSQACMDE=#"
		String value = null;

		int semiColonIdx = cookieHdr.indexOf(";", cIdx);
		if (semiColonIdx == -1) { // last cookie in list
			value = cookieHdr.substring(cIdx + cookieName.length());
		}
		else {
			value = cookieHdr.substring(cIdx + cookieName.length(), semiColonIdx);
		}
		return value;
	}

	/**
	 * Gets the user for a given token.
	 * 
	 * @param token
	 * @return
	 */
	public String getUsernameFromToken(String token) {
		if (token == null) {
			return null;
		}
		int dashIdx = token.lastIndexOf('-');
		if (dashIdx == -1) {
			// invalid token format
			return null;
		}
		return token.substring(0, dashIdx);
	}

	public int getProxyPort() {
		return this.httpAppPort;
	}

	public void setProxyPort(int port) {
		this.httpAppPort = port;
	}

	public int getConsolePort() {
		return this.httpAdminPort;
	}

	public void setConsolePort(int port) {
		this.httpAdminPort = port;
	}

	public String getCookieDomain() {
		return cookieDomain;
	}

	public void setCookieDomain(String cookieDomain) {
		this.cookieDomain = cookieDomain;
	}

	public TrafficManager getTrafficManager() {
		return appMgr;
	}

	/**
	 * Adds a global header to be injected into proxy requests targeted at
	 * registered application sites.
	 * 
	 * @param name
	 * @param value
	 */
	public void addGlobalHeader(String name, String value) {
		this.headers.put(name, value);
	}

	/**
	 * Injects any global headers into the headers buffer prior to sending the
	 * request on to the targeted site.
	 * 
	 * @param headersBfr
	 */
	public void injectGlobalHeaders(StringBuffer headersBfr) {
		for (Entry<String, String> e : headers.entrySet()) {
			headersBfr.append(e.getKey()).append(": ").append(e.getValue()).append(RequestHandler.CRLF);
		}
	}

	/**
	 * Returns an array of name/value pairs representing the set of global
	 * headers injected into requests targeting registered sites.
	 * 
	 * @return
	 */
	public NvPair[] getGlobalHeaders() {
		NvPair[] hdr = null;

		if (headers.size() > 0) {
			hdr = new NvPair[headers.size()];
			int idx = 0;
			for (Entry<String, String> e : headers.entrySet()) {
				hdr[idx++] = new NvPair(e.getKey(), e.getValue());
			}

		}
		return hdr;
	}

	public void setCookieName(String cookieName) {
		this.cookieName = cookieName;
	}

	public void setSignInPage(String absoluteLoginUrl) {
		this.loginPageUrl = absoluteLoginUrl;
	}

	public String getLoginPage() {
		return this.loginPageUrl;
	}

	public long getMinimumRepeatMillis() {
		return minReqOccRepeatMillis;
	}

	/**
	 * Set the minimum milliseconds that must elapse before an idnentical request
	 * will be allowed through the proxy to help identify and stop infinite 
	 * redirect loops.
	 * 
	 * @param millis
	 */
	public void setMinReqOccRepeatMillis(long millis) {
		minReqOccRepeatMillis = millis;
	}
}
