package org.lds.sso.appwrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.lds.sso.appwrap.conditions.evaluator.GlobalHeaderNames;
import org.lds.sso.appwrap.conditions.evaluator.LogicalSyntaxEvaluationEngine;
import org.lds.sso.appwrap.identity.ExternalUserSource;
import org.lds.sso.appwrap.identity.Session;
import org.lds.sso.appwrap.identity.SessionManager;
import org.lds.sso.appwrap.identity.UserManager;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.proxy.header.Header;
import org.lds.sso.appwrap.proxy.header.HeaderBuffer;
import org.lds.sso.appwrap.rest.RestVersion;

public class Config {
	private static final Logger cLog = Logger.getLogger(Config.class.getName());

    /**
     * The location of log files relative to the current directory. If not found
     * it will be created.
     */
    public static final String LOG_FILES_LOCATION = "./logs/";

    /**
     * The prefix attached to a connection identifier to create the name of the
     * log file to contain the conversation details for that connection.
     */
    public static final String LOG_FILES_PREFIX = "C-";

    /**
     * The suffix or extension attached to a connection identifier to create the
     * name of the log file to contain the conversation details for that
     * connection.
     */
    public static final String LOG_FILES_SUFFIX = ".log";

    /**
     * The default port on which appwrap will listen for requests to SSO
     * protected applications sitting behind appwrap.
     */
    public static final int DEFAULT_APP_PORT = 8080;

    /**
     * The default port on which appwrap will listen for requests to SSO
     * protected applications sitting behind appwrap.
     */
    public static final int DEFAULT_APP_HTTPS_PORT = 8443;

    /**
     * The default host used in the certificate generated to identify the
     * site if using HTTP over TLS.
     */
    public static final String DEFAULT_APP_HTTPS_CERT_HOST = "*.lds.org";

    /**
     * The OU value for the DN of the CA certificate and site generated 
     * certificates. Doesn't have to be a constant but ties all cert related
     * stuff together nicely.
     */
    public static final String CERT_ORG_UNIT = "WAMulator-dev";

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
     * An implicit alias made available to the configuration when auto-binding
     * is being used for the console port.
     */
    public static final String CONSOLE_PORT_ALIAS = "console-port";
    public static final String PROXY_PORT_ALIAS = "proxy-port";
    public static final String CONSOLE_PORT_MACRO = "{{" + CONSOLE_PORT_ALIAS + "}}";
    public static final String PROXY_PORT_MACRO = "{{" + PROXY_PORT_ALIAS + "}}";

    /**
	 * Allows some classes to get at the config instance without any means of
	 * passing the instance in.
	 */
	private static Config instance;

    /**
     * The port on which the simulator will listen for requests to SSO protected
     * applications sitting behind it.
     */
    private int proxyPort = DEFAULT_APP_PORT;

    /**
     * The port on which the simulator will listen for HTTP over TLS requests to 
     * SSO protected applications sitting behind it.
     */
    private int proxyHttpsPort = DEFAULT_APP_HTTPS_PORT;

    /**
     * The host name potentially including an asterisk as per RFC 2818 section
     * 3.1 which will be set as the CN of the certificate generated to 
     * identify the site. Examples are, "localhost.lds.org" and "*.lds.org".
     */
    private String proxyHttpsCertHost = "*.lds.org";

    /**
     * Indicates if TLS/SSL proxy should be started. Defaults to off.
     */
    private boolean proxyHttpsEnabled = false;
    
	/**
	 * The port on which the simulator provides its console pages and its
	 * rest interface endpoints.
	 */
	private int consolePort = DEFAULT_ADMIN_PORT;

	private UserManager userMgr = new UserManager();

	private SessionManager sessionMgr = null;

	/**
	 * Global headers (if any) injected for every request.
	 */
	private Map<String, String> headers = new TreeMap<String, String>();

	// define signmein/signmeout headers
	{
        headers.put(GlobalHeaderNames.SIGNIN, GlobalHeaderNames.SIGNIN_VALUE);
        headers.put(GlobalHeaderNames.SIGNOUT, GlobalHeaderNames.SIGNOUT_VALUE);
	}
	/**
	 * The default domain of the cookie set by the simulator
	 */
	public static final String DEFAULT_COOKIE_DOMAIN = ".lds.org";
	
	// set up custom syntax map and engine
	private LogicalSyntaxEvaluationEngine sEngine = new LogicalSyntaxEvaluationEngine();
    private Map<String, String> syntaxMap = new HashMap<String, String>();  

	private TrafficManager appMgr = new TrafficManager(sEngine, syntaxMap);

