package org.lds.sso.appwrap.opensso;

import org.lds.sso.appwrap.User;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;

public class AssignmentsChainInjectorTest {

	@Test
	public void testSingleAssig() throws SSOException {
		User u = new User("test", "pwd");
		u.addHeader(LegacyPropsInjectorDefs.CP_POSITIONS_SESSION_PROPERTY, "P4:W123:S456:A789");
		Assert.assertEquals(u.getProperty(LegacyPropsInjectorDefs.CACHE_TYPE), LegacyPropsInjectorDefs.SINGLE);
		Assert.assertEquals(u.getProperty(LegacyPropsInjectorDefs.CACHED_VALUE + "0"), "P4:123:456:789");
	}

	@Test
	public void testTwoAssigs() throws SSOException {
		User u = new User("test", "pwd");
		u.addHeader(LegacyPropsInjectorDefs.CP_POSITIONS_SESSION_PROPERTY, "P4:W123:S456:A789|P1:S456:A789");
		Assert.assertEquals(u.getProperty(LegacyPropsInjectorDefs.CACHE_TYPE), 
				LegacyPropsInjectorDefs.MULTI_CHAR + LegacyPropsInjectorDefs.MULTI_CHAR);
		Assert.assertEquals(u.getProperty(LegacyPropsInjectorDefs.CACHED_VALUE + "0"), "P4:123:456:789");
		Assert.assertEquals(u.getProperty(LegacyPropsInjectorDefs.CACHED_VALUE + "1"), "P1:456:789");
	}

	@Test
	public void testThreeAssigs() throws SSOException {
		User u = new User("test", "pwd");
		u.addHeader(LegacyPropsInjectorDefs.CP_POSITIONS_SESSION_PROPERTY, "P4:W123:S456:A789|P1:S456:A789|P57:W123:S456:A789|");
		Assert.assertEquals(u.getProperty(LegacyPropsInjectorDefs.CACHE_TYPE), 
				LegacyPropsInjectorDefs.MULTI_CHAR + LegacyPropsInjectorDefs.MULTI_CHAR + LegacyPropsInjectorDefs.MULTI_CHAR);
		Assert.assertEquals(u.getProperty(LegacyPropsInjectorDefs.CACHED_VALUE + "0"), "P4:123:456:789");
		Assert.assertEquals(u.getProperty(LegacyPropsInjectorDefs.CACHED_VALUE + "1"), "P1:456:789");
		Assert.assertEquals(u.getProperty(LegacyPropsInjectorDefs.CACHED_VALUE + "2"), "P57:123:456:789");
	}
}
