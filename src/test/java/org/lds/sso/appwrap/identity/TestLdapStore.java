package org.lds.sso.appwrap.identity;

import org.lds.sso.appwrap.identity.ldap.*;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * Holder of test against real LDAP system. Never check in with annotation uncommented. Only run manually to test
 * connectivity.
 */
public class TestLdapStore {
	Logger cLog = Logger.getLogger(TestLdapStore.class.getName());

	//@Test
	public void test() throws UnableToConnecToLdap, UnableToBindSearchUser, UnableToBindEndUser,
			UnableToLoadUserAttributes, UserNotFound, UnableToSearchForUser, UnableToGetUserAttributes {
		LdapStore.setEnv("ou=people,o=lds", "CN=???,OU=Apps,O=lds", "???", "ldap://gdirstage.wh.ldsglobal.net:636", false,
				new String[] {"ldsapps", "entrydn", "ldsmrn", "cn", "ldsposv2", "mail", "employeeStatus", "preferredname"});
		Map<String, List<String>> atts = LdapStore.getUserAttributes("boydmr", "");
		for(Entry<String, List<String>> ent : atts.entrySet()) {
			cLog.info(ent.getKey() + "=" + ent.getValue());
		}
	}
}