	private EntitlementsManager entitlementsMgr = new EntitlementsManager(sEngine);
	
	private TrafficRecorder trafficRcrdr = new TrafficRecorder();

	/**
	 * The name of the cookie set by the simulator. Defaults to "simulator-token".
	 */
	private String cookieName = "simulator-token";

	/**
	 * The URL of the sign-in page to which the user should be redirected to
	 * authenticate.
	 */
	private String loginPageUrl = null;

    /**
     * The default signin page location. If that JSP ever changes this will need
     * to be changed. Includes a macro of "{{HOST}}" that will be replaced with
     * the host of the first by-site element declared in configuration. If no
     * by-site element is declared then the sign-in page will not be defaulted.
     * 
     */
    private static final String DEFAULT_SIGNIN_PAGE_TMPLT = "http://{{HOST}}:"
            + CONSOLE_PORT_MACRO + "/admin/selectUser.jsp";
	
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

	private List<ExternalUserSource> externalUserSources = new ArrayList<ExternalUserSource>();

    private int maxEntries = MAX_TRAFFIC_ENTRIES;

    private boolean debugLoggingEnabled = false;

	private RestVersion restVersion = RestVersion.CD_OESv1;


    /**
     * If true indicates that auto-binding has been specified and the sign-in
     * URL must be resolved replacing the console-port implicit alias reference
     * after port binding has taken place.
     */
    private boolean signinRequiresResolution = false;

    /**
     * If true indicates that the signin page url should be checked to ensure
     * that it will work with the configured cookie domain.
     */
    private boolean signinRequiresCheck = false;

    /**
     * Indicates if the proxy should strip off any empty headers if seen including 
     * any injected by the proxy. Defaults to false.
     */
    private boolean stripEmptyHeaders = false;

    /**
     * The socket timeout in milliseconds for which a read of the input stream
     * of the socket connecting to the proxy will block before throwing a
     * {@link SocketTimeoutException}. Defaults to 20000 milliseconds which is 
     * 20 seconds.
     */
    private int proxyInboundSoTimeout = 20000;

    /**
     * The socket timeout in milliseconds for which a read of the input stream
     * of the socket used by the proxy to connect to the proxied server will
     * block before throwing a {@link SocketTimeoutException}. Defaults to 20000
     * milliseconds which is 20 seconds.
     */
    private int proxyOutboundSoTimeout = 20000;

    /**
     * Indicates whether traffic is only allowed from the local host or not. Defaults
     * to true.
     */
	private boolean allowLocalTrafficOnly = true;

	/**
	 * An optional console title override. If left empty or null then the
	 * default will be used in JSPs.
	 */
	private String customConsoleTitle;

	private String loginPageAction;
    

	private static final String SERVER_NAME = determineCurrentVersion();

	public static final String CANONICAL_CTX_QPARAM_NAME = "cctx";

	/**
	 * The default mime type if none is found for a given extension in the 
	 * mime.types file.
	 */
	private static final String DEFAULT_MIME_TYPE = "application/octect-stream";

	/**
	 * The mapping from extension to content type loaded from mime.types.
	 */
	public static final Map<String, String> mimeTypes = new TreeMap<String, String>();
	
	/**
	 * DISCARDS existing Config instance available from  {@link Config#instance}
	 * and creates a new clean unititialized one.
	 */
	public Config() {
		if (mimeTypes.size() == 0) {
			loadMimeTypes();
		}
		loadSessionManager();
		startRepeatRequestRecordSweeper();
		// clear out any residue in xml loader
		XmlConfigLoader2.parsingContextAccessor.get().clear();

		if (instance != null) {
			instance.stopRepeatRequestRecordSweeper();
		}
		instance = this;
	}

	/**
	 * Loads extension to content type mappings from mime.types file. Need to 
	 * handle lines like the following:
	 * 
	 * Multiple extensions for the same type.
	 * <pre>
	 * application/octet-stream	bin dms lrf mar so dist distz pkg bpk dump elc deploy
	 * </pre>
	 *
	 * Multiple white-space characters between tokens.
	 * <pre>
	 * application/ogg					ogx 
	 * </pre>
	 *
	 * Comment lines as identified by starting with a '#' character.
	 * <pre>
	 * # application/ocsp-response 
	 * </pre>
	 */
    private void loadMimeTypes() {
    	InputStream is = this.getClass().getClassLoader().getResourceAsStream("mime.types");
    	if (is == null) {
    		cLog.log(Level.SEVERE, "mime.types file not found on classpath. File extensions won't be resolveable to content-types.");
    		return;
    	}
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String line = null;
		try {
			line = in.readLine();
		}
		catch (IOException e) {
    		cLog.log(Level.SEVERE, "Unable to read from mime.types file. File extensions won't be resolveable to content-types.", e);
            Utils.quietlyClose(is);
    		return;
		}
		while (line != null) {
			try {
				if (! line.startsWith("#")) { // skip comment lines
					String[] toks = line.split("\\s"); // split on whitespace
					if (toks.length > 1) {
    					String type = toks[0];
    					for(int i=1; i<toks.length; i++) {
    						String ext = toks[i].trim();
    						
    						if (! "".equals(ext)) {
    							mimeTypes.put(ext, type);
    						}
    					}
					}
				}
				line = in.readLine();
			}
			catch (IOException e) {
	    		cLog.log(Level.SEVERE, "Unable to finish reading from mime.types file. Some file extensions may not be resolveable to content-types.", e);
                Utils.quietlyClose(is);
	    		return;
			}
    	}
	}

