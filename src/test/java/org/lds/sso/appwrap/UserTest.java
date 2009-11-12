package org.lds.sso.appwrap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UserTest {

	@Test
	public void testIsPermitted() {
		User usr = new User(null, null);
		usr.addAllowedUri(new AllowedUri("host", 80, "/some/wild/*", new String[] {"GET", "PUT"}));
		usr.addAllowedUri(new AllowedUri("host", 80, "/some/specific/path", new String[] {"GET", "PUT"}));
		Assert.assertTrue(usr.isPermitted("host", 80, "GET", "/some/wild/card/path"), "GET on path should match wild card");
		Assert.assertTrue(usr.isPermitted("host", 80, "PUT", "/some/wild/card/path"), "PUT on path should match wild card");
		Assert.assertFalse(usr.isPermitted("host", 80, "POST", "/some/wild/card/path"), "POST should not match");
		Assert.assertFalse(usr.isPermitted("host", 80, "GET","/some/path"), "GET on path should not match specific or wild card");
		Assert.assertTrue(usr.isPermitted("host", 80, "PUT","/some/specific/path"), "PUT path should match specific path");
		Assert.assertTrue(usr.isPermitted("host", 80, "GET","/some/specific/path"), "GET path should match specific path");
		Assert.assertFalse(usr.isPermitted("host", 80, "POST","/some/specific/path"), "POST should not match");
	}
}
