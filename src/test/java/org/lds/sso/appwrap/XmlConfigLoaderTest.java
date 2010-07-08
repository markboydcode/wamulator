package org.lds.sso.appwrap;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import org.lds.sso.appwrap.XmlConfigLoader2.CfgContentHandler; 
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.testng.Assert;
import org.testng.annotations.Test;

public class XmlConfigLoaderTest {
    
    public void testUserAttributes () throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias site=labs-local.lds.org?>"
            + "<config console-port='88' proxy-port='45'>"
            + "  <users>"
            + "    <user name='nnn' pwd='pwd'>"
            + "      <sso-header name='header-a' value='aaa'/>" 
            + "      <ldsApplication value='111'/>" 
            + "      <ldsApplication value='222'/>" 
            + "    </user>"
            + "  </users>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        User u = cfg.getUserManager().getUser("nnn");
        Assert.assertNotNull(u);
        NvPair[] atts = u.getAttributes();
        Assert.assertEquals(atts.length, 2);
        Assert.assertEquals(atts[0].getName(), User.LDSAPPS_ATT);
        Assert.assertEquals(atts[0].getValue(), "111");
        Assert.assertEquals(atts[1].getName(), User.LDSAPPS_ATT);
        Assert.assertEquals(atts[1].getValue(), "222");
    }
    
	@Test
	public void testResolveAliases() {
		CfgContentHandler hndlr = new CfgContentHandler();
        Map<String,String> aliases 
            = (Map<String, String>) XmlConfigLoader2.parsingContextAccessor.get()
                .get(XmlConfigLoader2.PARSING_ALIASES);

		aliases.put("aaa1", "vvv1");
		aliases.put("bbb", "www");
		aliases.put("ccc", "xxx");
		Assert.assertEquals(XmlConfigLoader2.resolveAliases("{{aaa1}}"), "vvv1");
		Assert.assertEquals(XmlConfigLoader2.resolveAliases("{{aaa1}}-"), "vvv1-");
		Assert.assertEquals(XmlConfigLoader2.resolveAliases("-{{aaa1}}"), "-vvv1");
		Assert.assertEquals(XmlConfigLoader2.resolveAliases("{{aaa1}}{{aaa1}}"), "vvv1vvv1");
		Assert.assertEquals(XmlConfigLoader2.resolveAliases("this {{aaa1}} that"), "this vvv1 that");
		Assert.assertEquals(XmlConfigLoader2.resolveAliases("this {{ccc}}{{aaa1}} that{{bbb}}"), "this xxxvvv1 thatwww");
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testUnmatchedAlias() {
		CfgContentHandler hndlr = new CfgContentHandler();
		XmlConfigLoader2.resolveAliases("---{{---");
	}

	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testMissingAlias() {
		CfgContentHandler hndlr = new CfgContentHandler();
		XmlConfigLoader2.resolveAliases("---{{jjj}}---");
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
    public void testLoadTextAliasFromSystem() throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias site=system:some-path-property?>"
            + "<config console-port='88' proxy-port='45'>"
            + "  <sso-sign-in-url value='http://{{site}}/auth/ui/sign-in'/>"
            + "</config>";
        System.setProperty("some-path-property", "the-path-property");
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        Assert.assertEquals(cfg.getLoginPage(), "http://the-path-property/auth/ui/sign-in");
    }

    @Test
    public void testOrderingOfElementsWithNestedURLs() throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-traffic>"
            + "  <by-site host='labs-local.lds.org' port='80'>"
            + "    <allow action='GET' cpath='/auth/ui/some/path/*'/>"
            + "    <unenforced cpath='/auth/ui/*'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        TrafficManager tman = cfg.getTrafficManager();
        
        String url = "http://labs-local.lds.org/auth/ui/some/path/lower";
        Assert.assertFalse(tman.isUnenforced(url), "should NOT be unenforced " + url);
        
        url = "http://labs-local.lds.org:80/auth/ui/some/path/lower";
        Assert.assertFalse(tman.isUnenforced(url), "should be NOT unenforced " + url);
        
        url = "http://labs-local.lds.org:111/auth/ui/some/path/lower";
        Assert.assertFalse(tman.isUnenforced(url), 
        "due to wrong port should NOT be unenforced " + url);
        
        url = "apps://labs-local.lds.org/auth/ui/some/path/lower";
        Assert.assertFalse(tman.isUnenforced(url), 
        "due to wrong scheme should NOT be unenforced " + url);
        
        url = "http://labs-local.lds.org:80/auth/ui/some/path/lower?locale=eng";
        Assert.assertFalse(tman.isUnenforced(url), 
        "due to query should NOT be unenforced " + url);
        
        url = "http://labs-local.lds.org/auth/diff/path";
        Assert.assertFalse(tman.isUnenforced(url), 
        "due to incorrect path should NOT be unenforced " + url);

        
        url = "http://labs-local.lds.org/auth/ui/some/path/lower";
        Assert.assertTrue(tman.isEnforced(url), "should be enforced " + url);
        
        url = "http://labs-local.lds.org:80/auth/ui/some/path/lower";
        Assert.assertTrue(tman.isEnforced(url), "should be enforced " + url);
        
        url = "http://labs-local.lds.org:111/auth/ui/some/path/lower";
        Assert.assertFalse(tman.isEnforced(url), 
        "due to wrong port should NOT be enforced " + url);
        
        url = "apps://labs-local.lds.org/auth/ui/some/path/lower";
        Assert.assertFalse(tman.isEnforced(url), 
        "due to wrong scheme should NOT be enforced " + url);
        
        url = "http://labs-local.lds.org:80/auth/ui/some/path/lower?locale=eng";
        Assert.assertFalse(tman.isEnforced(url), 
        "due to query should NOT be enforced " + url);
        
        url = "http://labs-local.lds.org/auth/diff/path";
        Assert.assertFalse(tman.isEnforced(url), 
        "due to incorrect path should NOT be enforced " + url);

        
        url = "http://labs-local.lds.org/auth/ui/sign-in";
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
    public void testIncorrectOrderingOfElementsWithNestedURLsThrowExcp() throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-traffic>"
            + "  <by-site host='labs-local.lds.org' port='80'>"
            + "    <unenforced cpath='/auth/ui/*'/>"
            + "    <allow action='GET' cpath='/auth/ui/some/path/*'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        try {
            XmlConfigLoader2.load(xml);
            Assert.fail("Should have thrown IllegalArgumentException since URLs " +
            		"matching the cpath of the 'allow' element will be consumed by the cpath of the " +
            		"preceeding 'unenforced' element.");
        }
        catch (Exception e) {
            Assert.assertNotNull(e.getCause(), "Should be an underlying exception.");
            Assert.assertSame(e.getCause().getClass(), IllegalArgumentException.class);
        }
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
	public void testArcaneSchemeConfigIsAllowed() throws Exception {
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
    public void test_FileAlias() throws Exception {
        String path = "file-has-lds-app-1234-test.xml";
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        FileWriter writer = new FileWriter(path);
        writer.write("<HasLdsApplication value='1234'/>\n");
        writer.flush();
        writer.close();
        
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias has-lds-app-1234=file:file-has-lds-app-1234-test.xml?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-traffic>"
            + "  <by-site host='labs-local.lds.org' port='45' scheme='http'>"
            + "   <allow cpath='/auth/_app/*' action='GET,POST' condition='{{has-lds-app-1234}}'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        TrafficManager tman = cfg.getTrafficManager();
        User u1234 = new User("bish", "bish"); 
        u1234.addAttribute(User.LDSAPPS_ATT, "1234");
        User user = new User("user", "user"); 
        user.addAttribute(User.LDSAPPS_ATT, "1000");

        String uri = "http://labs-local.lds.org:45/auth/_app/debug";
        Assert.assertTrue(tman.isPermitted("POST", uri, u1234), "should be allowed " + uri);
        Assert.assertFalse(tman.isPermitted("POST", uri, user), "should NOT be allowed " + uri);
    }

    @Test
    public void test_leadingTrailingLWSPTrimedFromAliasNameAndValue() throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias embedded wsp=embedded?>"
            + "<?alias    nameWSP   =nameWSP?>"
            + "<?alias valueWSP=    valueWSP    ?>"
            + "<?alias vembedded=v embedded?>"
            + "<undefined/>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        Map<String,String>vals = (Map<String, String>) XmlConfigLoader2.parsingContextAccessor.get().get(XmlConfigLoader2.PARSING_SYNTAXES);
        Assert.assertEquals(vals.get("embedded wsp"), "embedded");
        Assert.assertEquals(vals.get("nameWSP"), "nameWSP");
        Assert.assertEquals(vals.get("valueWSP"), "valueWSP");
        Assert.assertEquals(vals.get("vembedded"), "v embedded");
    }

    @Test
    public void test_conditionMustNotBePlainText() throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias somealias=plain-text?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-traffic>"
            + "  <by-site host='labs-local.lds.org' port='45' scheme='http'>"
            + "   <allow cpath='/auth/_app/*' action='GET,POST' condition='{{somealias}}'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        try {
            XmlConfigLoader2.load(xml);
            Assert.fail("Exception should have been thrown since condition " +
                    "content is plain text which won't work since it is " +
                    "XML burried within a processing instruction.");
        }
        catch(Exception e) {
            Throwable c = e.getCause();
            if (c.getClass() != IllegalArgumentException.class) {
                Assert.fail(c.getClass().getName() + " was thrown. Should " 
                        + "have been IllegalArgumentException.");
            }
        }
    }

    @Test
    public void test_conditionMustMatchDeclaredAlias() throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-traffic>"
            + "  <by-site host='labs-local.lds.org' port='45' scheme='http'>"
            + "   <allow cpath='/auth/_app/*' action='GET,POST' condition='{{somealias}}'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        try {
            XmlConfigLoader2.load(xml);
            Assert.fail("Exception should have been thrown since condition " +
                    "references non-existent alias.");
        }
        catch(Exception e) {
            Throwable c = e.getCause();
            if (c.getClass() != IllegalArgumentException.class) {
                Assert.fail(c.getClass().getName() + " was thrown. Should " 
                        + "have been IllegalArgumentException.");
            }
        }
    }

    @Test
    public void test_conditionValuesInParseContainers() throws Exception {
        
        System.setProperty("condition-syntax", "<HasLdsApplication value='123'/>");
        
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias somealias=system:condition-syntax?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-traffic>"
            + "  <by-site host='labs-local.lds.org' port='45' scheme='http'>"
            + "   <allow cpath='/auth/_app/*' action='GET,POST' condition='{{somealias}}'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        Map<String, String> conSyntaxes = (Map<String, String>) XmlConfigLoader2.parsingContextAccessor
                .get().get(XmlConfigLoader2.PARSING_ALIASES);
        Assert.assertNotNull(conSyntaxes.get("somealias"));
        Assert.assertEquals(conSyntaxes.get("somealias"), "<HasLdsApplication value='123'/>");
        Map<String, String> aliasValues = (Map<String, String>) XmlConfigLoader2.parsingContextAccessor
                .get().get(XmlConfigLoader2.PARSING_SYNTAXES);
        Assert.assertNotNull(aliasValues.get("somealias"));
        Assert.assertEquals(aliasValues.get("somealias"), "system:condition-syntax");
        
    }

    @Test(expectedExceptions=Exception.class)
    public void test_FileAlias_FileNotFound() throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias has-lds-account-1234=file:bogus-file-name.xml?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-traffic>"
            + "  <by-resource uri='app://labs-local.lds.org/auth/_app/*' allow='GET,POST' condition='{{has-lds-account-1234}}'/>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        TrafficManager tman = cfg.getTrafficManager();
        User u1234 = new User("bish", "bish"); 
        u1234.addHeader(UserHeaderNames.LDS_ACCOUNT_ID, "1234");
        User user = new User("user", "user"); 
        user.addHeader(UserHeaderNames.LDS_ACCOUNT_ID, "1000");

        String uri = "app://labs-local.lds.org/auth/_app/debug";
        boolean u12134Answer = tman.isPermitted("POST", uri, u1234); 
        Assert.fail("should have thrown exception");
    }
}
