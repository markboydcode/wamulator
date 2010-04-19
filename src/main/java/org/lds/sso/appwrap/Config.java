package org.lds.sso.appwrap;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.proxy.RequestHandler;
import org.lds.sso.appwrap.rest.RestVersion;

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
         * The default number of traffic recording entries that will be kept in memory.
         * This default can be overriden in the configuration file by setting the
         * max-entries attribute of the console-recording element as follows:
         * <code><console-recording sso="true" rest="true" max-entries="1000"/></code>
         */
        public static final int MAX_TRAFFIC_ENTRIES = 1000;

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

	private String stateCookieName;

	private String externalUserSource = null;

        private int maxEntries = MAX_TRAFFIC_ENTRIES;

        private boolean debugLoggingEnabled = false;

		private RestVersion restVersion;

	private static final String SERVER_NAME = determineCurrentVersion();

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
	
	private static String determineCurrentVersion() {
		String version = "current SSO Simulator in IDE";
		/* first see if we can get it from the package structure. The maven 
		 * build process automatically creates a suitable manifest file the 
		 * exposes these values. But if run in and IDE the manifest file isn't
		 * available and the implementation values will be null.
		 */
		Package pkg = Config.class.getPackage();
		if (pkg != null && pkg.getImplementationTitle() != null) {
			version = pkg.getImplementationTitle() + " "
			+ pkg.getImplementationVersion(); 
			if (cLog.isDebugEnabled()) {
				cLog.debug("Server Name set to: " + version);
			}
		}
		else {
			if (cLog.isDebugEnabled()) {
				cLog.debug("Server Name set to default value: " + version);
			}
		}
		/* when running in an IDE the version number is less important. we 
		 * could require inclusion of the project's root directory and pull it
		 * from the pom.xml file but I'll just put in a dummy version for now.
		 */
		return version;
	}

	/**
	 * Returns the server name which includes the version.
	 * 
	 * @return
	 */
	public static String serverName() {
		return Config.SERVER_NAME;
	}
	
	/**
	 * Expose server name via instance method so accessible to JSP pages.
	 * 
	 * @return
	 */
	public String getServerName() {
		return Config.serverName();
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
     * @return the getMaxEntries
     */
    public int getMaxEntries() {
        return maxEntries;
    }

    /**
     * @param getMaxEntries the getMaxEntries to set
     */
    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /**
     * @return the debugLoggingEnabled
     */
    public boolean isDebugLoggingEnabled() {
        return debugLoggingEnabled;
    }

    /**
     * @param debugLoggingEnabled the debugLoggingEnabled to set
     */
    public void setDebugLoggingEnabled(boolean debugLoggingEnabled) {
        this.debugLoggingEnabled = debugLoggingEnabled;
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
				int initialSize = requestOccurrence.size();
				if (initialSize < 0) {
					initialSize = 5;
					if (cLog.isDebugEnabled()) {
						cLog.debug("RequestOccurrences size was -1 which shouldn't be possible from map.size(). Setting to 5.");
					}
				}
				Map<RepeatRecord, AllowedUri> records = new HashMap<RepeatRecord, AllowedUri>(initialSize);
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

	/**
	 * Sets the source identifying portion of the shim's state cookie name.
	 * @param id
	 */
	public void setShimStateCookieId(String id) {
		this.stateCookieName = "saw" + id;
	}
	
	/**
	 * Returns the name of the cookie used for persisting current configuration
	 * of the shim. The name incorporates the source of configuration so that
	 * different running instances of the shim can behave differently so long 
	 * as they were started with different configuration sources. This isn't 
	 * perfect but should solve most issues when/if we decide to add support for
	 * such a cookie.
	 * @return
	 */
	public Object getShimStateCookieName() {
		return this.stateCookieName;
	}

	/**
	 * Sets the external site from whence user information can be loaded for 
	 * injection in headers. Can include a macro of "{username}" which will be
	 * replaced with the value entered in the codaUserSelect.jsp page. 
	 * 
	 * @param source
	 */
	public void setExternalUserSource(String source) {
		this.externalUserSource = source;
	}
	
	public String getExternalUserSource() {
		return this.externalUserSource;
	}

	/**
	 * Sets the version of the rest interface that should be exposed in the 
	 * console for applications to resolve authorization questions.
	 * 
	 * @param restVersion
	 */
	public void setRestVersion(RestVersion version) {
		this.restVersion = version;
	}
	
	/**
	 * Returns the rest version exposed for applications.
	 * @return
	 */
	public RestVersion getRestVersion() {
		return this.restVersion;
	}
}
