package org.lds.sso.appwrap;

import org.lds.sso.appwrap.bootstrap.Command;
import org.lds.sso.appwrap.exception.UnableToListenOnProxyPortException;
import org.lds.sso.appwrap.exception.UnableToStartJettyServerException;
import org.lds.sso.appwrap.exception.UnableToStopJettyServerException;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.proxy.ProxyListener;
import org.lds.sso.appwrap.rest.*;
import org.lds.sso.appwrap.rest.oes.v1.ArePermitted;
import org.lds.sso.appwrap.rest.oes.v1.AreTokensValid;
import org.lds.sso.appwrap.rest.oes.v1.GetOesV1CookieName;
import org.lds.sso.appwrap.ui.ImAliveHandler;
import org.lds.sso.appwrap.ui.rest.*;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

	//protected ProxyListener proxy = null;
	protected Thread proxyRunner = null;
	protected ProxyListener proxy;
	protected Server server = null;
	private String cfgSource;
	private final String cfgPath;

	private static final Map<String, Service> instances = new HashMap<String, Service>();

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
	 * Loads the service's configuration and sets up the jetty handling chain
	 * so that we are ready to start listening for requests.
	 * @param cfgPath
	 * @throws Exception
	 */
	private Service(String cfgPath) throws Exception {
		this.cfgPath = cfgPath;

        SourceAndReader sar = getCfgReader(cfgPath);
        Reader cfgrd = sar.getReader();
        cfgSource = sar.getSrc();

		XmlConfigLoader2.load(cfgrd, "from " + cfgPath);

		Config cfg = Config.getInstance();

		// Close down our reader so that the config file can be updated externally.
		cfgrd.close();

		// assumes that this directory contains .html and .jsp files
		// This is just a directory within your source tree, and can be exported as part of your normal .jar
		final String WEBAPPDIR = "webapp";

		if (cfg.getConsolePort() == 0) {
		    server = new Server();
	        Connector connector=new SocketConnector();
	        connector.setPort(0);
	        server.setConnectors(new Connector[]{connector});
		}
		else {
	        server = new Server(cfg.getConsolePort());
		}
		final String CONTEXTPATH = "/admin";

		// for localhost:port/admin/index.html and whatever else is in the webapp directory
		final URL warUrl = Service.class.getClassLoader().getResource(WEBAPPDIR);
		final String warUrlString = warUrl.toExternalForm();
		WebAppContext wac = new WebAppContext(warUrlString, CONTEXTPATH);
		JettyWebappUrlAdjustingHandler adj = new JettyWebappUrlAdjustingHandler(CONTEXTPATH, WEBAPPDIR, wac);
		SignInPageCdssoHandler cdsso = new SignInPageCdssoHandler(new String[] {"selectUser.jsp", "simpleSelectUser.jsp", "codaUserSelect.jsp"}, adj);
		ConfigInjectingHandlerList cfgInjector = new ConfigInjectingHandlerList();
        cfgInjector.addHandler(cdsso);
        //cfgInjector.addHandler(adj);

		HandlerCollection handlers = new HandlerCollection();
		if (cfg.getRestVersion() == null) {
		    throw new IllegalArgumentException("Must declare the REST interface that the Simulator should expose. " +
		    		"See documentation for the <config> element's rest-version attribute. Supported versions are: "
		            + RestVersion.getValidIdentifiers());
		}
		RestVersion rv = cfg.getRestVersion();
        String base = rv.getRestUrlBase();
		switch(rv) {
		case OPENSSO :
            LogUtils.info(logger, "Configuring single {0} rest service at: {1}{2}", rv.getVersionId(), cfg.getConsolePort(), base);
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
	                    LogUtils.info(logger, "Configuring {0} rest service for site {1}:{2} at: {3}", rv.getVersionId(), site.getHost(), site.getPort(), serviceBase);
	                    cfg.getTrafficRecorder().addRestInst(base, baseResolved, baseResolved + site.getHost() + "/getCookieName", site.getHost());
	                    handlers.addHandler(new GetOesV1CookieName(serviceBase + "getCookieName"));
	                    handlers.addHandler(new AreTokensValid(serviceBase + "areTokensValid"));
	                    handlers.addHandler(new ArePermitted(serviceBase + "arePermitted", site.getHost()));
	                }
	            }
		    }
		}

		handlers.addHandler(new SelectUserHandler("/admin/action/set-user"));
        handlers.addHandler(new SelectUserHandler("/auth/ui/authenticate"));
		handlers.addHandler(new SelectSessionHandler("/admin/action/set-session"));
		handlers.addHandler(new TerminateSessionHandler("/admin/action/terminate-session"));
        handlers.addHandler(new TrafficRecordingHandler("/admin/action/recording/"));
        handlers.addHandler(new ImAliveHandler(ImAliveHandler.IS_ALIVE_PATH));
        handlers.addHandler(new LogFileHandler("/admin/logs"));
        handlers.addHandler(new TerminateSimulatorHandler("/admin/shutdown"));

		// placing webapp handler after other handlers allows for actions to be placed
		// under same root context '/admin'.
		handlers.addHandler(cfgInjector);
		server.setHandler(handlers);
		server.setGracefulShutdown(1000);
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
    		ClassLoader cldr = Service.class.getClassLoader();
    		InputStream source = cldr.getResourceAsStream(path);

    		if (source == null) {
        		throw new IllegalArgumentException("Unable to find resource '"
        			+ path + "' on classpath.");
    		}
    		reader = new InputStreamReader(source);
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

	public String getCfgPath() {
		return cfgPath;
	}

	/**
	 * Starts the embedded jetty server and http proxy.
	 * @throws InterruptedException 
	 * @throws UnableToStartJettyServerException 
	 * @throws UnableToListenOnProxyPortException 
	 *
	 * @throws Exception
	 */
	public void start() throws InterruptedException, UnableToStartJettyServerException, 
	    UnableToListenOnProxyPortException  {
		Config cfg = Config.getInstance();
		cfg.setShimStateCookieId(cfgSource);

		try {
	        server.start();
		}
		catch(Exception e) {
		    throw new UnableToStartJettyServerException(e);
		}

		Connector[] connectors = server.getConnectors();
		cfg.setConsolePort(connectors[0].getLocalPort());

		//ProxyListener proxy = null;
		try {
		    proxy = new ProxyListener(cfg);
		}
		catch(IOException ioe) {
		    throw new UnableToListenOnProxyPortException(ioe);
		}
		proxyRunner = new Thread(proxy);
		proxyRunner.setDaemon(true);
		proxyRunner.setName("Proxy Listener");
		proxyRunner.start();

		while (server.isStarted() == false && proxy.isRunning() == false) {
		        Thread.sleep(1000);
		}
		StringBuffer line = new StringBuffer(cfg.getServerName().length());
		for (int i=0; i<cfg.getServerName().length(); i++) {
		    line.append('-');
		}
        dualLog("---------------------{0}\n" +
        "simulator version  : {1}\n" + 
        "console-rest port  : {2}\n" +
        "http proxy port    : {3}\n" + 
        "Rest Interface     : {4}\n" +
        "---------------------{5}\n" +
        "Simulator Console and Proxy are ready",
        line.toString(),cfg.getServerName(),String.valueOf(cfg.getConsolePort()),String.valueOf(cfg.getProxyPort()),cfg.getRestVersion().getVersionId(),line.toString());
	}

	private Thread runThread;
	private ConfigFileMonitor fileMonitor = null;

	public void startAndBlock() throws Exception {
		if (null == fileMonitor) {
			fileMonitor = new ConfigFileMonitor(command, this);
		}
		start();

		runThread = new Thread();
		while (true) {
			try {
				runThread.sleep(10000);
			}
			catch(Exception e) {
				throw new RuntimeException("Service incurred exception. Exiting.", e);
			}
		}
	}

	public Thread getRunThread() {
		return runThread;
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
		if (server.isStarted()) {
		    try {
		        server.stop();
		    }
		    catch (Exception e) {
		        throw new UnableToStopJettyServerException(e);
		    }
		}
		if ( proxy != null ) {//Runner != null ) {
			proxy.closeSocket(); //proxyRunner.interrupt();
		}
		new Config(); // eww... TODO:  Refactor Config to use a standard singleton pattern so we don't have to do stuff like this.
	}

	public boolean isStarted() {
		return server != null && server.isStarted() && proxyRunner != null && proxyRunner.isAlive();
	}

	public boolean isStopped() {
		return server != null && server.isStopped() && proxyRunner != null && !proxyRunner.isAlive();
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
			if (!cfgPath.toLowerCase().startsWith(Service.STRING_PREFIX) &&
				!cfgPath.toLowerCase().startsWith(Service.CLASSPATH_PREFIX)) {
				file = new File(cfgPath);
				lastUpdateTime = file.lastModified();
				// only watch the file-based config for changes
				LogUtils.info(Service.logger, "Starting up config file update monitor.  Monitoring config file: " + cfgPath);
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
		while (true) {
			try {
				Thread.sleep(5000);			// check file update every 5 seconds
				if (file.lastModified() != lastUpdateTime) {
					LogUtils.info(Service.logger, "@@ Config file has been updated, restarting server.");
					lastUpdateTime = file.lastModified();
					// The configuration file has changed, restart the service.
					svc.stop();
					svc.getRunThread().interrupt();
					svc.startAndBlock();
				}
			}
			catch (InterruptedException e) {
				// just eat it
			}
			catch (UnableToStopJettyServerException e) {
				LogUtils.warning(Service.logger, "Exception trying to shut down the server: " + e);
			}
			catch (Exception e) {
				LogUtils.warning(Service.logger, "Exception in file monitor: " + e);
			}
		}
	}
}
