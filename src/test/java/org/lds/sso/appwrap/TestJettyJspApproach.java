package org.lds.sso.appwrap;

import java.net.URL;

import org.lds.sso.appwrap.rest.AuthNHandler;
import org.lds.sso.appwrap.rest.AuthZHandler;
import org.lds.sso.appwrap.rest.GetCookieName;
import org.lds.sso.appwrap.rest.IsTokenValidHandler;
import org.lds.sso.appwrap.rest.LogoutHandler;
import org.mortbay.jetty.HandlerContainer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.webapp.WebAppContext;
import org.testng.annotations.Test;

public class TestJettyJspApproach {

	@Test
	public void testJettyJsp() throws Exception {
		Config cfg = new Config();
		cfg.getUserManager().setUser("ngia", "pwda");
		cfg.getUserManager().setUser("ngib", "pwdb");
		cfg.getUserManager().setUser("ngic", "pwdc");
		cfg.getUserManager().addPermission("ngia", "GET", "URLA");
		cfg.getUserManager().addPermission("ngib", "GET", "URLB");
		cfg.getUserManager().addPermission("ngic", "GET", "URLC");
		cfg.getUserManager().addHeader("mgia", "name", "value");
		
		System.out.println("admin port: " + cfg.getConsolePort());
		System.out.println("app port: " + cfg.getProxyPort());

		// assumes that this directory contains .html and .jsp files
		// This is just a directory within your source tree, and can be exported as part of your normal .jar
		final String WEBAPPDIR = "webapp";

		final Server server = new Server(cfg.getProxyPort());
		final String CONTEXTPATH = "/admin";
		
		// for localhost:port/admin/index.html and whatever else is in the webapp directory
		final URL warUrl = TestJettyJspApproach.class.getClassLoader().getResource(WEBAPPDIR);
		final String warUrlString = warUrl.toExternalForm();
		HandlerCollection handlers = new HandlerCollection();
		handlers.addHandler(new WebAppContext(warUrlString, CONTEXTPATH));
		handlers.addHandler(new GetCookieName("/rest/identity/getCookieNameForToken", cfg));
		handlers.addHandler(new AuthNHandler("/rest/identity/authenticate", cfg));
		handlers.addHandler(new AuthZHandler("/rest/identity/authorize", cfg));
		handlers.addHandler(new IsTokenValidHandler("/rest/identity/isTokenValid", cfg));
		handlers.addHandler(new LogoutHandler("/rest/identity/logout", cfg));
		server.setHandler(handlers);

		// for localhost:port/servlets/cust, etc.
		//final Context context = new Context(server, "/servlets", Context.SESSIONS);
		//context.addServlet(new ServletHolder(new CustomerServlet(whatever)), "/cust");
		//context.addServlet(new ServletHolder(new UserServlet(whatever)), "/user");

		server.start();	
		while (true) {
			Thread.sleep(5000);
		}
	}
}
