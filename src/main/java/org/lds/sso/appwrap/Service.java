package org.lds.sso.appwrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.proxy.ProxyListener;
import org.lds.sso.appwrap.rest.AuthNHandler;
import org.lds.sso.appwrap.rest.AuthZHandler;
import org.lds.sso.appwrap.rest.GetCookieName;
import org.lds.sso.appwrap.rest.IsTokenValidHandler;
import org.lds.sso.appwrap.rest.LogoutHandler;
import org.lds.sso.appwrap.rest.RestVersion;
import org.lds.sso.appwrap.rest.oes.v1.ArePermitted;
import org.lds.sso.appwrap.rest.oes.v1.AreTokensValid;
import org.lds.sso.appwrap.rest.oes.v1.EncodeLinks;
import org.lds.sso.appwrap.rest.oes.v1.GetOesV1CookieName;
import org.lds.sso.appwrap.rest.oes.v1.HandlerHitLogger;
import org.lds.sso.appwrap.ui.ImAliveHandler;
import org.lds.sso.appwrap.ui.rest.Add404UriToCfgHandler;
import org.lds.sso.appwrap.ui.rest.ConfigInjectingHandlerList;
import org.lds.sso.appwrap.ui.rest.JettyWebappUrlAdjustingHandler;
import org.lds.sso.appwrap.ui.rest.LogFileHandler;
import org.lds.sso.appwrap.ui.rest.SelectSessionHandler;
import org.lds.sso.appwrap.ui.rest.SelectUserHandler;
import org.lds.sso.appwrap.ui.rest.TerminateSessionHandler;
import org.lds.sso.appwrap.ui.rest.TrafficRecordingHandler;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;

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
	private static final Logger cLog = Logger.getLogger(Service.class);
	
	public static final String CLASSPATH_PREFIX = "classpath:";
	private static final String STRING_PREFIX = "string:";
	
	//protected ProxyListener proxy = null;
	protected Thread proxyRunner = null;
	protected Server server = null;
	private String cfgSource;

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
	
	/**
	 * Loads the service's configuration and sets up the jetty handling chain
	 * so that we are ready to start listening for requests.
	 * @param cfgPath
	 * @throws Exception
	 */
	public Service(String cfgPath) throws Exception {
        SourceAndReader sar = getCfgReader(cfgPath);
        Reader cfgrd = sar.getReader();
        cfgSource = sar.getSrc();
        
		XmlConfigLoader2.load(cfgrd, "from " + cfgPath);
		
		Config cfg = Config.getInstance();

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
		ConfigInjectingHandlerList cfgInjector = new ConfigInjectingHandlerList();
		cfgInjector.addHandler(adj);

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
            System.out.println("Starting single " + rv.getVersionId() + " rest service at: " + base);
            cfg.getTrafficRecorder().addRestInst(base, base + "getCookieNameForToken", "n/a");
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
		        System.out.println("Starting single " + rv.getVersionId() + " rest service at: " + base);
	            cfg.getTrafficRecorder().addRestInst(base, base + "getCookieName", "''");
                handlers.addHandler(new GetOesV1CookieName(base + "getCookieName"));
                handlers.addHandler(new AreTokensValid(base + "areTokensValid"));
                handlers.addHandler(new ArePermitted(base + "arePermitted", ""));
                handlers.addHandler(new EncodeLinks(base + "encode"));
		    }
		    else {
		        /*
		         * Start one rest service per declared unique by-site host.
		         */
		        Set<String> hosts = new TreeSet<String>();
	            for ( SiteMatcher site : tmgr.getSites()) {
	                String serviceBase = base + site.getHost() + "/";
	                if( ! hosts.contains(site.getHost())) {
	                    hosts.add(site.getHost());
	                    System.out.println("Starting " + rv.getVersionId() + " rest service for site " + site.getHost() + " at: " + serviceBase);
	                    cfg.getTrafficRecorder().addRestInst(base, base + "getCookieName", site.getHost());
	                    handlers.addHandler(new GetOesV1CookieName(serviceBase + "getCookieName"));
	                    handlers.addHandler(new AreTokensValid(serviceBase + "areTokensValid"));
	                    handlers.addHandler(new ArePermitted(serviceBase + "arePermitted", site.getHost()));
	                    handlers.addHandler(new EncodeLinks(serviceBase + "encode"));
	                }
	            }
		    }
		}
		
		handlers.addHandler(new SelectUserHandler("/admin/action/set-user"));
        handlers.addHandler(new SelectUserHandler("/auth/ui/authenticate"));
		handlers.addHandler(new SelectSessionHandler("/admin/action/set-session"));
		handlers.addHandler(new Add404UriToCfgHandler("/admin/action/add-uri-to-"));
		handlers.addHandler(new TerminateSessionHandler("/admin/action/terminate-session"));
        handlers.addHandler(new TrafficRecordingHandler("/admin/action/recording/"));
        handlers.addHandler(new ImAliveHandler(ImAliveHandler.IS_ALIVE_PATH));
        handlers.addHandler(new LogFileHandler("/admin/logs"));
		
		// placing webapp handler after other handlers allows for actions to be placed 
		// under same root context '/admin'.
		handlers.addHandler(cfgInjector);
		server.setHandler(handlers);
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
	
	/**
	 * Starts the embedded jetty server and http proxy.
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		Config cfg = Config.getInstance();
		cfg.setShimStateCookieId(cfgSource);

		server.start();
		
		Connector[] connectors = server.getConnectors();
		cfg.setConsolePort(connectors[0].getLocalPort());

		ProxyListener proxy = new ProxyListener(cfg);
		proxyRunner = new Thread(proxy);
		proxyRunner.setDaemon(true);
		proxyRunner.setName("Proxy Listener");
		proxyRunner.start();

		dualLog("simulator version  : " + cfg.getServerName());
        dualLog("admin-rest port    : " + cfg.getConsolePort());
        dualLog("http proxy port    : " + cfg.getProxyPort());
        dualLog("Rest Interface     : " + cfg.getRestVersion().getVersionId());
		if (null != cfg.getGlobalHeaders()) {
			for (NvPair pair : cfg.getGlobalHeaders()) {
				dualLog(pair.getName() + " : " + pair.getValue());
			}
		}
	}
	
	/**
	 * Logs to both console and log file.
	 * 
	 * @param msg
	 */
	private void dualLog(String msg) {
        System.out.println(msg);
        cLog.info(msg);
	}
	
	/**
	 * Stops the embedded jetty web server and http proxy.
	 * 
	 * @throws Exception
	 */
	public void stop() throws Exception {
		if (server.isStarted()) {
			server.stop();
		}
		proxyRunner.interrupt();
	}

	/**
	 * Java application entry point.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
	    verifyArgs(args);
		Service svc = new Service(args[0]);
		svc.start();
		
		while (true) {
			try {
				Thread.sleep(10000);
			}
			catch(Exception e) {
				throw new RuntimeException("Service incurred exception. Exiting.", e);
			}
		}
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
