package org.lds.sso.appwrap;

import org.lds.sso.appwrap.XmlConfigLoader2.CfgContentHandler;
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
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
			+ "<config console-port='{{rest-port}}'/>";
		Config cfg = new Config();
		cfg.setConsolePort(0);
		XmlConfigLoader2.load(xml);
		Assert.assertEquals(cfg.getConsolePort(), 81, "console port should have been changed to 81.");
	}
	
	@Test
	public void testConfigAgent() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<?alias site=labs-local.lds.org?>"
			+ "<config console-port='88'>"
			+ " <agent port='80'>"
			+ "  <cookie name='lds-policy' domain='.lds.org'/>"
			+ "  <sign-in-url value='http://{{site}}/auth/ui/sign-in'/>"
			+ " </agent>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		Assert.assertEquals(cfg.getProxyPort(), 80);
		Assert.assertEquals(cfg.getCookieName(), "lds-policy");
		Assert.assertEquals(cfg.getCookieDomain(), ".lds.org");
		Assert.assertEquals(cfg.getLoginPage(), "http://labs-local.lds.org/auth/ui/sign-in");
	}

	@Test
	public void testConfigSite() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<?alias rest-port=99?>"
			+ "<?alias site=labs-local.lds.org?>"
			+ "<config console-port='88'>"
		    + " <site host='{{site}}'>"
	        + "  <header name='policy-service-url' value='http://{{site}}:{{rest-port}}/rest/'/>"
	        + "  <app cctx='/auth' ctx='/auth' port='8411'/>"
	        + "  <unenforced url='/auth/ui/*'/>"
	        + " </site>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager appMgr = cfg.getTrafficManager();
		Assert.assertNotNull(appMgr.getSite("labs-local.lds.org"), "site not set");
		StringBuffer sb = new StringBuffer();
		cfg.injectGlobalHeaders(sb);
		Assert.assertTrue(sb.toString().contains("policy-service-url: http://labs-local.lds.org:99/rest/"), "policy-service-url header not set");
		cfg.getAppManager().
		TBC
		Assert.assertEquals(cfg.getCookieDomain(), ".lds.org");
		Assert.assertEquals(cfg.getLoginPage(), "http://labs-local.lds.org/auth/ui/sign-in");
	}
}
