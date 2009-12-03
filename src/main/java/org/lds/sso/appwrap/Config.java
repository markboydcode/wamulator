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

	private int maxRepeatReqCount = 4;

	protected Thread sweeper;

	private long repeatRecordSleepPeriod = 30000;
	
	protected final Map<AllowedUri, RepeatRecord> requestOccurrence = new HashMap<AllowedUri, RepeatRecord>();

	/**
	 * Determines if forward proxying of non-sso traffic is allowed. Defaults to false.
	 */
	private boolean allow_forward_proxying = false;


	public static final String SERVER_NAME = "App Wrap Reverse Proxy";

	public static final String CANONICAL_CTX_QPARAM_NAME = "cctx";

	/**
	 * DISCARDS existing Config instance available from  {@link Config#instance}
	 * and creates a new clean unititialized one.
	 */
	public Config() {
		loadSessionManager();
		startRepeatRequestRecordSweeper();

		if (instance != null) {
			instance.stopRepeatRequestRecordSweeper();
		}
		instance = this;
	}
	
	/**
	 * Returns the current instance of {@link Config} creating a new one if one
	 * has not yet been instantiated. Each time the {@link Config}'s constructor
	 * is called that new instance will be the one returned from this method and
	 * the old one is discarded.
	 * 
	 * @return
	 */
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
	 * Set the minimum milliseconds that must elapse before a request with an
	 * identical path can be received without percieving the two requests as 
	 * being part of an infinite repeat loop.
	 * 
	 * @param millis
	 */
	public void setMinReqOccRepeatMillis(long millis) {
		minReqOccRepeatMillis = millis;
	}

	/**
	 * Sets the maximum number of requests having the same path with each being
	 * received within {@ling #minReqOccRepeatMillis} of the last before it is
	 * deemed an infinite request loop and and error message page is served.
	 * 
	 * @return
	 */
	public void setMaxRepeatCount(int max) {
		maxRepeatReqCount = max;
	}

	public int getMaxRepeatCount() {
		return maxRepeatReqCount;
	}
	
	

	/**
	 * Sweeper for removing repeat requests from the request occurrence cache.
	 * 
	 * @author Mark Boyd
	 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
	 *
	 */
	public class RepeatRecordsSweeper implements Runnable {

		public void run() {
			boolean interrupted = false;
			while (! interrupted) {
				Map<RepeatRecord, AllowedUri> records = new HashMap<RepeatRecord, AllowedUri>(requestOccurrence.size());
				for( Map.Entry<AllowedUri, RepeatRecord> ent : requestOccurrence.entrySet() ) {
					records.put(ent.getValue(), ent.getKey());
				}
				for( Map.Entry<RepeatRecord, AllowedUri> ent : records.entrySet() ) {
					RepeatRecord record = ent.getKey();
					long elapsedSinceLast = System.currentTimeMillis() - record.millisOfLastCall;
					
					if (elapsedSinceLast > minReqOccRepeatMillis) {
						requestOccurrence.remove(ent.getValue());
					}
				}
				try {
					Thread.sleep(repeatRecordSleepPeriod);
				}
				catch (InterruptedException e) {
					interrupted = true;
				}
			}
			sweeper = null;
		}
	}

	/**
	 * Object for keeping track of how many repeats of a specific url have been 
	 * seen in rapid repeat to detect infinite redirect loops.
	 * 
	 * @author Mark Boyd
	 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
	 *
	 */
	public static class RepeatRecord {
		public Long millisOfLastCall = -1L;
		public int repeatCount = 0;
	}

	public Object getRequestOccurrenceCache() {
		// TODO Auto-generated method stub
		return null;
	}

	public RepeatRecord getRepeatRequestRecord(AllowedUri uri) {
		return requestOccurrence.get(uri);
	}

	public void addRepeatRequestRecord(AllowedUri uri, RepeatRecord record) {
		requestOccurrence.put(uri, record);
	}

	/**
	 * Starts the repeat request record sweeper allowing overriding if needed.
	 */
	protected void startRepeatRequestRecordSweeper() {
		sweeper = new Thread(new RepeatRecordsSweeper());
		sweeper.setDaemon(true);
		sweeper.setName("RepeatRequestSweeper");
		sweeper.start();
	}
	
	public void stopRepeatRequestRecordSweeper() {
		if (sweeper != null) {
			sweeper.interrupt();
		}
	}

	public long getRepeatRecordSleepPeriod() {
		return repeatRecordSleepPeriod;
	}

	public void setRepeatRecordSweeperSleepPeriod(long repeatRecordSleepPeriod) {
		this.repeatRecordSleepPeriod = repeatRecordSleepPeriod;
	}

	/**
	 * Determines if we allow requests not mapped to sso traffic to pass on to
	 * their targeted url.
	 * 
	 * @param parseBoolean
	 */
	public void setAllowForwardProxying(boolean bool) {
		this.allow_forward_proxying = bool;	
	}
	
	public boolean getAllowForwardProxying() {
		return this.allow_forward_proxying;	
	}
}
