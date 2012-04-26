package org.lds.sso.appwrap.identity;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.httpclient.HttpException;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.identity.ExternalUserSource.ConfigurationException;
import org.lds.sso.appwrap.identity.ExternalUserSource.Response;
import org.lds.sso.appwrap.identity.coda.CodaUserSource;
import org.lds.sso.appwrap.identity.ldap.LdapUserSource;
import org.lds.sso.appwrap.identity.ldap.UnableToBindEndUser;
import org.lds.sso.appwrap.identity.ldap.UnableToBindSearchUser;
import org.lds.sso.appwrap.identity.ldap.UnableToConnecToLdap;
import org.lds.sso.appwrap.identity.ldap.UnableToGetUserAttributes;
import org.lds.sso.appwrap.identity.ldap.UnableToLoadUserAttributes;
import org.lds.sso.appwrap.identity.ldap.UnableToSearchForUser;
import org.lds.sso.appwrap.identity.ldap.UserNotFound;
import org.lds.sso.appwrap.identity.legacy.WamulatorUserSource;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests features of cascading user sources such as 'stopOnFound' and 
 * 'aggregation'.
 * 
 * @author BoydMR
 *
 */
public class CascadingUserSourceIntegrationTest {

	@Test
	public void test_wamulatorSource_stopOnFound_false_then_wamulatorSource() throws ConfigurationException, IOException {
		Config cfg = new Config(); // clear configuration.
		ExternalUserSource src = new WamulatorUserSource();
		src.setUserManager(cfg.getUserManager());
		Properties props = new Properties();
		props.setProperty("preload-only", "true");
		props.setProperty("xml", ""
			+ "<users>"
			+ " <user name='ngiwb1' pwd='password1'>"
			+ "  <att name='acctid' aggregation='fix'    value='555'  />"
			+ "  <att name='apps'                        value='aaa'  />"
			+ "  <att name='apps'                        value='bbb'  />"
			+ "  <att name='apps' aggregation='merge'    value='ddd'  />"
			+ "  <att name='preferredname'               value='Jay Admin Man'/>"
			+ "  <att name='givenname'                   value='Jay Admin'/>"
			+ "  <att name='preferredlanguage'           value='eng'/>"
			+ "  <att name='att-2'                       value='val2-1'/>"
			+ "  <att name='att-2' aggregation='replace' value='val2-2'/>"
			+ " </user>"
			+ "</users>");
		src.setConfig(new Path(), props);
		
		Response s = src.loadExternalUser("ngiwb1", "password1");
		Assert.assertEquals(s, Response.UserNotFound, "should have returned user-not-found since stopOnFound=false.");

		// now run with another instance and see if it honors the 
		src = new WamulatorUserSource();
		src.setUserManager(cfg.getUserManager());
		props = new Properties();
		props.setProperty("xml", ""
			+ "<users>"
			+ " <user name='ngiwb1' pwd='password1'>"
			+ "  <att name='acctid'                      value='888'  />" // should be ignored
			+ "  <att name='apps'                        value='ccc'  />" // should be added
			+ "  <att name='preferredname'               value='Admin'/>" // should be added since merge is default
			+ "  <att name='att-2'                       value='val3' />" // should have one value: val3
			+ " </user>"
			+ "</users>");
		src.setConfig(new Path(), props);
		
		s = src.loadExternalUser("ngiwb1", "password1");
		Assert.assertEquals(s, Response.UserInfoLoaded, "should have returned user-info-loaded since stopOnFound not specified and defaults to true.");
		User usr = cfg.getUserManager().getUser("ngiwb1");
		Assert.assertEquals(usr.getAttribute("acctid").length, 1, "acctid should not have been added to");
		Assert.assertEquals(usr.getAttribute("acctid")[0], "555", "acctid should not have been changed");
		Assert.assertEquals(usr.getAttribute("apps").length, 4, "apps should have been added to");
		Assert.assertEquals(usr.getAttribute("apps")[0], "aaa", "apps values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("apps")[1], "bbb", "apps values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("apps")[2], "ccc", "apps values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("apps")[3], "ddd", "apps values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("preferredname").length, 2, "preferredname should have been added to");
		Assert.assertEquals(usr.getAttribute("preferredname")[0], "Admin", "preferredname values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("preferredname")[1], "Jay Admin Man", "preferredname values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("att-2").length, 1, "att-2 should only have one value");
		Assert.assertEquals(usr.getAttribute("att-2")[0], "val3", "att-2 value should be val3");
	}

