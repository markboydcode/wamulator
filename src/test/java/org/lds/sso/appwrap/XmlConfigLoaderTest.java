package org.lds.sso.appwrap;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.lds.sso.appwrap.AppEndPoint.Scheme;
import org.lds.sso.appwrap.XmlConfigLoader2.CfgContentHandler;
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.identity.ExternalUserSource.ConfigurationException;
import org.lds.sso.appwrap.identity.User;
import org.lds.sso.appwrap.xml.Alias;
import org.lds.sso.appwrap.xml.AliasHolder;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

public class XmlConfigLoaderTest {
    
    //@Test
    public void test_loading_of_profile_headers () throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias site=labs-local.lds.org?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-cookie name='lds-policy' domain='.lds.org'/>"
            + " <sso-traffic>"
            + "  <by-site scheme='http' host='www.lds.org' port='45'>"
            + "   <cctx-mapping thost='127.0.0.1' tport='1000'>"
            + "    <headers>"
            + "     <profile-att name='prof-1' attribute='att-1'/>"
            + "     <profile-att name='prof-2' attribute='att-2'/>"
            + "    </headers>"
            + "   </cctx-mapping>"
            + "  </by-site>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        SiteMatcher site = cfg.getTrafficManager().getSite(Scheme.HTTP, "www.lds.org", 45);
        EndPoint ep = site.getEndpointForCanonicalUrl("/some/path");
        Assert.assertTrue(ep instanceof AppEndPoint, "should be instance of AppEndPoint");
        AppEndPoint app = (AppEndPoint) ep;
        Map<String, String> prof = app.profileHeaders;
        
        Assert.assertNotNull(prof.get("prof-1"), "should have found profile header of 'prof-1'");
        Assert.assertEquals(prof.get("prof-1"), "att-1", "should have found profile header value 'att-1' for 'prof-1'");
        