	private static String determineCurrentVersion() {
		String version = "SSO Simulator in IDE";
		/* first see if we can get it from the package structure. The maven 
		 * build process automatically creates a suitable manifest file the 
		 * exposes these values. But if run in and IDE the manifest file isn't
		 * available and the implementation values will be null.
		 */
		String resource = "about.txt";
		InputStream is = Config.class.getClassLoader().getResourceAsStream("about.txt");
		if (is != null) {
		    byte[] bytes = new byte[100];
		    try {
                int read = is.read(bytes);
                String content = new String(bytes, 0, read);
                if (content.contains("project.version")) {
                    // must be running in IDE which doesn't do maven filtering :(
                    // hopefully current directory is the project root containing pom.xml
                    File file = new File("pom.xml");
                    if (file.exists()) {
                        PomVersionExtractor pve = new PomVersionExtractor();
                        FileReader fr = new FileReader(file);
                        try {
                            version = "SSO Simulator v" + pve.getVersion(fr, file.getAbsolutePath());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Utils.quietlyClose(fr);
                    }
                }
                else {
                    version = "SSO Simulator v" + content;
                }
            } catch (IOException e) {
                LogUtils.severe(cLog, "Can't find version indication", e);
            } finally {
                IOUtils.closeQuietly(is);
            }
		}
		else {
		    LogUtils.warning(cLog, "Can't find version indicating file: {0}", resource);
		}
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
        /*
         * need to be careful here. HttpClient submits two copies of the 
         * showing how careful we must be to identify the session properly.
         * For future reference:
         * 
         * Netscape format = Cookie: lds-policy=user1-13165790
         * RFC2901 format  = Cookie: $Version="1"; lds-policy="user1-13165790"; $Path="/"; $Domain=".lds.org"
         * RFC2965 format  = Cookie: $Version="1"; lds-policy="user1-13165790"; $Path="/"; $Domain=".lds.org"; $Port
         * 
        HeaderBuffer [
        Cookie: lds-policy=user1-13165790,
        Cookie: $Version="1"; lds-policy="user1-13165790"; $Path="/"; $Domain=".lds.org",
        User-Agent: Jakarta Commons-HttpClient/3.1,
        Host: local.lds.org:25584,
        Proxy-Connection: Keep-Alive,
        Connection: close,
        X-shim: handled,
        policy-signin: signmein,
        policy-signout: signmeout,
        X-connId: C-0002
        ]
         *
         * headers captured while using opensso looked like old netscape format
         * although in the AMAuthCookie case it is also wrapping the value in
         * quotes perhaps due to the non-alphanumeric characters.
        Cookie: JSESSIONID=FA5497D112C24381F06293E48151E663; amlbcookie=01;
        AMAuthCookie="AQIC5wM2LY4SfcynYE2P7XXg5qbAVZMlDWSJnLS+8Cx/TXI=@AAJTSQACMDE=#"
        */
		String cookieName = getCookieName() + "=";

		int cIdx = cookieHdr.indexOf(cookieName);
        
		if (cIdx == -1) {
			return null;
		}

		String value = null;

		int semiColonIdx = cookieHdr.indexOf(";", cIdx);
		if (semiColonIdx == -1) { 
		    // must be netscape format and last cookie in list
			value = cookieHdr.substring(cIdx + cookieName.length());
		}
		else {
		    // must be: 
	        // lds-policy="user1-13165790"; $Path...
			value = cookieHdr.substring(cIdx + cookieName.length(), semiColonIdx);
		}
		// finally see if it is a quoted string and remove the quotes
		if (value.startsWith("\"") && value.endsWith("\"")) {
            // "user1-13165790" length is 16 chars
            // 0123456789012345 indices
		    value = value.substring(1,value.length()-1);
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
		Session s = getSessionManager().getSessionForToken(token);
		if (s == null) {
			return null;
		}
		return s.getUsername();
	}

	public int getProxyPort() {
		return this.proxyPort;
	}

    public void setProxyPort(int port) {
        this.proxyPort = port;
        getTrafficManager().proxyPortChanged(port);
    }

    public void setProxyHttpsPort(int port) {
        this.proxyHttpsPort = port;
    }

    public int getProxyHttpsPort() {
        return this.proxyHttpsPort;
    }

    public void setProxyHttpsCertHost(String host) {
        this.proxyHttpsCertHost = host;
    }

    public String getProxyHttpsCertHost() {
        return this.proxyHttpsCertHost;
    }
    
    /**
     * Determines if TLS/SSL proxy should-be used. Defaults to off.
     */
    public void setProxyHttpsEnabled(boolean b) {
        this.proxyHttpsEnabled = b;
    }
    
    /**
     * Indicates if TLS/SSL proxy is/should-be in use. Defaults to off.
     */
    public boolean getProxyHttpsEnabled() {
        return this.proxyHttpsEnabled;
    }

	public int getConsolePort() {
		return this.consolePort;
	}

	public void setConsolePort(int port) {
		this.consolePort = port;
        getTrafficManager().consolePortChanged(port);
	}
	
	public void stopLogicalSyntaxEvaluationThread() {
		if(sEngine != null) {
			sEngine.stopGarbageCollecting();
			sEngine = null;
		}
	}

    public TrafficManager getTrafficManager() {
        return appMgr;
    }

    public EntitlementsManager getEntitlementsManager() {
        return entitlementsMgr;
    }

	/**
	 * Injects any global headers into the headers buffer prior to sending the
	 * request on to the targeted site.
	 * 
	 * @param headersBfr
	 */
	public void injectGlobalHeaders(HeaderBuffer headersBfr) {
		for (Entry<String, String> e : headers.entrySet()) {
			headersBfr.removeHeader(e.getKey());
		}
		for (Entry<String, String> e : headers.entrySet()) {
			
			headersBfr.append(new Header(e.getKey(), e.getValue()));
		}
	}

	public void setCookieName(String cookieName) {
		this.cookieName = cookieName;
	}

	/**
	 * Sets the action of the form in wamulator provided sign-in pages if the
	 * default needs to change.
	 * 
	 * @param action
	 */
	public void setSignInPageAction(String action) {
		this.loginPageAction = action;
	}

	/**
	 * If not null or empty string then this becomes the action of the form for
	 * the wamulator's sign-in pages.
	 * 
	 * @return
	 */
	public String getLoginAction() {
		return this.loginPageAction;
	}
	
	public void setSignInPage(String absoluteLoginUrl) {
		this.loginPageUrl = absoluteLoginUrl;
		this.signinRequiresCheck = true;
	}

	/**
	 * Returns the URL to the sign-in page resolving the console-port implicit
	 * alias if specified in configuration and providing reasonable default URL
	 * if a URL is not specified in config. Also verifies that the sign-in page
	 * URL will allow the cookie to be set for the specified domain of the cookie.
	 * @return
	 */
	public String getLoginPage() {
	    if (loginPageUrl == null) {
	        // set up default sign-in page since none specified.
            SiteMatcher site = getTrafficManager().getMasterSite();
            loginPageUrl = DEFAULT_SIGNIN_PAGE_TMPLT.replace("{{HOST}}", site.getHost());
            signinRequiresResolution = true;
	    }
	    if (signinRequiresResolution) {
	        String url = this.loginPageUrl;
            if (url.contains(CONSOLE_PORT_MACRO)) {
                url = url.replace(CONSOLE_PORT_MACRO, ("" + this.getConsolePort()));
                loginPageUrl = url;
                signinRequiresResolution = false;
            }
            if (url.contains(PROXY_PORT_MACRO)) {
                url = url.replace(PROXY_PORT_MACRO, ("" + this.getProxyPort()));
                loginPageUrl = url;
                signinRequiresResolution = false;
            }
	    }
        if (signinRequiresCheck) {
            signinRequiresCheck = false;
            String minusScheme = loginPageUrl.replaceFirst("^http://", "");
            String minusPath = minusScheme.replaceFirst("/.*$", "");
            String host = minusPath.replaceFirst(":.*$", "");
            SiteMatcher authSite = getTrafficManager().getMasterSite();
            String authCkDomain = getSessionManager().getMasterCookieDomain();
            boolean isDomainWide = authCkDomain.startsWith(".");
            
            if ((isDomainWide && ! host.endsWith(authCkDomain))
                    || (!isDomainWide && ! host.equals(authSite.getHost()))) {
                throw new IllegalArgumentException(""
                        + "Sign-in page has a host of '"
                        + host + "' but cookie domain is set to '"
                        + authCkDomain + "'. Cookie will not be "
                        + "accepted by browser.");
            }
        }
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
     * The maximum number of log file entries that should be created before
     * rolling the count back over to zero.
     * 
     * @return the getMaxEntries
     */
    public int getMaxEntries() {
        return maxEntries;
    }

    /**
     * Sets the max number of log files that will be created by the wamulator
     * before rolling back over to zero.
     * 
     * @param maxEntries the maxEntries to set
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
					LogUtils.fine(cLog, "RequestOccurrences size was -1 which shouldn't be possible from map.size(). Setting to 5.");
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
	 * @param bool
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
	 * Adds an {@link ExternalUserSource} from whence user information can be 
	 * loaded for injection in headers and evaluation in policies.  
	 * 
	 * @param source
	 */
	public void addExternalUserSource(ExternalUserSource source) {
		this.externalUserSources.add(source);
	}
	
	public List<ExternalUserSource> getExternalUserSources() {
		return this.externalUserSources;
	}

	/**
	 * Sets the version of the rest interface that should be exposed in the 
	 * console for applications to resolve authorization questions.
	 * 
	 * @param version
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

	/**
	 * Tells config that signin page resolution must be performed before returning
	 * the signin page url.
	 */
    public void setSignInRequiresResolution() {
        signinRequiresResolution = true;
    }

    /**
     * Indicates if the proxy will strip empty headers if seen
     * including any injected by the proxy itself.
     * @return
     */
    public void setStripEmptyHeaders(boolean b) {
        this.stripEmptyHeaders = b;
    }

    /**
     * Tells the proxy whether or not it should strip empty headers if seen
     * including any injected by the proxy itself.
     * @return
     */
    public boolean getStripEmptyHeaders() {
        return this.stripEmptyHeaders;
    }

    /**
     * Sets the timeout in milliseconds that the proxy will wait to receive input
     * from the connecting input stream.
     * 
     * @param millis
     * @return
     */
    public void setProxyInboundSoTimeout(int millis) {
        proxyInboundSoTimeout = millis;
    }

    /**
     * The timeout in milliseconds that the proxy will wait to receive input
     * from the connecting input stream.
     * 
     * @return
     */
    public int getProxyInboundSoTimeout() {
        return proxyInboundSoTimeout ;
    }

    /**
     * Sets the timeout in milliseconds that the proxy will wait to receive input
     * from the downstream server's input stream.
     * 
     * @param millis
     * @return
     */
    public void setProxyOutboundSoTimeout(int millis) {
        proxyOutboundSoTimeout = millis;
    }

    /**
     * The timeout in milliseconds that the proxy will wait to receive input
     * from the downstream server's input stream.
     * 
     * @return
     */
    public int getProxyOutboundSoTimeout() {
        return proxyOutboundSoTimeout ;
    }

    /**
     * Returns true if configuration specifies that only local traffic is allowed
     * on the proxy and console ports.
     * 
     * @return
     */
	public boolean isAllowingLocalTrafficOnly() {
		return allowLocalTrafficOnly;
	}

	/**
     * Configures whether local traffic only is accepted on procy and console
     * ports.
     * 
     * @return
     */
	public void setAllowingLocalTrafficOnly(boolean bool) {
		allowLocalTrafficOnly = bool;
	}

	/**
	 * Overrides the title of console pages to help keep track of which window
	 * or browser tab contains your http console.
	 * 
	 * @param consoleTitle
	 */
	public void setConsoleTitle(String consoleTitle) {
		this.customConsoleTitle = consoleTitle;
	}
	
	/**
	 * Returns the value that should override the default title of console 
	 * pages to help keep track of which window
	 * or browser tab contains your http console.
	 */
	public String getConsoleTitle() {
		return customConsoleTitle;
	}

	/**
	 * Uses the apache mime.types file from 
	 * http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types
	 * to translate from a file extension to a content type or drop back to 
	 * application/octet-stream if not found or if no extension is had.
	 * 
	 * @param path
	 * @return
	 */
	public String getContentType(String path) {
		int idx = path.lastIndexOf('.'); 
		if (idx == -1) {
			return DEFAULT_MIME_TYPE;
		}
		String ext = path.substring(idx+1);
		String type = mimeTypes.get(ext);
		
		if (type == null) {
			return DEFAULT_MIME_TYPE;
		}
		return type;
	}
}