	@Test
	public void test_wamulatorSource_stopOnFound_false_then_codaUserSource() throws ConfigurationException, IOException {
		Config cfg = new Config(); // clear configuration.
		ExternalUserSource src = new WamulatorUserSource();
		src.setUserManager(cfg.getUserManager());
		Properties props = new Properties();
		props.setProperty("preload-only", "true");
		props.setProperty("xml", ""
			+ "<users>"
			+ " <user name='ngiwb1' pwd='password1'>"
			+ "  <att name='acctid' aggregation='fix'    value='555'  />"
			+ "  <att name='apps'                        value='aaa'  />"
			+ "  <att name='apps'                        value='bbb'  />"
			+ "  <att name='apps' aggregation='merge'    value='ddd'  />"
			+ "  <att name='preferredname'               value='Jay Admin Man'/>"
			+ "  <att name='givenname'                   value='Jay Admin'/>"
			+ "  <att name='preferredlanguage'           value='eng'/>"
			+ "  <att name='att-2'                       value='val2-1'/>"
			+ "  <att name='att-2' aggregation='replace' value='val2-2'/>"
			+ " </user>"
			+ "</users>");
		src.setConfig(new Path(), props);
		
		Response s = src.loadExternalUser("ngiwb1", "password1");
		Assert.assertEquals(s, Response.UserNotFound, "should have returned user-not-found since stopOnFound=false.");

		// now run with a coda instance and see if it honors aggregation 
		src = new CodaUserSource() {

			@Override
			protected Results callCoda(String uri) throws HttpException, IOException {
				Results res = new Results(200, ""
						 + "<org.lds.community.data.ws.dto.OssoMemberDto>"
						 + "  <acctid>888</acctid>"
						 + "  <apps>ccc</apps>"
						 + "  <preferredname>Admin</preferredname>"
						 + "  <att-2>val3</att-2>"
						 + "</org.lds.community.data.ws.dto.OssoMemberDto>");
				return res;
			}
			
		};
		src.setUserManager(cfg.getUserManager());
		props = new Properties();
		props.setProperty("url", "https://cint.lds.org/coda/services/1/member/osso/{username}");
		src.setConfig(new Path(), props);
		
		s = src.loadExternalUser("ngiwb1", "password1");
		Assert.assertEquals(s, Response.UserInfoLoaded, "should have returned user-info-loaded since stopOnFound not specified and defaults to true.");
		User usr = cfg.getUserManager().getUser("ngiwb1");
		Assert.assertEquals(usr.getAttribute("acctid").length, 1, "acctid should not have been added to");
		Assert.assertEquals(usr.getAttribute("acctid")[0], "555", "acctid should not have been changed");
		Assert.assertEquals(usr.getAttribute("apps").length, 4, "apps should have been added to");
		Assert.assertEquals(usr.getAttribute("apps")[0], "aaa", "apps values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("apps")[1], "bbb", "apps values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("apps")[2], "ccc", "apps values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("apps")[3], "ddd", "apps values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("preferredname").length, 2, "preferredname should have been added to");
		Assert.assertEquals(usr.getAttribute("preferredname")[0], "Admin", "preferredname values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("preferredname")[1], "Jay Admin Man", "preferredname values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("att-2").length, 1, "att-2 should only have one value");
		Assert.assertEquals(usr.getAttribute("att-2")[0], "val3", "att-2 value should be val3");
	}
	
