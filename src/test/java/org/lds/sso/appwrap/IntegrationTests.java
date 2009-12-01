package org.lds.sso.appwrap;

import org.lds.sso.clientlib.PDPAccessor;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class IntegrationTests {
	
	int shared_cfg_console_port = 1774;
	int shared_cfg_proxy_port = 9997;
	Service service = null;
	private int starts = 0;
	
	/**
	 * Example of how to start the service, test both call level interface (cli)
	 * and http rest interface, then shutdown the service. Leverages the SSO 
	 * client lib for hitting the rest interface.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testStandaloneService_RestApiResponseMatchesCliResponse() throws Exception {
		int console_rest_port = 1775;
		
		Service service = new Service("string:"
			+ "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='" + console_rest_port + "' proxy-port='9998'>"
			+ " <sso-traffic>"
			+ "  <by-resource uri='app://labs-local.lds.org/auth/ui/*' unenforced='true'/>"
			+ "  <by-resource uri='app://labs-local.lds.org/auth/_app/*' allow='GET,POST'/>"
			+ " </sso-traffic>"
			+ " <users>"
	        + "  <user name='ngia' pwd='pwda'>"
            + "    <sso-header name='policy-birthdate' value='1960-09-25'/>"
            + "    <sso-header name='policy-email' value='me@someplace.com'/>"
            + "    <sso-header name='policy-preferred-name' value='Samson the great'/>"
            + "  </user>"
			+ " </users>"
		    + "</config>");
		service.start();

		// now get loaded config and PDPAccessor and verify comparable behavior 
		Config cfg = Config.getInstance();
		TrafficManager tman = cfg.getTrafficManager();
		PDPAccessor pdp = new PDPAccessor("http://127.0.0.1:" + console_rest_port + "/rest/");

		String uri = "http://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isUnenforced(uri), "due to wrong scheme should NOT be unenforced " + uri);
		Assert.assertFalse(pdp.isPermitted("bogus-token", "GET", uri), "due to wrong scheme should NOT be unenforced " + uri);
		
		uri = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isUnenforced(uri), "should be unenforced " + uri);
		Assert.assertTrue(pdp.isPermitted("bogus-token", "GET", uri), "should be allowed since unenforced " + uri);

		String token = pdp.authenticate("ngia", "pwda");
		
		uri = "http://labs-local.lds.org/auth/_app/debug";
		Assert.assertFalse(tman.isPermitted("POST", uri), "due to wrong scheme should NOT be allowed " + uri);
		Assert.assertFalse(pdp.isPermitted(token, "GET", uri), "due to wrong scheme should NOT be unenforced " + uri);

		uri = "app://labs-local.lds.org/auth/_app/debug";
		Assert.assertTrue(tman.isPermitted("POST", uri), "should be allowed " + uri);
		Assert.assertTrue(pdp.isPermitted(token, "POST", uri), "should be allowed " + uri);

		pdp.terminateSession(token, "ngia");
		service.stop();
	}
	
	/**
	 * Starts up a service instance if not already instantiated and ties it to
	 * specific ports but without any sso-traffic or user definitions.
	 * 
	 * @throws Exception
	 */
	private Service connectService() throws Exception {
		if (service == null) {
			starts++;
			
			if (starts > 1) {
				throw new IllegalStateException("Should only start service once.");
			}
			service = new Service("string:"
					+ "<?xml version='1.0' encoding='UTF-8'?>"
					+ "<config console-port='" + shared_cfg_console_port + "'" 
					+ " proxy-port='" + shared_cfg_proxy_port + "'>"
				    + "</config>");
				service.start();
		}
		return service;
	}

	/**
	 * Example of re-purposing a single service already listening on specific
	 * ports to use a different config object for different test scenarios.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testConfigSwap3() throws Exception {
		Service service = connectService();

		// now get load new underlying config thereby swapping out behavior 
		// except for what ports the service instance is already using. 
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			// ports are irrelevant since service isn't being restarted only
			// config is being reloaded. But they have to parse properly which
			// means they must have proper integer values.
			+ "<config console-port='1' proxy-port='2'>"  
			+ " <sso-traffic>"
			+ "  <by-resource uri='app3://labs-local.lds.org/auth/ui/*' unenforced='true'/>"
			+ "  <by-resource uri='app3://labs-local.lds.org/auth/_app/*' allow='GET,POST'/>"
			+ " </sso-traffic>"
			+ " <users>"
	        + "  <user name='test3' pwd='test3'/>"
			+ " </users>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		PDPAccessor pdp = new PDPAccessor("http://127.0.0.1:" + shared_cfg_console_port + "/rest/");

		String uri = null;
		
		uri = "app3://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isUnenforced(uri), "should be unenforced " + uri);
		Assert.assertTrue(pdp.isPermitted("bogus-token", "GET", uri), "should be allowed since unenforced " + uri);

		uri = "app2://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isUnenforced(uri), "should NOT be unenforced since not in config " + uri);
		Assert.assertFalse(pdp.isPermitted("bogus-token", "GET", uri), "should NOT be allowed since not in config " + uri);

		String token = pdp.authenticate("test3", "test3");
		
		uri = "app3://labs-local.lds.org/auth/_app/debug";
		Assert.assertTrue(tman.isPermitted("POST", uri), "should be allowed " + uri);
		Assert.assertTrue(pdp.isPermitted(token, "POST", uri), "should be allowed " + uri);

		uri = "app2://labs-local.lds.org/auth/_app/debug";
		Assert.assertFalse(tman.isPermitted("POST", uri), "should NOT be allowed since not in config " + uri);
		Assert.assertFalse(pdp.isPermitted(token, "POST", uri), "should NOT be allowed since not in config " + uri);

		pdp.terminateSession(token, "test3");
	}
	
	
	/**
	 * Example of re-purposing a single service already listening on specific
	 * ports to use a different config object for different test scenarios.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testConfigSwap2() throws Exception {
		Service service = connectService();

		// now get load new underlying config thereby swapping out behavior 
		// except for what ports the service instance is already using. 
		String xml = 
			"<?xml version='1.0' encoding='UTF-8'?>"
			// ports are irrelevant since service isn't being restarted only
			// config is being reloaded. But they have to parse properly which
			// means they must have proper integer values.
			+ "<config console-port='1' proxy-port='2'>"  
			+ " <sso-traffic>"
			+ "  <by-resource uri='app2://labs-local.lds.org/auth/ui/*' unenforced='true'/>"
			+ "  <by-resource uri='app2://labs-local.lds.org/auth/_app/*' allow='GET,POST'/>"
			+ " </sso-traffic>"
			+ " <users>"
	        + "  <user name='test2' pwd='test2'/>"
			+ " </users>"
		    + "</config>";
		Config cfg = new Config();
		XmlConfigLoader2.load(xml);
		TrafficManager tman = cfg.getTrafficManager();
		PDPAccessor pdp = new PDPAccessor("http://127.0.0.1:" + shared_cfg_console_port + "/rest/");

		String uri = null;
		
		uri = "app2://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isUnenforced(uri), "should be unenforced " + uri);
		Assert.assertTrue(pdp.isPermitted("bogus-token", "GET", uri), "should be allowed since unenforced " + uri);

		uri = "app3://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isUnenforced(uri), "should NOT be unenforced since not in config " + uri);
		Assert.assertFalse(pdp.isPermitted("bogus-token", "GET", uri), "should NOT be allowed since not in config " + uri);

		String token = pdp.authenticate("test2", "test2");
		
		uri = "app2://labs-local.lds.org/auth/_app/debug";
		Assert.assertTrue(tman.isPermitted("POST", uri), "should be allowed " + uri);
		Assert.assertTrue(pdp.isPermitted(token, "POST", uri), "should be allowed " + uri);

		uri = "app3://labs-local.lds.org/auth/_app/debug";
		Assert.assertFalse(tman.isPermitted("POST", uri), "should NOT be allowed since not in config " + uri);
		Assert.assertFalse(pdp.isPermitted(token, "POST", uri), "should NOT be allowed since not in config " + uri);

		pdp.terminateSession(token, "test2");
	}
	
	@AfterClass
	public void shutdown() throws Exception {
		System.out.println("-----shutting down.");
		if (service != null) {
			service.stop();
		}
	}
}
