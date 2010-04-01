package org.lds.sso.appwrap;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SessionManagerTest {

	@Test
	public void testConcurrentModification() {
		SessionManager sman = new SessionManager();
		sman.generateSessionToken("1");
		sman.generateSessionToken("2");
		sman.generateSessionToken("3");
		sman.generateSessionToken("4");
		Iterator<Session> itr = sman.getSessions().iterator();
		itr.next();
		sman.generateSessionToken("5");
		itr.next();
		itr = sman.getSessions().iterator();
		sman.isValidToken("3");
		itr.next();
		sman.generateSessionToken("6");
		sman.terminateSession("5");
		sman.generateSessionToken("7");
		sman.terminateSession("6");
		itr.next();
	}
}