	@Test
	public void test_wamulatorSource_stopOnFound_false_then_ldapUserSource() throws ConfigurationException, IOException {
		Config cfg = new Config(); // clear configuration.
		ExternalUserSource src = new WamulatorUserSource();
		src.setUserManager(cfg.getUserManager());
		Properties props = new Properties();
		props.setProperty("preload-only", "true");
		props.setProperty("xml", ""
			+ "<users>"
			+ " <user name='ngiwb1' pwd='password1'>"
			+ "  <att name='acctid' aggregation='fix'    value='555'  />"
			+ "  <att name='apps'                        value='aaa'  />"
			+ "  <att name='apps'                        value='bbb'  />"
			+ "  <att name='apps' aggregation='merge'    value='ddd'  />"
			+ "  <att name='preferredname'               value='Jay Admin Man'/>"
			+ "  <att name='givenname'                   value='Jay Admin'/>"
			+ "  <att name='preferredlanguage'           value='eng'/>"
			+ "  <att name='att-2'                       value='val2-1'/>"
			+ "  <att name='att-2' aggregation='replace' value='val2-2'/>"
			+ " </user>"
			+ "</users>");
		src.setConfig(new Path(), props);
		
		Response s = src.loadExternalUser("ngiwb1", "password1");
		Assert.assertEquals(s, Response.UserNotFound, "should have returned user-not-found since stopOnFound=false.");

		// now run with a ldap instance and see if it honors aggregation 
		src = new LdapUserSource() {

			@Override
			protected Map<String, List<String>> callLdap(String username, String password) throws UnableToConnecToLdap,
					UnableToBindSearchUser, UserNotFound, UnableToSearchForUser, UnableToBindEndUser,
					UnableToGetUserAttributes, UnableToLoadUserAttributes {
				Map<String, List<String>> atts = new HashMap<String, List<String>>();
				
				atts.put("acctid", Arrays.asList(new String[] {"888"}));
				atts.put("apps", Arrays.asList(new String[] {"ccc"}));
				atts.put("preferredname", Arrays.asList(new String[] {"Admin"}));
				atts.put("att-2", Arrays.asList(new String[] {"val3"}));
				return atts;
			}

			@Override
			protected void testLdap(String searchBase, String dn, String pwd, String url, boolean enableTls,
					String[] list) {
				// just don't throw exception to simulate successful connection to ldap
			}
		};
		src.setUserManager(cfg.getUserManager());
		props = new Properties();
		props.setProperty("url", "a");
		props.setProperty("search-base-dn", "a");
		props.setProperty("search-bind-dn", "a");
		props.setProperty("search-bind-pwd", "a");
		src.setConfig(new Path(), props);
		
		s = src.loadExternalUser("ngiwb1", "password1");
		Assert.assertEquals(s, Response.UserInfoLoaded, "should have returned user-info-loaded since stopOnFound not specified and defaults to true.");
		User usr = cfg.getUserManager().getUser("ngiwb1");
		Assert.assertEquals(usr.getAttribute("acctid").length, 1, "acctid should not have been added to");
		Assert.assertEquals(usr.getAttribute("acctid")[0], "555", "acctid should not have been changed");
		Assert.assertEquals(usr.getAttribute("apps").length, 4, "apps should have been added to");
		Assert.assertEquals(usr.getAttribute("apps")[0], "aaa", "apps values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("apps")[1], "bbb", "apps values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("apps")[2], "ccc", "apps values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("apps")[3], "ddd", "apps values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("preferredname").length, 2, "preferredname should have been added to");
		Assert.assertEquals(usr.getAttribute("preferredname")[0], "Admin", "preferredname values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("preferredname")[1], "Jay Admin Man", "preferredname values should be in alphabetical order");
		Assert.assertEquals(usr.getAttribute("att-2").length, 1, "att-2 should only have one value");
		Assert.assertEquals(usr.getAttribute("att-2")[0], "val3", "att-2 value should be val3");
	}
}