        Assert.assertNotNull(prof.get("prof-2"), "should have found profile header of 'prof-2'");
        Assert.assertEquals(prof.get("prof-2"), "att-2", "should have found profile header value 'att-2' for 'prof-2'");
    }
    
    //@Test
    public void test_loading_of_fixed_headers () throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias site=labs-local.lds.org?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-cookie name='lds-policy' domain='.lds.org'/>"
            + " <sso-traffic>"
            + "  <by-site scheme='http' host='www.lds.org' port='45'>"
            + "   <cctx-mapping thost='127.0.0.1' tport='1000'>"
            + "    <headers>"
            + "     <fixed-value name='single' value='single-1'/>"
            + "     <fixed-value name='multi' value='multi-1'/>"
            + "     <fixed-value name='multi' value='multi-2'/>"
            + "    </headers>"
            + "   </cctx-mapping>"
            + "  </by-site>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        SiteMatcher site = cfg.getTrafficManager().getSite(Scheme.HTTP, "www.lds.org", 45);
        EndPoint ep = site.getEndpointForCanonicalUrl("/some/path");
        Assert.assertTrue(ep instanceof AppEndPoint, "should be instance of AppEndPoint");
        AppEndPoint app = (AppEndPoint) ep;
        Map<String, List<String>> fixed = app.fixedHeaders;
        
        Assert.assertNotNull(fixed.get("single"), "should have found fixed header of 'single'");
        Assert.assertEquals(fixed.get("single").size(), 1, "should have found one fixed header value for 'single'");
        Assert.assertEquals(fixed.get("single").get(0), "single-1", "should have found one fixed header value of 'single-1' for 'single'");
        
        Assert.assertNotNull(fixed.get("multi"), "should have found fixed header of 'multi'");
        Assert.assertEquals(fixed.get("multi").size(), 2, "should have found two fixed header values for 'multi'");
        Assert.assertEquals(fixed.get("multi").get(0), "multi-1", "should have found fixed header value of 'multi-1' for 'multi' at position 0");
        Assert.assertEquals(fixed.get("multi").get(1), "multi-2", "should have found fixed header value of 'multi-2' for 'multi' at position 1");
    }
    
    @Test
    public void test_loading_of_attributes () throws Exception {
    	System.getProperties().remove("non-existent-sys-prop");
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias site=labs-local.lds.org?>"
            + "<?system-alias xml-usr-src-cfg=non-existent-sys-prop default="
            + "\"xml="
            + " <users>"
            + "  <unique-attribute name='ldsaccountid'/>"
            + "   <user name='nnn' pwd='pwd'>"
            + "    <att name='ldsaccountid' value='20'/>"
            + "   </user>"
            + "   <user name='mmm' pwd='pwd'>"
            + "    <att name='ldsaccountid' value='33'/>"
            + "   </user>"
            + " </users>"
            + "\"?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-cookie name='lds-policy' domain='.lds.org'/>"
            + " <sso-traffic>"
            + "  <by-site scheme='http' host='www.lds.org' port='45'/>"
            + " </sso-traffic>"
            + " <user-source type='xml'>{{xml-usr-src-cfg}}</user-source>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        Assert.assertEquals(cfg.getUserManager().getUsers().size(), 2, "two users should have been loaded");
        Assert.assertNotNull(cfg.getUserManager().getUser("nnn"), "user 'nnn' should have been loaded");
        Assert.assertNotNull(cfg.getUserManager().getUser("nnn").getAttributes(), "at least one attribute should have been loaded");
        Assert.assertEquals(cfg.getUserManager().getUser("nnn").getAttributes().length, 2, "at least one attribute in addition to automatic cn attribute should have been loaded");
        Assert.assertNotNull(cfg.getUserManager().getUser("nnn").getAttribute("ldsaccountid"), "ldsaccountid attribute should have been loaded");
        String[] ids = cfg.getUserManager().getUser("nnn").getAttribute("ldsaccountid");
        Assert.assertEquals(ids.length, 1, "only one ldsaccountid attribute should have been loaded for user");
        Assert.assertEquals(ids[0], "20", "ldsaccountid of 20 should have been loaded");
        Assert.assertNotNull(cfg.getUserManager().getUser("mmm"), "user 'mmm' should have been loaded");
        Assert.assertNotNull(cfg.getUserManager().getUser("mmm").getAttributes(), "at least one attribute should have been loaded");
        Assert.assertEquals(cfg.getUserManager().getUser("mmm").getAttributes().length, 2, "at least one attribute in addition to automatic cn attribute should have been loaded");
        Assert.assertNotNull(cfg.getUserManager().getUser("mmm").getAttribute("ldsaccountid"), "ldsaccountid attribute should have been loaded");
        String[] ids2 = cfg.getUserManager().getUser("mmm").getAttribute("ldsaccountid");
        Assert.assertEquals(ids2.length, 1, "only one ldsaccountid attribute should have been loaded for user");
        Assert.assertEquals(ids2[0], "33", "ldsaccountid of 33 should have been loaded");
    }
    
    @Test
    public void test_unique_attribute_enforcement () throws Exception {
    	System.getProperties().remove("non-existent-sys-prop");
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias site=labs-local.lds.org?>"
            + "<?system-alias xml-usr-src-cfg=non-existent-sys-prop default="
            + "\"xml="
            + " <users>"
            + "  <unique-attribute name='ldsaccountid'/>"
            + "   <user name='nnn' pwd='pwd'>"
            + "    <att name='ldsaccountid' value='20'/>"
            + "   </user>"
            + "   <user name='mmm' pwd='pwd'>"
            + "    <att name='ldsaccountid' value='33'/>"
            + "   </user>"
            + "   <user name='ooo' pwd='pwd'>"
            + "    <att name='ldsaccountid' value='20'/>"
            + "   </user>"
            + " </users>"
            + " \r\n"
            + " enforce-uniqueness=ldsaccountid"
            + " \r\n"
            + "\"?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-cookie name='lds-policy' domain='.lds.org'/>"
            + " <sso-traffic>"
            + "  <by-site scheme='http' host='www.lds.org' port='45'/>"
            + " </sso-traffic>"
            + " <user-source type='xml'>{{xml-usr-src-cfg}}</user-source>"
            + "</config>";
        Config cfg = new Config();
        try {
            XmlConfigLoader2.load(xml);
        }
        catch(Exception e) {
            Throwable t = e.getCause();
            if (!(t instanceof SAXException)) {
                Assert.fail("Threw " + t.getClass().getName() + " but expected SAXException");
            }
            t=t.getCause();
            if (!(t instanceof ConfigurationException)) {
                Assert.fail("Threw " + t.getClass().getName() + " but expected ConfigurationException");
            }
            t=t.getCause();
            if (!(t instanceof IllegalArgumentException)) {
                Assert.fail("Threw " + t.getClass().getName() + " but expected IllegalArgumentException");
            }
            String msg = t.getMessage();
            if (msg == null) {
            	Assert.fail("Exception message should not be null.");
            }
            if (! msg.contains("Uniqueness Constraint")) {
            	Assert.fail("Exception message should have contained the words, 'Uniqueness Constraint'.");
            	
            }
            return;
        }
        Assert.fail("IllegalArgumentException should have been thrown since ldsaccountid was declared unique and a duplicate value was incurred.");
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void test_LackOfSiteInMasterDomain () throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias site=labs-local.lds.org?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-cookie name='lds-policy' domain='.lds.org'/>"
            + " <sso-traffic>"
            + "  <by-site scheme='http' host='josephsmith.net' port='45'/>"
            + "  <by-site scheme='http' host='mormon.org' port='45'/>"
            + "  <by-site scheme='http' host='ldschurch.org' port='45'/>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        try {
            XmlConfigLoader2.load(xml);
        }
        catch(Exception e) {
            Throwable t = e.getCause();
            if (!(t instanceof IllegalStateException)) {
                Assert.fail("Threw " + t.getClass().getName() + " but expected IllegalStateException");
            }
            return;
        }
        Assert.fail("IllegalStateException should have been thrown since no by-site was defined in the master cookie domain.");
    }
    
    @Test
    public void test_localhost_Domain () throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias site=labs-local.lds.org?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-cookie name='lds-policy' domain='localhost'/>"
            + " <sso-traffic>"
            + "  <by-site scheme='http' host='localhost' port='45'/>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        try {
            XmlConfigLoader2.load(xml);
        }
        catch(Exception e) {
            Throwable t = e.getCause();
            Assert.fail("Shouldn't have thrown an exception", t);
        }
    }
    
    //@SuppressWarnings("unchecked")
    //@Test
    public void testEmbeddedConditions () throws Exception {
    	// make sure system prop is empty, then define sys alias with default 
    	System.getProperties().remove("non-existent-sys-prop");
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias site=labs-local.lds.org?>"
            + "<?system-alias xml-user-source-props=non-existent-sys-prop default=" 
            + "\"xml=" 
            + "  <users>"
            + "    <user name='nnn' pwd='pwd'>"
            + "    </user>"
            + "  </users>"
            + "\"?>"
            + "<config console-port='88' proxy-port='45'>"
            + "  <conditions>"
            + "   <condition alias='o&apos;hare prj'>"
            + "    <OR site='{{site}}'>" + ((char)10) + ((char)9) + ((char)13)
            + "     <Attribute name='acctid' operation='equals' value='o&apos;hare prj'/>"
            + "<!-- comment gets dropped -->"
            + "     <Attribute name='acctid' operation='equals' value='o&quot;hare prj'/>"
            + "    </OR>"
            + "   </condition>"
            + "  </conditions>"
            + " <sso-traffic>"
            + "  <by-site scheme='http' host='local.lds.org' port='45'>"
            + "   <cctx-mapping thost='127.0.0.1' tport='1000' />"
            + "   <allow action='GET' cpath='/conditional/*' condition='{{o&apos;hare prj}}'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + " <user-source type='xml'>{{xml-user-source-props}}</user-source>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        AliasHolder aliases = XmlConfigLoader2.get(XmlConfigLoader2.PARSING_ALIASES);
        Assert.assertTrue(aliases.containsAlias("o'hare prj"), "should have alias key of o'hare");
        // note that white space is preserved in syntax.
        // also note that carriage returns are normalized to line feeds
        Assert.assertEquals(aliases.getAliasValue("o'hare prj"), "    <OR site='labs-local.lds.org'>\n\t\n     <Attribute name='acctid' operation='equals' value='o&apos;hare prj'></Attribute>     <Attribute name='acctid' operation='equals' value='o&quot;hare prj'></Attribute>    </OR>   ");
        SiteMatcher site = cfg.getTrafficManager().getSite(Scheme.HTTP, "local.lds.org", 45);
        OrderedUri uri = site.getUriMatcher(Scheme.HTTP, "local.lds.org", 45, "/conditional/test", null);
        Assert.assertTrue(uri instanceof AllowedUri, "URI should have been instance of AllowedUri but was " + uri.getClass().getSimpleName());
        AllowedUri au = (AllowedUri) uri;
        String condId = site.conditionsMap.get(au);
        Assert.assertNotNull(condId, "URI should have been assigned condition");
        Assert.assertEquals(condId, "o'hare prj");
    }
    
    @Test
    public void test_default_signin_page () throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-traffic>"
            + "  <by-site scheme='http' host='local.lds.org' port='45'/>"
            + "  <by-site scheme='http' host='another.place.net' port='45'/>"
            + "  <by-site scheme='http' host='other.host.com' port='45'/>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        Assert.assertEquals(cfg.getLoginPage(), "http://local.lds.org:88/admin/selectUser.jsp");
    }
    
    @Test
    public void test_default_signin_page_w_port_auto_bind () throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='auto' proxy-port='45'>"
            + " <sso-traffic>"
            + "  <by-site scheme='http' host='local.lds.org' port='45'/>"
            + "  <by-site scheme='http' host='another.place.net' port='45'/>"
            + "  <by-site scheme='http' host='other.host.com' port='45'/>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        Assert.assertEquals(cfg.getLoginPage(), "http://local.lds.org:" 
                + cfg.getConsolePort() + "/admin/selectUser.jsp");
    }
    
    @Test
    public void test_proxy_tls_loaded () throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <proxy-tls https-port='444' cert-host='*.my.host'/>"
            + " <sso-traffic>"
            + "  <by-site scheme='http' host='local.lds.org' port='45'/>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        Assert.assertEquals(cfg.getProxyHttpsPort(), 444);
        Assert.assertEquals(cfg.getProxyHttpsCertHost(), "*.my.host");
    }
    
    @Test
    public void test_ex_if_no_bysite () throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='88' proxy-port='45'>"
            + "</config>";
        Config cfg = new Config();
        try {
            XmlConfigLoader2.load(xml);
        }
        catch(Exception e) {
            Throwable t = e.getCause();
            if (!(t instanceof IllegalStateException)) {
                Assert.fail("Threw " + t.getClass().getName() + " but expected IllegalStateException");
            }
            return;
        }
        Assert.fail("IllegalStateException should have been thrown since no by-site was defined.");
    }

    @Test
    public void test_signin_page_ex_w_wrong_cookie_domain() throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='88' proxy-port='45'>"
            + "  <sso-cookie name='lds-policy' domain='.host.net'/>"
            + "  <sso-sign-in-url value='http://my.site.com/auth/ui/sign-in'/>"
            + "  <sso-traffic>"
            + "   <by-site host='local.host.net' port='45'/>"
            + "  </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        try {
            cfg.getLoginPage();
        }
        catch(IllegalArgumentException e) {
            return; // should throw it
        }
        Assert.fail("Should have thrown IllegalArgumentException since cookie domain was set to '.host.net' but no by-site was configured in that domain.");
    }

    //@Test
    public void testUserAttributes () throws Exception {
    	System.setProperty("config.props", "xml="
                + "  <users>"
                + "    <user name='nnn' pwd='pwd'>"
                + "      <att name='ldsApplications' value='111'/>" 
                + "      <att name='ldsApplications' value='222'/>" 
                + "    </user>"
                + "  </users>");
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias site=labs-local.lds.org?>"
            + "<?system-alias xml-source-props=config.props?>"		
            + "<config console-port='88' proxy-port='45'>"
            + "  <sso-cookie name='lds-policy' domain='.host.net'/>"
            + "  <sso-traffic>"
            + "   <by-site host='local.host.net' port='45'/>"
            + "  </sso-traffic>"
            + "  <user-source type='xml'>{{xml-source-props}}</user-source>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        User u = cfg.getUserManager().getUser("nnn");
        Assert.assertNotNull(u);
        NvPair[] atts = u.getAttributes();
        // 3 is correct since 'cn' attribute is added automatically
        Assert.assertEquals(atts.length, 3);
        Assert.assertEquals(atts[0].getName(), "cn");
        Assert.assertEquals(atts[0].getValue(), "nnn");
        Assert.assertEquals(atts[1].getName(), "ldsApplications");
        Assert.assertEquals(atts[1].getValue(), "111");
        Assert.assertEquals(atts[2].getName(), "ldsApplications");
        Assert.assertEquals(atts[2].getValue(), "222");
    }
    
	@Test
	public void testResolveAliases() {
		CfgContentHandler hndlr = new CfgContentHandler();
        AliasHolder aliases 
            = XmlConfigLoader2.get(XmlConfigLoader2.PARSING_ALIASES);

		aliases.addAlias(new Alias("aaa1", "vvv1"));
		aliases.addAlias(new Alias("bbb", "www"));
		aliases.addAlias(new Alias("ccc", "xxx"));
		Assert.assertEquals(Alias.resolveAliases("{{aaa1}}"), "vvv1");
		Assert.assertEquals(Alias.resolveAliases("{{aaa1}}-"), "vvv1-");
		Assert.assertEquals(Alias.resolveAliases("-{{aaa1}}"), "-vvv1");
		Assert.assertEquals(Alias.resolveAliases("{{aaa1}}{{aaa1}}"), "vvv1vvv1");
		Assert.assertEquals(Alias.resolveAliases("this {{aaa1}} that"), "this vvv1 that");
		Assert.assertEquals(Alias.resolveAliases("this {{ccc}}{{aaa1}} that{{bbb}}"), "this xxxvvv1 thatwww");
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testUnmatchedAlias() {
		CfgContentHandler hndlr = new CfgContentHandler();
		Alias.resolveAliases("---{{---");
	}

	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testMissingAlias() {
		CfgContentHandler hndlr = new CfgContentHandler();
		Alias.resolveAliases("---{{jjj}}---");
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
			+ "<config console-port='{{rest-port}}' proxy-port='45'>"
            + "  <sso-cookie name='lds-policy' domain='.host.net'/>"
            + "  <sso-traffic>"
            + "   <by-site host='local.host.net' port='45'/>"
            + "  </sso-traffic>"
            + "</config>"
			;
		Config cfg = new Config();
		cfg.setConsolePort(0);
		XmlConfigLoader2.load(xml);
		Assert.assertEquals(cfg.getConsolePort(), 81, "console port should have been changed to 81.");
		Assert.assertEquals(cfg.getProxyPort(), 45, "proxy port should have been changed to 45.");
	}

	@Test
	public void testSchemaDefault() throws Exception {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='81' proxy-port='45' "
			+ "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
			+ "xmlns='http://code.lds.org/schema/wamulator' "
			+ "xsi:schemaLocation='http://code.lds.org/schema/wamulator http://code.lds.org/schema/wamulator/wamulator-5.0.xsd'>"
            + "  <sso-cookie domain='.host.net'/>"
            + "  <sso-traffic>"
            + "   <by-site host='local.host.net' port='45'/>"
            + "  </sso-traffic>"
            + "</config>"
			;
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		Assert.assertEquals(cfg.getCookieName(), "obssoCookie", "Cookie name should match the default in the wamulator-5.0.xsd");
	}
	
	@Test(expectedExceptions=Exception.class)
	public void testCookieNameMissingNoSchemaFail() throws Exception {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='81' proxy-port='45'>"
            + "  <sso-cookie domain='.host.net'/>"
            + "  <sso-traffic>"
            + "   <by-site host='local.host.net' port='45'/>"
            + "  </sso-traffic>"
            + "</config>"
			;
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
	}

	@Test(expectedExceptions=Exception.class)
	public void testSchemaValidation() throws Exception {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='81' proxy-port='45' unknownAttr='dude' "
			+ "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
			+ "xmlns='http://code.lds.org/schema/wamulator' "
			+ "xsi:schemaLocation='http://code.lds.org/schema/wamulator http://code.lds.org/schema/wamulator/wamulator-5.0.xsd'>"
            + "  <sso-cookie name='cookieName' domain='.host.net'/>"
            + "  <sso-traffic>"
            + "   <by-site host='local.host.net' port='45'/>"
            + "  </sso-traffic>"
            + "</config>"
			;
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
	}
	
	@Test
	public void testConfigAgent() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<?alias site=labs-local.lds.org?>"
			+ "<config console-port='88' proxy-port='45'>"
		    + "  <sso-cookie name='lds-policy' domain='.lds.org'/>"
			+ "  <sso-sign-in-url value='http://{{site}}/auth/ui/sign-in'/>"
            + "  <sso-traffic>"
            + "   <by-site host='local.lds.org' port='45'/>"
            + "  </sso-traffic>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		Assert.assertEquals(cfg.getProxyPort(), 45);
		Assert.assertEquals(cfg.getCookieName(), "lds-policy");
		Assert.assertEquals(cfg.getSessionManager().getMasterCookieDomain(), ".lds.org");
		Assert.assertEquals(cfg.getLoginPage(), "http://labs-local.lds.org/auth/ui/sign-in");
	}

    @Test
    public void testLoadTextAliasFromClasspath() throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?classpath-alias site=XmlConfigLoaderTest.txt?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-sign-in-url value='http://{{site}}/auth/ui/sign-in'/>"
            + " <sso-traffic>"
            + "  <by-site host='{{site}}' port='80'/>"
            + " </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        Assert.assertEquals(cfg.getLoginPage(), "http://test-host-domain.lds.org/auth/ui/sign-in");
    }

    @Test
    public void testLoadTextAliasFromSystem() throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?system-alias site=some-path-property?>"
            + "<?system-alias other-site=some-other-path-property default=someother.host.net?>"
            + "<?system-alias other-other-site=some-other-other-path-property default={{other-site}}?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-cookie name='lds-policy' domain='.host.net'/>"
            + " <sso-sign-in-url value='http://{{site}}/auth/ui/sign-in'/>"
            + " <sso-traffic>"
            + "  <by-site host='{{site}}' port='80'/>"
            + " </sso-traffic>"
            + "</config>";
        System.setProperty("some-path-property", "some.host.net");
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        Assert.assertEquals(cfg.getLoginPage(), "http://some.host.net/auth/ui/sign-in");
        Assert.assertEquals(XmlConfigLoader2.get(XmlConfigLoader2.PARSING_ALIASES).getAliasValue("other-site"), "someother.host.net");
        Assert.assertEquals(XmlConfigLoader2.get(XmlConfigLoader2.PARSING_ALIASES).getAliasValue("other-other-site"), "someother.host.net");
    }

    //@Test
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
        
        url = "https://labs-local.lds.org/auth/ui/some/path/lower";
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
        
        url = "https://labs-local.lds.org/auth/ui/sign-in";
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


    //@Test
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
        
        url = "https://labs-local.lds.org/auth/ui/sign-in";
        Assert.assertFalse(tman.isUnenforced(url), 
        "due to wrong scheme should NOT be unenforced " + url);
        
        url = "http://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
        Assert.assertFalse(tman.isUnenforced(url), 
        "due to query should NOT be unenforced " + url);
        
        url = "http://labs-local.lds.org/auth/diff/path";
        Assert.assertFalse(tman.isUnenforced(url), 
        "due to incorrect path should NOT be unenforced " + url);
    }


	//@Test
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
		URL filePath = XmlConfigLoaderTest.class.getClassLoader().getResource("XmlConfigDefaultRuleTest.xml");
		String xml = 
			"<?file-alias policy-src-xml=\"" + filePath.getPath().replace("/", File.separator) + "\"?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-site scheme='app' host='labs-local.lds.org' port='80'>"
			+ "   <cctx-mapping thost='127.0.0.1' tport='9999'>"
            + "    <policy-source>xml={{policy-src-xml}}</policy-source>"
            + "   </cctx-mapping>"
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

