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

import org.lds.sso.appwrap.proxy.ProxyListener;
import org.lds.sso.appwrap.rest.AuthNHandler;
import org.lds.sso.appwrap.rest.AuthZHandler;
import org.lds.sso.appwrap.rest.GetCookieName;
import org.lds.sso.appwrap.rest.IsTokenValidHandler;
import org.lds.sso.appwrap.rest.LogoutHandler;
import org.lds.sso.appwrap.ui.rest.Add404UriToCfgHandler;
import org.lds.sso.appwrap.ui.rest.SelectSessionHandler;
import org.lds.sso.appwrap.ui.rest.SelectUserHandler;
import org.lds.sso.appwrap.ui.rest.TerminateSessionHandler;
import org.lds.sso.appwrap.ui.rest.TrafficRecordingHandler;
import org.mortbay.jetty.Server;
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

	//protected ProxyListener proxy = null;
	protected Thread proxyRunner = null;
	protected Server server = null;
	
	public Service(String cfgPath) throws Exception {
		Reader cfgrd = getCfgReader(cfgPath);
		XmlConfigLoader2.load(cfgrd, "from " + cfgPath);
		
		Config cfg = Config.getInstance();

		// assumes that this directory contains .html and .jsp files
		// This is just a directory within your source tree, and can be exported as part of your normal .jar
		final String WEBAPPDIR = "webapp";

		server = new Server(cfg.getConsolePort());
		final String CONTEXTPATH = "/admin";
		
		// for localhost:port/admin/index.html and whatever else is in the webapp directory
		final URL warUrl = Service.class.getClassLoader().getResource(WEBAPPDIR);
		final String warUrlString = warUrl.toExternalForm();
		ConfigInjectingHandlerList cfgInjector = new ConfigInjectingHandlerList();
		cfgInjector.addHandler(new WebAppContext(warUrlString, CONTEXTPATH));

		HandlerCollection handlers = new HandlerCollection();
		handlers.addHandler(cfgInjector);
		handlers.addHandler(new GetCookieName("/rest/identity/getCookieNameForToken"));
		handlers.addHandler(new AuthNHandler("/rest/identity/authenticate"));
		handlers.addHandler(new AuthZHandler("/rest/identity/authorize"));
		handlers.addHandler(new IsTokenValidHandler("/rest/identity/isTokenValid"));
		handlers.addHandler(new LogoutHandler("/rest/identity/logout"));
		handlers.addHandler(new SelectUserHandler("/ui/set-user"));
		handlers.addHandler(new SelectSessionHandler("/ui/set-session"));
		handlers.addHandler(new Add404UriToCfgHandler("/ui/add-uri-to-"));
		handlers.addHandler(new TerminateSessionHandler("/ui/terminate-session"));
		handlers.addHandler(new TrafficRecordingHandler("/ui/traffic/recording/"));
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
	protected Reader getCfgReader(String path) {
    	Reader reader = null;
    	
    	if (path.toLowerCase().startsWith("string:")) {
    		path = path.substring("string:".length());
    		reader = new StringReader(path);
    	}    	
    	else if (path.toLowerCase().startsWith("classpath:")) {
    		path = path.substring("classpath:".length());
    		ClassLoader cldr = this.getClass().getClassLoader();
    		InputStream source = cldr.getResourceAsStream(path);
    		
    		if (source == null) {
        		throw new IllegalArgumentException("Unable to find resource '"
        			+ path + "' on classpath.");
    		}
    		reader = new InputStreamReader(source);
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
			}
			catch (FileNotFoundException e) { // should never happen
        		throw new IllegalArgumentException("Unable to load file '"
            			+ file.getAbsolutePath() + "'.");
			}
    	}
    	return reader;
	}
	
	/**
	 * Starts the embedded jetty server and http proxy.
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		Config cfg = Config.getInstance();
		System.out.println("admin-rest port: " + cfg.getConsolePort());
		System.out.println("http proxy port: " + cfg.getProxyPort());

		server.start();	
		
		ProxyListener proxy = new ProxyListener(cfg);
		proxyRunner = new Thread(proxy);
		proxyRunner.setDaemon(true);
		proxyRunner.setName("Proxy Listener");
		proxyRunner.start();
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
		if (args.length < 1) {
			throw new IllegalStateException("An xml configuration file path must be specified when starting.");
		}
		
		Service svc = new Service(args[0]);
		svc.start();
		
		while (true) {
			Thread.sleep(10000);
		}
	}
}
