package org.lds.sso.appwrap;

import java.io.File;
import java.io.FileReader;
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
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;

public class Service {

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			throw new IllegalStateException("An xml configuration file path must be specified when starting.");
		}
		File file = new File(args[0]);
		if (! file.exists()) {
			throw new IllegalArgumentException("File '" + args[0] + "' not found.");
		}
		if (file.isDirectory()) {
			throw new IllegalArgumentException("File '" + file.getAbsolutePath() + "' is a directory.");
		}
		FileReader reader = new FileReader(file);
		XmlConfigLoader2.load(reader, "from file " + file.getAbsolutePath());
		
		/*
		cfg.setProxyPort(82);
		cfg.setConsolePort(81);
		cfg.setCookieName("lds-policy");
		cfg.addGlobalHeader("policy-service-url", "http://labs-local.lds.org:81/rest/");
		//cfg.setLoginPage("http://labs-local.lds.org:81/admin/selectUser.jsp");
		cfg.setSignInPage("http://labs-local.lds.org:82/auth/ui/sign-in?a=b");
		
		UserManager mgr = cfg.getUserManager();
		mgr.setUser("ngia", "pwda");
		mgr.addHeaderForLastUserAdded("policy-birthdate", "1960-09-25");
		//mgr.addPermissionForLastUserAdded("GET", "/auth/_app/debug.xqy");
		
		mgr.setUser("ngib", "pwdb");
		mgr.addHeaderForLastUserAdded("policy-preferred-name", "Billy-bob");
		mgr.addHeaderForLastUserAdded("policy-birthdate", "1980-06-18");

		mgr.setUser("ngic", "pwdc");
		
		TrafficManager appMgr = cfg.getTrafficManager();
		appMgr.addUnenforcedUrl("http://labs-local.lds.org:80/auth/ui/*");
		
		// add global permissions for auth'd users
		appMgr.addPermittedUrl("GET", "/auth/_app/debug.xqy*");
		
		appMgr.setSite("labs-local.lds.org", "/auth", "/auth", 8411);
		*/
		Config cfg = Config.getInstance();
		System.out.println("admin-rest port: " + cfg.getConsolePort());
		System.out.println("http proxy port: " + cfg.getProxyPort());

		// assumes that this directory contains .html and .jsp files
		// This is just a directory within your source tree, and can be exported as part of your normal .jar
		final String WEBAPPDIR = "webapp";

		final Server server = new Server(cfg.getConsolePort());
		final String CONTEXTPATH = "/admin";
		
		// for localhost:port/admin/index.html and whatever else is in the webapp directory
		final URL warUrl = Service.class.getClassLoader().getResource(WEBAPPDIR);
		final String warUrlString = warUrl.toExternalForm();
		ConfigInjectingHandlerList cfgInjector = new ConfigInjectingHandlerList(cfg);
		cfgInjector.addHandler(new WebAppContext(warUrlString, CONTEXTPATH));

		HandlerCollection handlers = new HandlerCollection();
		handlers.addHandler(cfgInjector);
		handlers.addHandler(new GetCookieName("/rest/identity/getCookieNameForToken", cfg));
		handlers.addHandler(new AuthNHandler("/rest/identity/authenticate", cfg));
		handlers.addHandler(new AuthZHandler("/rest/identity/authorize", cfg));
		handlers.addHandler(new IsTokenValidHandler("/rest/identity/isTokenValid", cfg));
		handlers.addHandler(new LogoutHandler("/rest/identity/logout", cfg));
		handlers.addHandler(new SelectUserHandler("/ui/set-user", cfg));
		handlers.addHandler(new SelectSessionHandler("/ui/set-session", cfg));
		handlers.addHandler(new Add404UriToCfgHandler("/ui/add-uri-to-", cfg));
		handlers.addHandler(new TerminateSessionHandler("/ui/terminate-session", cfg));
		handlers.addHandler(new TrafficRecordingHandler("/ui/traffic/recording/", cfg));
		server.setHandler(handlers);

		// for localhost:port/servlets/cust, etc.
		//final Context context = new Context(server, "/servlets", Context.SESSIONS);
		//context.addServlet(new ServletHolder(new CustomerServlet(whatever)), "/cust");
		//context.addServlet(new ServletHolder(new UserServlet(whatever)), "/user");

		server.start();	
		
		ProxyListener proxy = new ProxyListener(cfg);
		Thread proxyRunner = new Thread(proxy);
		proxyRunner.setDaemon(true);
		proxyRunner.setName("Proxy Listener");
		proxyRunner.start();
		
		while (true) {
			Thread.sleep(10000);
		}
	}
}