//		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
//		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to query should NOT be allowed " + url);
//		
//		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
//		Assert.assertFalse(tman.isPermitted("GET", url, u), "due to query should NOT be allowed " + url);
//		
//		url = "app://labs-local.lds.org:80/auth/ui/sign-in?locale=eng";
//		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to query should NOT be allowed " + url);
//		
//		url = "app://labs-local.lds.org/auth/ui/sign-in?locale=eng";
//		Assert.assertFalse(tman.isPermitted("PUT", url, u), "due to query should NOT be allowed " + url);
	}

	//@Test
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

	//@Test
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

	//@Test
	public void testIsAllowedBySiteConfig() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-site host='labs-local.lds.org' port='80'>"
			+ "    <cctx-mapping thost='127.0.0.1' tport='8411' />"
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

	//@Test
	public void testHttpsSchemeConfigIsAllowed() throws Exception {
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='88' proxy-port='45'>"
			+ " <sso-traffic>"
			+ "  <by-site scheme='https' host='labs-local.lds.org' port='80'>"
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
		res = tman.isUnenforced("https://labs-local.lds.org/auth/ui/sign-in");
		Assert.assertTrue(res, "should be unenforced http://labs-local.lds.org:/auth/ui/sign-in");
	}

    //@Test
    public void test_FileAlias() throws Exception {
        String path = "file-has-lds-app-1234-test.xml";
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        FileWriter writer = new FileWriter(path);
        writer.write("<Attribute name='ldsApplications' operation='EQUALS' value='1234'/>\n");
        writer.flush();
        writer.close();
        
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?file-alias has-lds-app-1234=file-has-lds-app-1234-test.xml?>"
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
        u1234.addAttributeValues("ldsApplications", new String[] {"1234"});
        User user = new User("user", "user"); 
        user.addAttributeValues("ldsApplications", new String[] {"1000"});

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
            + "<config proxy-port='80' console-port='81'>"
            + "  <sso-cookie name='lds-policy' domain='.host.net'/>"
            + "  <sso-traffic>"
            + "   <by-site host='local.host.net' port='45'/>"
            + "  </sso-traffic>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        AliasHolder vals = XmlConfigLoader2.get(XmlConfigLoader2.PARSING_ALIASES);
        Assert.assertEquals(vals.getAliasValue("embedded wsp"), "embedded");
        Assert.assertEquals(vals.getAliasValue("nameWSP"), "nameWSP");
        Assert.assertEquals(vals.getAliasValue("valueWSP"), "valueWSP");
        Assert.assertEquals(vals.getAliasValue("vembedded"), "v embedded");
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

    //@Test
    public void test_conditionValuesInParseContainers() throws Exception {
        
        System.setProperty("condition-syntax", "<Attribute name='acctid' operation='equals' value='123'/>");
        
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
        AliasHolder conSyntaxes = XmlConfigLoader2.get(XmlConfigLoader2.PARSING_ALIASES);
        Assert.assertNotNull(conSyntaxes.getAliasValue("somealias"));
        Assert.assertEquals(conSyntaxes.getAliasValue("somealias"), "<Attribute name='acctid' operation='equals' value='123'/>");
        Assert.assertEquals(conSyntaxes.getAlias("somealias").toString(), "system:condition-syntax");        
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
        u1234.addAttributeValues("ldsApplications", new String[] {"1234"});
        User user = new User("user", "user"); 
        user.addAttributeValues("ldsApplications", new String[] {"1000"});

        String uri = "app://labs-local.lds.org/auth/_app/debug";
        boolean u12134Answer = tman.isPermitted("POST", uri, u1234); 
        Assert.fail("should have thrown exception");
    }
}
