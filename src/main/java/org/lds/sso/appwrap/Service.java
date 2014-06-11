package org.lds.sso.appwrap;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.lds.sso.appwrap.bootstrap.Command;
import org.lds.sso.appwrap.exception.UnableToListenOnHttpsProxyPortException;
import org.lds.sso.appwrap.exception.UnableToListenOnProxyPortException;
import org.lds.sso.appwrap.exception.UnableToStartJettyServerException;
import org.lds.sso.appwrap.exception.UnableToStopJettyServerException;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.proxy.ListenerCoordinator;
import org.lds.sso.appwrap.proxy.ProxyListener;
import org.lds.sso.appwrap.proxy.tls.HttpsProxyListener;
import org.lds.sso.appwrap.rest.*;
import org.lds.sso.appwrap.rest.exposeews.v1.CookieName;
import org.lds.sso.appwrap.rest.exposeews.v1.UserNameForToken;
import org.lds.sso.appwrap.rest.oes.v1.ArePermitted;
import org.lds.sso.appwrap.rest.oes.v1.AreTokensValid;
import org.lds.sso.appwrap.rest.oes.v1.GetOesV1CookieName;
import org.lds.sso.appwrap.security.LocalHostOnlyEnforcingHandler;
import org.lds.sso.appwrap.ui.FavIconHandler;
import org.lds.sso.appwrap.ui.ImAliveHandler;
import org.lds.sso.appwrap.ui.rest.*;
import org.lds.stack.logging.ConsoleHandler;
import org.lds.stack.logging.SingleLineFormatter;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * The entry point for starting up the shim. Supports both application execution
 * via a main method and programmatic execution via constructor
 * {@link #Service(String)} and the {@link #start()} and {@link #stop()}
 * methods.
 *
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class Service {
	public static final Logger logger = Logger.getLogger(Service.class.getName());

	public static final String CLASSPATH_PREFIX = "classpath:";
	public static final String STRING_PREFIX = "string:";

    /**
     * The physical classpath based path to the templates used for the http console.
     * This path is loaded via Service.getResource() meaning that the full path is
     * relative to the package directory structure of this class. Ditto on the static
     * files.
     */
    public static final String UI_TEMPLATES_PATH = "ui/templates";
    public static final String UI_STATICS_PATH = "ui/static";

    // set up http console's UI templates and related static resources handler
    private final Configuration uiCfg = loadFreemarkerConfig();
    private final ResourceHandler uiStaticsHandler = loadUiStaticResourcesHandler();

    //protected ProxyListener proxy = null;
    protected Thread proxyRunner = null;
    protected ProxyListener proxy;
    protected Thread tlsProxyRunner = null;
    protected HttpsProxyListener tlsProxy;
    protected Server server = null;
    private String cfgSource;
    private boolean loggingConfigured = false;
    private final String cfgPath;

    private static final Map<String, Service> instances = new HashMap<String, Service>();


    /**
     * Two sets of resources are needed for the UI, templates and static files and both are loaded via
     * Class.getResource() and this method verifies that the containing directory is available or terminates
     * the start-up process if not found providing as helpful a message as possible as to whey if failed.
     * @param label
     * @param path
     */
    private void verifyResourceAvailability(String label, String path) {
        URL s = Service.class.getResource(path);
        if (s == null) {

            // this should always work so we use it to help point out where we are looking for templates
            s = Service.class.getResource("Service.class");
            if (s == null) {
                // should never happen but make sure we handle if it does
                throw new IllegalStateException("Cannot set up Wamulator Http Console. " + label + " base not found. Uses " + this.getClass().getName()
                        + ".getResource(\"" + path + "\") and that is not found.");
            }
            else {
                // tell'em where we were looking so they/we can figure out what is wrong after it dies
                String url = s.toExternalForm();
                url = url.substring(0, url.indexOf("Service.class"));
                url += path;
                throw new IllegalStateException("Cannot set up Wamulator Http Console. " + label + " base not found at: " + url);
            }
        }
        else {
            logger.log(Level.INFO, "Wamulator Http Console " + label + " loaded from: " + s.toExternalForm());
        }
    }

    /**
     * Sets up handler for static resources used by the http console.
     *
     * @return
     */
    private ResourceHandler loadUiStaticResourcesHandler() {
        verifyResourceAvailability("Static Resources", Service.UI_STATICS_PATH);
        URL statics = Service.class.getResource(Service.UI_STATICS_PATH);
        ResourceHandler staticsHandler = new ResourceHandler();

        try {
            staticsHandler.setBaseResource(Resource.newResource(statics));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot set up Wamulator Http Console. Static Resources base can't be configured", e);
            System.exit(1);
        }
        return staticsHandler;
    }

    /**
     * Sets up Freemarker template library configuration.
     *
     * @return
     */
    private Configuration loadFreemarkerConfig() {
        logger.log(Level.INFO, "Searching for Wamulator Http Console Templates");
        Configuration fmc = new Configuration();

        // verify templates are accessible then set location for freemarker
        verifyResourceAvailability("Templates", UI_TEMPLATES_PATH);
        fmc.setClassForTemplateLoading(Service.class, "");

        fmc.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
        fmc.setDefaultEncoding("UTF-8");
        fmc.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        fmc.setIncompatibleImprovements(new Version(2, 3, 20));
        return fmc;
    }

	/**
	 * Class for conveying from {@link Service#getCfgReader(String)} the config
	 * file source unique identifier for this run and a reader ready to read the
	 * config file.
	 */
	public static class SourceAndReader {
	    private Reader reader = null;
        private String src = null;

        SourceAndReader(String source, Reader rdr){
	        this.src = source;
	        this.reader = rdr;
	    }

        public Reader getReader() {
            return reader;
        }

        public String getSrc() {
            return src;
        }
	}

	private static Thread shutdownHook = new Thread( "Wamulator shutdown" )
    {
        public void run()
        {
            if ( instances != null && instances.size() > 0 ) {
            	for ( Map.Entry<String, Service> entry : instances.entrySet() ) {
	            	try {
	            		entry.getValue().stop();
	            		entry.getValue().stopBlocking();

	            	} catch ( Exception e ) {
	            		LogUtils.throwing(logger, "Shutdown failed for service with cfgPath: {0}", e, entry.getKey());
	            	}
            	}
            }
        }
    };

    static
    {
        shutdownHook.setContextClassLoader( null );
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
    
	public static final Service invoke(Command command) {
		Service.command = command;
		command.execute();
		return getService(command.getCfgPath());
	}

	public static final Service getService(String cfgPath) {
		if ( instances.get(cfgPath) == null || instances.get(cfgPath).isStopped() ) {
			synchronized(Service.class) {
				if ( instances.get(cfgPath) == null || instances.get(cfgPath).isStopped() ) {
					try {
						instances.put(cfgPath, new Service(cfgPath));
					} catch ( Exception e ) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return instances.get(cfgPath);
	}
	
	/**
	 * Replace default ConsoleHandler with better stack ConsoleHandler
	 */
	public void configureLogging() {
		if(loggingConfigured) {
			return;
		}
		//Replacing any instances of the default ConsoleHandler with much better Stack ConsoleHandler. :)
		Logger logger = Logger.getLogger("org.lds.sso.appwrap");
		Logger rootLogger = Logger.getLogger("");
		for(Handler handler : rootLogger.getHandlers()) {
			if(handler instanceof java.util.logging.ConsoleHandler) {
				ConsoleHandler consoleHandler = new ConsoleHandler();
				consoleHandler.setLevel(handler.getLevel());
				consoleHandler.setFormatter(new SingleLineFormatter());
				logger.addHandler(consoleHandler);
				logger.setUseParentHandlers(false);
				loggingConfigured = true;
			}
		}
	}

	/**
	 * To prevent memory leaking remove Stack ConsoleHandler on shutdown.
	 */
	public void cleanupLogging() {
		if(!loggingConfigured) {
			return;
		}
		//Replacing any instances of the default ConsoleHandler with much better Stack ConsoleHandler. :)
/*		Logger logger = Logger.getLogger("org.lds.sso.appwrap");
		logger.setUseParentHandlers(true);
		for(Handler handler : logger.getHandlers()) {
			logger.removeHandler(handler);
		}
*/
		Logger logger = Logger.getLogger("org.lds.sso.appwrap");
		LogManager.getLogManager().reset();
		try {
			LogManager.getLogManager().readConfiguration();
		} catch (IOException e) {
			System.out.println("Error attempting to re-configure WAM Logging.");
		}
		logger.setUseParentHandlers(true);
		loggingConfigured = false;
	}

	public static Service restartService(Service service) {
		try {
			String path = service.getCfgPath();
			service.stop();
			instances.remove(path);
			Service newService = Service.getService(path);
			newService.startAndBlock();
			return newService;
		} catch(Exception e) {
			throw new RuntimeException("Failed to restart Service.  Exiting emulator", e);
		}
	}

	/**
	 * Loads the service's configuration and sets up the jetty handling chain
	 * so that we are ready to start listening for requests.
	 * @param cfgPath
	 * @throws Exception
	 */
	private Service(String cfgPath) throws Exception {
		this.cfgPath = cfgPath;

        SourceAndReader sar = getCfgReader(cfgPath);
        Reader cfgrd = null;

        try {
            cfgrd =sar.getReader();
            cfgSource = sar.getSrc();

            XmlConfigLoader2.load(cfgrd, "from " + cfgPath);
            // Close down our reader so that the config file can be updated externally.
            cfgrd.close();
        }
        finally {
            Utils.quietlyClose(cfgrd);
        }

		Config cfg = Config.getInstance();

        /*
         * Create a collection of handlers that know what URL to watch for and
         * handle the request if not already handled by someone else. All get
         * called regardless so they need to verify the request still needs to
         * be handled before doing so.
         */
		HandlerCollection handlers = null;
		if (cfg.isAllowingLocalTrafficOnly()) {
			LogUtils.info(logger, "Restricting console and REST traffic to local host.", (Object []) null);
			handlers = new LocalHostOnlyEnforcingHandler();
		}
		else {
			handlers = new HandlerCollection();
		}

        // handle root path '/' redirect
        handlers.addHandler(new RedirectToHandler("/", Config.WAMULATOR_CONSOLE_BASE + "/traffic"));

        // set up http console UI handling: all requests starting with the console path /wamulator/console
        // will be handled by this handler including 404 not founds.
        HttpConsoleHandlersList uiHandlers = new HttpConsoleHandlersList(Config.WAMULATOR_CONSOLE_BASE);
        // plug in the sign-in page with its cdsso wrapping handler
        SigninPageController signInPC = new SigninPageController(Config.WAMULATOR_SIGNIN_PAGE_PATH, uiCfg);
        uiHandlers.addHandler(new SignInPageCdssoHandler(signInPC));
        // plug in the headers debugging helper page
        uiHandlers.addHandler(new HeadersPageController(Config.WAMULATOR_CONSOLE_BASE + "/headers", uiCfg));

        uiHandlers.addHandler(new ListUsersController(Config.WAMULATOR_CONSOLE_BASE + "/listUsers", uiCfg));
        uiHandlers.addHandler(new ShowTrafficController(Config.WAMULATOR_CONSOLE_BASE + "/traffic", uiCfg));
        uiHandlers.addHandler(new ShowRestTrafficController(Config.WAMULATOR_CONSOLE_BASE + "/rest-traffic", uiCfg));
        uiHandlers.addHandler(new UrlPrefixTrimmingHandler(Config.WAMULATOR_CONSOLE_BASE, uiStaticsHandler));
        handlers.addHandler(uiHandlers);

        // add handlers for fine grained permissions restful endpoints
		RestVersion rv = cfg.getRestVersion();
        String base = rv.getRestUrlBase();
		switch(rv) {
		case OPENSSO :
            LogUtils.info(logger, "Configuring single {0} rest service at: {1}{2}", rv.getVersionId(), String.valueOf(cfg.getConsolePort()), base);
            cfg.getTrafficRecorder().addRestInst(base, base, base + "getCookieNameForToken", "n/a");
			handlers.addHandler(new GetCookieName(base + "getCookieNameForToken"));
			handlers.addHandler(new AuthNHandler(base + "authenticate"));
			handlers.addHandler(new AuthZHandler(base + "authorize"));
			handlers.addHandler(new IsTokenValidHandler(base + "isTokenValid"));
			handlers.addHandler(new LogoutHandler(base + "logout"));
			break;

		case CD_OESv1 :
		    TrafficManager tmgr = cfg.getTrafficManager();
		    /*
		     * See if we have any by-site declarations for which we start a
		     * rest service with a policy-domain specific to that site. If not
		     * then start it at the base and set the policy-domain to the
		     * empty string so that an instance will be there for the example
		     * startup files.
		     */
		    if (tmgr.getSites().size() == 0) {
		        LogUtils.info(logger, "Configuring single {0} rest service at: {1}", rv.getVersionId(), base);
		        String baseResolved = base.replace("{version}", "1.0");
	            cfg.getTrafficRecorder().addRestInst(base, baseResolved, baseResolved + "getCookieName", "''");
                handlers.addHandler(new GetOesV1CookieName(baseResolved + "getCookieName"));
                handlers.addHandler(new AreTokensValid(baseResolved + "areTokensValid"));
                handlers.addHandler(new ArePermitted(baseResolved + "arePermitted", ""));
		    }
		    else {
		        /*
		         * Start one rest service per declared unique by-site host.
		         */
		        Set<String> hosts = new TreeSet<String>();
	            for ( SiteMatcher site : tmgr.getSites()) {
                    String baseResolved = base.replace("{version}", "1.0");
	                String serviceBase = baseResolved + site.getHost() + "/";
	                if( ! hosts.contains(site.getHost())) {
	                    hosts.add(site.getHost());
	                    LogUtils.info(logger, "Configuring {0} rest service for site {1}:{2} at: {3}", rv.getVersionId(), String.valueOf(site.getHost()), String.valueOf(site.getPort()), serviceBase);
	                    cfg.getTrafficRecorder().addRestInst(base, baseResolved, baseResolved + site.getHost() + "/getCookieName", site.getHost());
	                    handlers.addHandler(new GetOesV1CookieName(serviceBase + "getCookieName"));
	                    handlers.addHandler(new AreTokensValid(serviceBase + "areTokensValid"));
	                    handlers.addHandler(new ArePermitted(serviceBase + "arePermitted", site.getHost()));
	                }
	            }
		    }
		}

        // add handlers for ui handlers
		handlers.addHandler(new SelectUserHandler(cfg.getWamulatorServiceUrlBase() + "/action/set-user"));
		handlers.addHandler(new SelectSessionHandler(cfg.getWamulatorServiceUrlBase() + "/action/set-session"));
		handlers.addHandler(new TerminateSessionHandler(cfg.getWamulatorServiceUrlBase() + "/action/terminate-session"));
        handlers.addHandler(new TrafficRecordingHandler(cfg.getWamulatorServiceUrlBase() + "/action/recording/"));
        handlers.addHandler(new ImAliveHandler(ImAliveHandler.IS_ALIVE_PATH));
        handlers.addHandler(new FavIconHandler(FavIconHandler.FAVICON_PATH));
        handlers.addHandler(new LogFileHandler(cfg.getWamulatorServiceUrlBase() + "/logs"));
        handlers.addHandler(new TerminateSimulatorHandler(cfg.getWamulatorServiceUrlBase() + "/shutdown"));

        // token authority handler so that WAMulator can front policy-exposee
        // but run against real OAM environments for hosts/apps/policies.
        handlers.addHandler(new UserNameForToken(cfg.getWamulatorServiceUrlBase() + "/oam-ta/username-for-token"));
        handlers.addHandler(new CookieName(cfg.getWamulatorServiceUrlBase() + "/oam-ta/cookie-name"));


        // now set up the jetty server with these handlers
        if (cfg.getConsolePort() == 0) {
            server = new Server(0);
        }
        else {
            server = new Server(cfg.getConsolePort());
        }
		server.setHandler(handlers);
		server.setStopTimeout(1000); // how long to wait for existing connections to finish at shutdown
	}

	/**
	 * Converts a string path to the configuration file to a Reader with support
	 * for file, classpath, and String based resources. To indicate a classpath
	 * based resource prefix the path with "classpath:". To indicate a String
	 * based resource prefix the xml configuration String with "string:".
	 *
	 * Examples:
	 *
	 * "/some/current/directory/relative/path.txt"
	 * "classpath:/file/relative/to/classpath.txt"
	 * "string:<config console-port='88' proxy-port='45'>..."
	 *
	 * @param path
	 * @return
	 */
	public static SourceAndReader getCfgReader(String path) {
    	Reader reader = null;

    	if (path.toLowerCase().startsWith(STRING_PREFIX)) {
    		path = path.substring(STRING_PREFIX.length());
    		reader = new StringReader(path);
    		return new SourceAndReader("Str" + path.hashCode(), reader);
    	}
    	else if (path.toLowerCase().startsWith(CLASSPATH_PREFIX)) {
    		path = path.substring(CLASSPATH_PREFIX.length());
    		URL source = findConfigFileOnClasspath(Thread.currentThread().getContextClassLoader(), path);

    		if (source == null) {
        		throw new IllegalArgumentException("Unable to find resource '"
        			+ path + "' on classpath.");
    		}
    		try {
    			reader = new InputStreamReader(source.openStream());
    		} catch (IOException e) { // should never happen
        		throw new IllegalArgumentException("Unable to load file '"
            			+ source + "'.");
			}
    		logger.info("Use config file located: "+source);
            return new SourceAndReader("Cp" + path.hashCode(), reader);
    	}
    	else {
    		// assume file path
    		File file = new File(path);
    		if (! file.exists()) {
        		throw new IllegalArgumentException("Unable to find file '"
            			+ file.getAbsolutePath() + "'.");
    		}
    		if (file.isDirectory()) {
    			throw new IllegalArgumentException("File '" + file.getAbsolutePath() + "' is a directory.");
    		}
    		logger.info("Use config file located: "+file.getAbsolutePath());
    		try {
				reader = new FileReader(file);
	            return new SourceAndReader("File" + path.hashCode(), reader);
			}
			catch (FileNotFoundException e) { // should never happen
        		throw new IllegalArgumentException("Unable to load file '"
            			+ file.getAbsolutePath() + "'.");
			}
    	}
	}

	/**
	 * Find the config file in the ClassLoader that is closest to the top.  Opposite of normal ClassLoader.getResource() logic.
	 * This model is necessary to effectively support classpath based reloading.
	 * @param classLoader
	 * @param path
	 * @return
	 */
	public static URL findConfigFileOnClasspath(ClassLoader classLoader, String path) {
		Enumeration<URL> results = null;
		try {
			results = classLoader.getResources(path);
		} catch(IOException e) {
			return null;
		}
		if(results == null) {
			return null;
		}
		URL lastURL = null;
		while(results.hasMoreElements()) {
			lastURL = results.nextElement();
		}
		return lastURL;
	}

	public String getCfgPath() {
		return cfgPath;
	}

	/**
	 * Starts the embedded jetty server and http proxy.
	 * @throws InterruptedException
	 * @throws UnableToStartJettyServerException
	 * @throws UnableToListenOnProxyPortException
	 * @throws UnableToListenOnHttpsProxyPortException 
	 *
	 * @throws Exception
	 */
	public void start() throws UnableToStartJettyServerException,
	    UnableToListenOnProxyPortException, UnableToListenOnHttpsProxyPortException  {
		configureLogging();
		Config cfg = Config.getInstance();
		cfg.setShimStateCookieId(cfgSource);

        try {
	        server.start();
		}
		catch(Exception e) {
		    throw new UnableToStartJettyServerException(e);
		}

		Connector[] connectors = server.getConnectors();
        ServerConnector sconn = (ServerConnector) connectors[0];
		cfg.setConsolePort(sconn.getLocalPort());
		ListenerCoordinator coordinator = new ListenerCoordinator(cfg);

        try {
            proxy = new ProxyListener(cfg, coordinator);
        }
        catch(IOException ioe) {
            throw new UnableToListenOnProxyPortException("Proxy is unable to listen on port " + cfg.getProxyPort(), ioe);
        }
        proxyRunner = new Thread(proxy);
        proxyRunner.setName("Http Proxy Listener");
        proxyRunner.start();

        // start https proxy if configured to do so
        boolean tlsEnabled = cfg.getProxyHttpsEnabled();
        if (tlsEnabled) {
            try {
                tlsProxy = new HttpsProxyListener(cfg, coordinator);
            }
            catch(IOException ioe) {
                throw new UnableToListenOnHttpsProxyPortException(ioe);
            }
        }
        tlsProxyRunner = new Thread(tlsProxy);
        tlsProxyRunner.setName("HttpS Proxy Listener");
        tlsProxyRunner.start();

        while (server.isStarted() == false || proxy.isRunning() == false
                || (tlsProxy != null && tlsProxy.isRunning() == false)) {
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {/* Do nothing */}
        }
        StringBuffer line = new StringBuffer(cfg.getServerName().length());
        for (int i=0; i<cfg.getServerName().length(); i++) {
            line.append('-');
        }
        dualLog("\n---------------------{0}\n" +
        "simulator version  : {1}\n" +
        "console-rest port  : {2}\n" +
        "http proxy port    : {3}\n" +
        (tlsEnabled ?
        "httpS proxy port   : {4}\n" : "" ) +
        "Rest Interface     : {5}\n" +
        "---------------------{6}\n" +
        "Simulator Console and Proxy are ready",
        line.toString(),
        cfg.getServerName(),
        String.valueOf(cfg.getConsolePort()),
        String.valueOf(cfg.getProxyPort()),
        String.valueOf(cfg.getProxyHttpsPort()),
        cfg.getRestVersion().getVersionId(),
        line.toString());
    }

	private volatile boolean stop = false;
	private ConfigFileMonitor fileMonitor = null;

	public void startAndBlock() throws UnableToStartJettyServerException, UnableToListenOnProxyPortException, UnableToListenOnHttpsProxyPortException {
		if (null == fileMonitor) {
			fileMonitor = new ConfigFileMonitor(command, this);
		}
		start();

		while (!stop) {
			try {
				Thread.sleep(10000);
			}
			catch(Exception e) {
				throw new RuntimeException("Service incurred exception. Exiting.", e);
			}
		}
		stop = false;
	}

	public void stopBlocking() {
		stop = true;
	}

	/**
	 * Logs to both console and log file.
	 *
	 * @param msg
	 */
	private void dualLog(String msg, Object... params) {
        LogUtils.info(logger, msg, params);
	}

	/**
	 * Stops the embedded jetty web server and http proxy.
	 * @throws UnableToStopJettyServerException
	 *
	 * @throws Exception
	 */
	public void stop() throws UnableToStopJettyServerException {
		cleanupLogging();
		if (server.isStarted()) {
		    try {
		        server.stop();
		    }
		    catch (Exception e) {
		        throw new UnableToStopJettyServerException(e);
		    }
		}
        if ( proxy != null ) {//Runner != null ) {
            try {
                proxy.stop(); //proxyRunner.interrupt();
            } catch(Exception e) {
                /* do Nothing */
            }
        }
        if ( tlsProxy != null ) {//Runner != null ) {
            try {
                tlsProxy.stop(); //proxyRunner.interrupt();
            } catch(Exception e) {
                /* do Nothing */
            }
        }
        Config.getInstance().stopLogicalSyntaxEvaluationThread();
        new Config(); // flush out config
	}

	public boolean isStarted() {
		return server != null && server.isStarted() 
		&& proxyRunner != null && proxyRunner.isAlive()
		&& tlsProxyRunner != null && tlsProxyRunner.isAlive();
	}

	public boolean isStopped() {
		return server != null && server.isStopped() 
		&& proxyRunner != null && !proxyRunner.isAlive()
		&& tlsProxyRunner != null && !tlsProxyRunner.isAlive();
	}

	private static Command command;
	/**
	 * Java application entry point.
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
	    verifyArgs(args);

	    command = Command.parseCommand(args);
	    command.execute();
	}

	/**
	 * Performs validation on arguments passed to application.
	 * @param args
	 */
	public static void verifyArgs(String[] args) {
        if (args.length < 1) {
            throw new IllegalStateException("An xml configuration file path must be specified when starting.");
        }
	}
}

class ConfigFileMonitor implements Runnable {
	protected String cfgPath;
	protected File file;
	protected Service svc;
	protected long lastUpdateTime;
	protected Thread t;
	ConfigFileMonitor(Command command, Service svc) {
		if (null != command && null != svc) {
			this.svc = svc;
			cfgPath = command.getCfgPath();
			if (!cfgPath.toLowerCase().startsWith(Service.STRING_PREFIX)) {
				if(cfgPath.toLowerCase().startsWith(Service.CLASSPATH_PREFIX)) {
					URL cfgFile = Service.findConfigFileOnClasspath(Thread.currentThread().getContextClassLoader(), cfgPath.replace(Service.CLASSPATH_PREFIX, ""));
					if(cfgFile != null) {
						file = new File(cfgFile.getFile());
					}
				} else {
					file = new File(cfgPath);
				}
				if(file == null || !file.exists()) {
					LogUtils.info(Service.logger, "Not monitoring config for updates because it isn't file-based.");
					return;
				}
				lastUpdateTime = file.lastModified();
				// only watch the file-based config for changes
				LogUtils.info(Service.logger, "Starting up config file update monitor.  Monitoring config file: " + file.getAbsolutePath());
				t = new Thread(this);
				t.setDaemon(true);
				t.start();
			}
			else {
				LogUtils.info(Service.logger, "Not monitoring config for updates because it isn't file-based.");
			}
		}
	}

	public void run() {
		boolean leave = false;
		while (!leave) {
			try {
				Thread.sleep(1000);			// check file update every 1 seconds
				if (file.lastModified() != lastUpdateTime) {
					LogUtils.info(Service.logger, "@@ Config file has been updated, restarting server.");
					lastUpdateTime = file.lastModified();
					leave = true;
				}
			}
			catch (InterruptedException e) {
				// just eat it
			}
			catch (Exception e) {
				LogUtils.warning(Service.logger, "Exception in file monitor: " + e);
			}
		}
		// The configuration file has changed, restart the service.
		Service.restartService(svc);
	}
}
