package org.lds.sso.appwrap;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.testng.annotations.Test;

public class UserManagerTest {

	@Test
	public void testConcurrentModSafety() {
		UserManager umgr = new UserManager();
		umgr.setUser("me", "");
		umgr.setUser("m1", "");
		umgr.setUser("m2", "");
		umgr.setUser("m3", "");
		umgr.setUser("m4", "");
		Collection<User> users = umgr.getUsers();
		Iterator<User> itr = users.iterator();
		User usr = itr.next();
		umgr.removeUser("m3");
		usr = itr.next();
		umgr.setUser("anotherone", "");
		usr = itr.next();
	}
}
