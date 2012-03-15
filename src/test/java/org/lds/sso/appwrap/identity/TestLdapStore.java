package org.lds.sso.appwrap.identity;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.lds.sso.appwrap.identity.ldap.LdapStore;
import org.lds.sso.appwrap.identity.ldap.UnableToBindEndUser;
import org.lds.sso.appwrap.identity.ldap.UnableToBindSearchUser;
import org.lds.sso.appwrap.identity.ldap.UnableToConnecToLdap;
import org.lds.sso.appwrap.identity.ldap.UnableToGetUserAttributes;
import org.lds.sso.appwrap.identity.ldap.UnableToLoadUserAttributes;
import org.lds.sso.appwrap.identity.ldap.UnableToSearchForUser;
import org.lds.sso.appwrap.identity.ldap.UserNotFound;

public class TestLdapStore {
	Logger cLog = Logger.getLogger(TestLdapStore.class.getName());

	//@Test
	public void test() throws UnableToConnecToLdap, UnableToBindSearchUser, UnableToBindEndUser,
			UnableToLoadUserAttributes, UserNotFound, UnableToSearchForUser, UnableToGetUserAttributes {
		LdapStore.setEnv("ou=people,o=lds", "CN=???,OU=Apps,O=lds", "???", "ldap://gdirstage.wh.ldsglobal.net:636", 
				new String[] {"ldsapps", "entrydn", "ldsmrn", "cn", "ldsposv2", "mail", "employeeStatus", "preferredname"});
		Map<String, List<String>> atts = LdapStore.getUserAttributes("boydmr", "");
		for(Entry<String, List<String>> ent : atts.entrySet()) {
			cLog.info(ent.getKey() + "=" + ent.getValue());
		}
	}
}
