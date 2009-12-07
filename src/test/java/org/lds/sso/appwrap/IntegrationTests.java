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
	public void testRunningAppDirectly() throws Exception {
		Service service = new Service("YouthAppProxy.xml");
		service.start();

		PDPAccessor pdp = new PDPAccessor("http://127.0.0.1:1776/rest/");

		String photoUsr = pdp.authenticate("photo-editor", "pwda");
		String photo2Usr = pdp.authenticate("photo-editor2", "pwda");
		String fieldUsr = pdp.authenticate("field-moderator", "pwda");
		String hqUsr = pdp.authenticate("hq-moderator", "pwdb");
		String ipUsr = pdp.authenticate("ip-moderator", "pwdb");
		String corrUsr = pdp.authenticate("corr-moderator", "pwdb");

		String photo_uri = "app://labs-local.lds.org/youth-approval/photo-editor";
		String field_uri = "app://labs-local.lds.org/youth-approval/field-moderator";
		String hq_uri = "app://labs-local.lds.org/youth-approval/hq-moderator";
		String ip_uri = "app://labs-local.lds.org/youth-approval/ip-moderator";
		String corr_uri = "app://labs-local.lds.org/youth-approval/corr-moderator";
		
		Assert.assertFalse(pdp.isPermitted(photoUsr, "GET", corr_uri), "should NOT be allowed for photoUsr" + corr_uri);
		Assert.assertFalse(pdp.isPermitted(photo2Usr, "GET", corr_uri), "should NOT be allowed for photo2Usr" + corr_uri);
		Assert.assertFalse(pdp.isPermitted(fieldUsr, "GET", corr_uri), "should NOT be allowed for fieldUsr" + corr_uri);
		Assert.assertFalse(pdp.isPermitted(hqUsr, "GET", corr_uri), "should NOT be allowed for hqUsr" + corr_uri);
		Assert.assertFalse(pdp.isPermitted(ipUsr, "GET", corr_uri), "should NOT be allowed for ipUsr" + corr_uri);
		Assert.assertTrue(pdp.isPermitted(corrUsr, "GET", corr_uri), "should be allowed for corrUsr" + corr_uri);

		Assert.assertFalse(pdp.isPermitted(photoUsr, "GET", ip_uri), "should NOT be allowed for photoUsr" + ip_uri);
		Assert.assertFalse(pdp.isPermitted(photo2Usr, "GET", ip_uri), "should NOT be allowed for photo2Usr" + ip_uri);
		Assert.assertFalse(pdp.isPermitted(fieldUsr, "GET", ip_uri), "should NOT be allowed for fieldUsr" + ip_uri);
		Assert.assertFalse(pdp.isPermitted(hqUsr, "GET", ip_uri), "should NOT be allowed for hqUsr" + ip_uri);
		Assert.assertTrue(pdp.isPermitted(ipUsr, "GET", ip_uri), "should be allowed for ipUsr" + ip_uri);
		Assert.assertFalse(pdp.isPermitted(corrUsr, "GET", ip_uri), "should NOT be allowed for corrUsr" + ip_uri);

		Assert.assertFalse(pdp.isPermitted(photoUsr, "GET", hq_uri), "should NOT be allowed for photoUsr" + hq_uri);
		Assert.assertFalse(pdp.isPermitted(photo2Usr, "GET", hq_uri), "should NOT be allowed for photo2Usr" + hq_uri);
		Assert.assertFalse(pdp.isPermitted(fieldUsr, "GET", hq_uri), "should NOT be allowed for fieldUsr" + hq_uri);
		Assert.assertTrue(pdp.isPermitted(hqUsr, "GET", hq_uri), "should be allowed for hqUsr" + hq_uri);
		Assert.assertFalse(pdp.isPermitted(ipUsr, "GET", hq_uri), "should NOT be allowed for ipUsr" + hq_uri);
		Assert.assertFalse(pdp.isPermitted(corrUsr, "GET", hq_uri), "should NOT be allowed for corrUsr" + hq_uri);

		Assert.assertTrue(pdp.isPermitted(photoUsr, "GET", photo_uri), "should be allowed for photoUsr" + photo_uri);
		Assert.assertTrue(pdp.isPermitted(photo2Usr, "GET", photo_uri), "should be allowed for photo2Usr" + photo_uri);
		Assert.assertFalse(pdp.isPermitted(fieldUsr, "GET", photo_uri), "should NOT be allowed for fieldUsr" + photo_uri);
		Assert.assertFalse(pdp.isPermitted(hqUsr, "GET", photo_uri), "should NOT be allowed for hqUsr" + photo_uri);
		Assert.assertFalse(pdp.isPermitted(ipUsr, "GET", photo_uri), "should NOT be allowed for ipUsr" + photo_uri);
		Assert.assertFalse(pdp.isPermitted(corrUsr, "GET", photo_uri), "should NOT be allowed for corrUsr" + photo_uri);

		Assert.assertFalse(pdp.isPermitted(photoUsr, "GET", field_uri), "should NOT be allowed for photoUsr" + field_uri);
		Assert.assertFalse(pdp.isPermitted(photo2Usr, "GET", field_uri), "should NOT be allowed for photo2Usr" + field_uri);
		Assert.assertTrue(pdp.isPermitted(fieldUsr, "GET", field_uri), "should be allowed for fieldUsr" + field_uri);
		Assert.assertFalse(pdp.isPermitted(hqUsr, "GET", field_uri), "should NOT be allowed for hqUsr" + field_uri);
		Assert.assertFalse(pdp.isPermitted(ipUsr, "GET", field_uri), "should NOT be allowed for ipUsr" + field_uri);
		Assert.assertFalse(pdp.isPermitted(corrUsr, "GET", field_uri), "should NOT be allowed for corrUsr" + field_uri);

		pdp.terminateSession(photoUsr, "photo-editor");
		pdp.terminateSession(photo2Usr, "photo-editor2");
		pdp.terminateSession(fieldUsr, "field-moderator");
		pdp.terminateSession(hqUsr, "hq-moderator");
		pdp.terminateSession(ipUsr, "ip-moderator");
		pdp.terminateSession(corrUsr, "corr-moderator");
		service.stop();
	}
	
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
		User u = new User("", ""); // no conditions are specified so any user object can be used.
		PDPAccessor pdp = new PDPAccessor("http://127.0.0.1:" + console_rest_port + "/rest/");

		String uri = "http://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isUnenforced(uri), "due to wrong scheme should NOT be unenforced " + uri);
		Assert.assertFalse(pdp.isPermitted("bogus-token", "GET", uri), "due to wrong scheme should NOT be allowed " + uri);
		
		uri = "app://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isUnenforced(uri), "should be unenforced " + uri);
		Assert.assertTrue(pdp.isPermitted("bogus-token", "GET", uri), "should be allowed since unenforced " + uri);

		String token = pdp.authenticate("ngia", "pwda");
		
		uri = "http://labs-local.lds.org/auth/_app/debug";
		Assert.assertFalse(tman.isPermitted("POST", uri, u), "due to wrong scheme should NOT be allowed " + uri);
		Assert.assertFalse(pdp.isPermitted(token, "GET", uri), "due to wrong scheme should NOT be allowed " + uri);

		uri = "app://labs-local.lds.org/auth/_app/debug";
		Assert.assertTrue(tman.isPermitted("POST", uri, u), "should be allowed " + uri);
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
		User u = new User("", ""); // no conditions are specified so any user object can be used.

		String uri = null;
		
		uri = "app3://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isUnenforced(uri), "should be unenforced " + uri);
		Assert.assertTrue(pdp.isPermitted("bogus-token", "GET", uri), "should be allowed since unenforced " + uri);

		uri = "app2://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isUnenforced(uri), "should NOT be unenforced since not in config " + uri);
		Assert.assertFalse(pdp.isPermitted("bogus-token", "GET", uri), "should NOT be allowed since not in config " + uri);

		String token = pdp.authenticate("test3", "test3");
		
		uri = "app3://labs-local.lds.org/auth/_app/debug";
		Assert.assertTrue(tman.isPermitted("POST", uri, u), "should be allowed " + uri);
		Assert.assertTrue(pdp.isPermitted(token, "POST", uri), "should be allowed " + uri);

		uri = "app2://labs-local.lds.org/auth/_app/debug";
		Assert.assertFalse(tman.isPermitted("POST", uri, u), "should NOT be allowed since not in config " + uri);
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
		User u = new User("", ""); // no conditions are specified so any user object can be used.

		String uri = null;
		
		uri = "app2://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertTrue(tman.isUnenforced(uri), "should be unenforced " + uri);
		Assert.assertTrue(pdp.isPermitted("bogus-token", "GET", uri), "should be allowed since unenforced " + uri);

		uri = "app3://labs-local.lds.org/auth/ui/sign-in";
		Assert.assertFalse(tman.isUnenforced(uri), "should NOT be unenforced since not in config " + uri);
		Assert.assertFalse(pdp.isPermitted("bogus-token", "GET", uri), "should NOT be allowed since not in config " + uri);

		String token = pdp.authenticate("test2", "test2");
		
		uri = "app2://labs-local.lds.org/auth/_app/debug";
		Assert.assertTrue(tman.isPermitted("POST", uri, u), "should be allowed " + uri);
		Assert.assertTrue(pdp.isPermitted(token, "POST", uri), "should be allowed " + uri);

		uri = "app3://labs-local.lds.org/auth/_app/debug";
		Assert.assertFalse(tman.isPermitted("POST", uri, u), "should NOT be allowed since not in config " + uri);
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
