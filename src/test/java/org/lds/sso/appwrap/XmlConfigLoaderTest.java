package org.lds.sso.appwrap;

import java.util.Arrays;
import java.util.Collections;

import org.lds.sso.appwrap.XmlConfigLoader2.CfgContentHandler;
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.opensso.LegacyPropsInjectorDefs;
import org.lds.sso.clientlib.PDPAccessor;
import org.testng.Assert;
import org.testng.annotations.Test;

public class XmlConfigLoaderTest {
	@Test
	public void testResolveAliases() {
		CfgContentHandler hndlr = new CfgContentHandler();
		hndlr.aliases.put("aaa1", "vvv1");
		hndlr.aliases.put("bbb", "www");
		hndlr.aliases.put("ccc", "xxx");
		Assert.assertEquals(hndlr.resolveAliases("{{aaa1}}"), "vvv1");
		Assert.assertEquals(hndlr.resolveAliases("{{aaa1}}-"), "vvv1-");
		Assert.assertEquals(hndlr.resolveAliases("-{{aaa1}}"), "-vvv1");
		Assert.assertEquals(hndlr.resolveAliases("{{aaa1}}{{aaa1}}"), "vvv1vvv1");
		Assert.assertEquals(hndlr.resolveAliases("this {{aaa1}} that"), "this vvv1 that");
		Assert.assertEquals(hndlr.resolveAliases("this {{ccc}}{{aaa1}} that{{bbb}}"), "this xxxvvv1 thatwww");
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testUnmatchedAlias() {
		CfgContentHandler hndlr = new CfgContentHandler();
		hndlr.resolveAliases("---{{---");
	}

	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testMissingAlias() {
		CfgContentHandler hndlr = new CfgContentHandler();
		hndlr.resolveAliases("---{{jjj}}---");
	}
	
	@Test 
	public void testPath() {
		Path p = new Path();
		Assert.assertEquals(p.matches("/"), true, "path should be root value '/'");
		p.add("step");
		Assert.assertEquals(p.matches("/step"), true, "path should be '/step'");
		p.add("stop");
		Assert.assertEquals(p.matches("/step/stop"), true, "path should be '/step/stop'");
		p.remove("stop");
		Assert.assertEquals(p.matches("/step"), true, "path should be '/step'");
		p.add("stop");
		Assert.assertEquals(p.matches("/step/stop"), true, "path should be '/step/stop'");
		p.remove("stop");
		Assert.assertEquals(p.matches("/step"), true, "path should be '/step'");
		p.remove("step");
		Assert.assertEquals(p.matches("/"), true, "path should be root value '/'");
	}
	
	@Test
	public void testConfigAlias() throws Exception {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<?alias rest-port=81?>"
			+ "<config console-port='{{rest-port}}' proxy-port='45'/>";
		Config cfg = new Config();
		cfg.setConsolePort(0);
		XmlConfigLoader2.load(xml);
		Assert.assertEquals(cfg.getConsolePort(), 81, "console port should have been changed to 81.");
		Assert.assertEquals(cfg.getProxyPort(), 45, "proxy port should have been changed to 45.");
	}
	
	@Test
	public void testConfigAgent() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<?alias site=labs-local.lds.org?>"
			+ "<config console-port='88' proxy-port='45'>"
		    + "  <sso-cookie name='lds-policy' domain='.lds.org'/>"
			+ "  <sso-sign-in-url value='http://{{site}}/auth/ui/sign-in'/>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		Assert.assertEquals(cfg.getProxyPort(), 45);
		Assert.assertEquals(cfg.getCookieName(), "lds-policy");
		Assert.assertEquals(cfg.getCookieDomain(), ".lds.org");
		Assert.assertEquals(cfg.getLoginPage(), "http://labs-local.lds.org/auth/ui/sign-in");
	}

	@Test
	public void testLoadTextAliasFromClasspath() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<?alias site=classpath:XmlConfigLoaderTest.txt?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ "  <sso-sign-in-url value='http://{{site}}/auth/ui/sign-in'/>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		Assert.assertEquals(cfg.getLoginPage(), "http://test-host-domain.lds.org/auth/ui/sign-in");
	}

	@Test
	public void testUnenforcedBySitePathWildcard() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-site host='labs-local.lds.org' port='80'>"
			+ "    <unenforced cpath='/auth/ui/*'/>"
	        + "  </by-site>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		
		String url = "http://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isUnenforced(url), "should be unenforced " + url);
		
		url = "http://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertTrue(tman.isUnenforced(url), "should be unenforced " + url);
		
		url = "http://labs-local.lds.org:111/auth/ui/sign-in";
		Assert.assertFalse(tman.isUnenforced(url), 
		"due to wrong port should NOT be unenforced " + url);
		
		url = "apps://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isUnenforced(url), 
		"due to wrong scheme should NOT be unenforced " + url);
		
		url = "http://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isUnenforced(url), 
		"due to query should NOT be unenforced " + url);
		
		url = "http://labs-local.lds.org/auth/diff/path";
		Assert.assertFalse(tman.isUnenforced(url), 
		"due to incorrect path should NOT be unenforced " + url);
	}


	@Test
	public void testUnenforcedByResourcePathWildcard() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-resource uri='http://labs-local.lds.org:80/auth/ui/*' unenforced='true'/>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		
		String url = "http://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isUnenforced(url), "should be unenforced " + url);
		
		url = "http://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertTrue(tman.isUnenforced(url), "should be unenforced " + url);
		
		url = "http://labs-local.lds.org:111/auth/ui/sign-in";
		Assert.assertFalse(tman.isUnenforced(url), 
		"due to wrong port should NOT be unenforced " + url);
		
		url = "apps://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isUnenforced(url), 
		"due to wrong scheme should NOT be unenforced " + url);
		
		url = "http://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isUnenforced(url), 
		"due to query should NOT be unenforced " + url);
		
		url = "http://labs-local.lds.org/auth/diff/path";
		Assert.assertFalse(tman.isUnenforced(url), 
		"due to incorrect path should NOT be unenforced " + url);
	}

	@Test
	public void testAllowedBySitePathWildcard() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-site scheme='app' host='labs-local.lds.org' port='80'>"
			+ "    <allow action='GET,PUT' cpath='/auth/ui/*'/>"
	        + "  </by-site>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		User u = new User("", ""); // no conditions are specified so any user object can be used.
		
		String url = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);

		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to query should NOT be allowed " + url);
	}

	@Test
	public void testAllowedByResourcePathWildcard() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-resource uri='app://labs-local.lds.org:80/auth/ui/*' allow='GET,PUT'/>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		User u = new User("", ""); // no conditions are specified so any user object can be used.
		
		String url = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);

		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to query should NOT be allowed " + url);
	}

	@Test
	public void testAllowedBySiteEmptyPathWildcard() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-site scheme='app' host='labs-local.lds.org' port='80'>"
			+ "    <allow action='GET,PUT' cpath='*'/>"
	        + "  </by-site>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		User u = new User("", ""); // no conditions are specified so any user object can be used.
		
		String url = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);

		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to query should NOT be allowed " + url);
	}

	@Test
	public void testAllowedByResourceEmptyPathWildcard() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-resource uri='app://labs-local.lds.org/*' allow='GET,PUT'/>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		User u = new User("", ""); // no conditions are specified so any user object can be used.
		
		String url = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);

		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to query should NOT be allowed " + url);
	}

	@Test
	public void testAllowedBySiteQueryWildcard() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-site scheme='app' host='labs-local.lds.org' port='80'>"
			+ "    <allow action='GET,PUT' cpath='/auth/ui/sign-in?*'/>"
	        + "  </by-site>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		User u = new User("", ""); // no conditions are specified so any user object can be used.
		
		String url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);

		url = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to missing query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to missing query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to missing query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to missing query should NOT be allowed " + url);
	}

	@Test
	public void testAllowedByResourceQueryWildcard() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-resource uri='app://labs-local.lds.org/auth/ui/sign-in?*' allow='GET,PUT'/>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		User u = new User("", ""); // no conditions are specified so any user object can be used.
		
		String url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);

		url = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to missing query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to missing query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to missing query should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to missing query should NOT be allowed " + url);
	}

	@Test
	public void testAllowedBySiteQueryWithWildcard() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-site scheme='app' host='labs-local.lds.org' port='80'>"
			+ "    <allow action='GET,PUT' cpath='/auth/ui/sign-in?a=b&amp;*'/>"
	        + "  </by-site>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		User u = new User("", ""); // no conditions are specified so any user object can be used.
		
		String url = "app://labs-local.lds.org/auth/ui/sign-in?a=b&locale=eng";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?a=b&locale=eng";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?a=b&locale=eng";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in?a=b&locale=eng";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);

		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to missing query portion should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to missing query portion should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to missing query portion should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to missing query portion should NOT be allowed " + url);
	}

	@Test
	public void testAllowedByResourceQueryWithWildcard() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-resource uri='app://labs-local.lds.org/auth/ui/sign-in?a=b&amp;*' allow='GET,PUT'/>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		User u = new User("", ""); // no conditions are specified so any user object can be used.
		
		String url = "app://labs-local.lds.org/auth/ui/sign-in?a=b&locale=eng";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?a=b&locale=eng";
		Assert.assertTrue(tman.isPermitted("GET", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?a=b&locale=eng";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in?a=b&locale=eng";
		Assert.assertTrue(tman.isPermitted("PUT", url, u), "should be allowed " + url);

		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to missing query portion should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to missing query portion should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to missing query portion should NOT be allowed " + url);
		
		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to missing query portion should NOT be allowed " + url);
	}

	@Test
	public void testIsAllowedBySiteConfig() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-site host='labs-local.lds.org' port='80'>"
			+ "    <cctx-mapping cctx='/auth*' thost='127.0.0.1' tport='8411' tpath='/auth*'/>"
			+ "    <unenforced cpath='/auth/ui/*'/>"
	        + "    <allow action='GET,POST' cpath='/auth/_app/*'/>"
	        + "  </by-site>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		boolean res = tman.isUnenforced("http://labs-local.lds.org/auth/ui/sign-in");
		Assert.assertTrue(res, "should be unenforced http://labs-local.lds.org:/auth/ui/sign-in");
		res = tman.isUnenforced("http://labs-local.lds.org/auth/ui/sign-in");
		Assert.assertTrue(res, "should be unenforced http://labs-local.lds.org/auth/ui/sign-in");
		res = tman.isUnenforced("http://labs-local.lds.org:80/auth/ui/sign-in");
		Assert.assertTrue(res, "should be unenforced http://labs-local.lds.org/auth/ui/sign-in");
	}

	@Test
	public void testIsAllowedByResourcesConfig() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-resource uri='http://labs-local.lds.org/auth/ui/*' unenforced='true'/>"
			+ "  <by-resource uri='http://labs-local.lds.org/auth/_app/*' allow='GET,POST'/>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		boolean res = tman.isUnenforced("http://labs-local.lds.org/auth/ui/sign-in");
		Assert.assertTrue(res, "should be unenforced http://labs-local.lds.org:/auth/ui/sign-in");
		res = tman.isUnenforced("http://labs-local.lds.org/auth/ui/sign-in");
		Assert.assertTrue(res, "should be unenforced http://labs-local.lds.org/auth/ui/sign-in");
		res = tman.isUnenforced("http://labs-local.lds.org:80/auth/ui/sign-in");
		Assert.assertTrue(res, "should be unenforced http://labs-local.lds.org/auth/ui/sign-in");
	}

	@Test
	public void testArcaneSchemConfigIsAllowed() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-site scheme='app' host='labs-local.lds.org' port='80'>"
			+ "    <unenforced cpath='/auth/ui/*'/>"
	        + "    <allow action='GET,POST' cpath='/auth/_app/*'/>"
	        + "  </by-site>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		boolean res = tman.isUnenforced("http://labs-local.lds.org/auth/ui/sign-in");
		Assert.assertFalse(res, "due to wrong scheme should NOT be unenforced http://labs-local.lds.org:/auth/ui/sign-in");
		res = tman.isUnenforced("app://labs-local.lds.org/auth/ui/sign-in");
		Assert.assertTrue(res, "should be unenforced http://labs-local.lds.org:/auth/ui/sign-in");
	}

	@Test
	public void testArcaneSchemConfigByResourceIsAllowed() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-resource uri='app://labs-local.lds.org/auth/ui/*' unenforced='true'/>"
			+ "  <by-resource uri='app://labs-local.lds.org/auth/_app/*' allow='GET,POST'/>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		User u = new User("", ""); // no conditions are specified so any user object can be used.

		String uri = "http://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isUnenforced(uri), "due to wrong scheme should NOT be unenforced " + uri);

		uri = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isUnenforced(uri), "should be unenforced " + uri);

		uri = "http://labs-local.lds.org/auth/_app/debug";
		Assert.assertFalse(tman.isPermitted("POST", uri, u), "due to wrong scheme should NOT be allowed " + uri);

		uri = "app://labs-local.lds.org/auth/_app/debug";
		Assert.assertTrue(tman.isPermitted("POST", uri, u), "should be allowed " + uri);
	}

	@Test
	public void testConditionIsBishopAllowed() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<?alias is-bishop=classpath:is-bishop-test.xml?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-resource uri='app://labs-local.lds.org/auth/_app/*' allow='GET,POST' condition='{{is-bishop}}'/>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		User bish = new User("bish", "bish"); 
		bish.addHeader(LegacyPropsInjectorDefs.CP_POSITIONS_SESSION_PROPERTY, "P4:W100S200A300"); // 4 is a bishop
		User user = new User("user", "user"); 

		String uri = "app://labs-local.lds.org/auth/_app/debug";
		Assert.assertTrue(tman.isPermitted("POST", uri, bish), "should be allowed " + uri);
		Assert.assertFalse(tman.isPermitted("POST", uri, user), "should NOT be allowed " + uri);
	}

	@Test
	public void testConditionHasSpecificLdsAccountIdAllowed() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<?alias has-lds-account-1234=classpath:has-lds-account-1234-test.xml?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-resource uri='app://labs-local.lds.org/auth/_app/*' allow='GET,POST' condition='{{has-lds-account-1234}}'/>"
			+ " </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		User u1234 = new User("bish", "bish"); 
		u1234.addHeader(LegacyPropsInjectorDefs.CP_LDS_ACCOUNT_ID_PROPERTY, "1234");
		User user = new User("user", "user"); 
		user.addHeader(LegacyPropsInjectorDefs.CP_LDS_ACCOUNT_ID_PROPERTY, "1000");

		String uri = "app://labs-local.lds.org/auth/_app/debug";
		Assert.assertTrue(tman.isPermitted("POST", uri, u1234), "should be allowed " + uri);
		Assert.assertFalse(tman.isPermitted("POST", uri, user), "should NOT be allowed " + uri);
	}
}
