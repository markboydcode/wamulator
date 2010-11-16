package org.lds.sso.appwrap;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SessionManagerTest {

	@Test
	public void testConcurrentModification() {
		SessionManager sman = new SessionManager();
		sman.setMasterCookieDomain(".test.org");
		sman.addCookieDomain(".test.org");
		sman.generateSessionToken("1", "host.test.org");
		sman.generateSessionToken("2", "host.test.org");
		sman.generateSessionToken("3", "host.test.org");
		sman.generateSessionToken("4", "host.test.org");
		Iterator<Session> itr = sman.getSessions(".test.org").iterator();
		itr.next();
		sman.generateSessionToken("5", "host.test.org");
		itr.next();
		itr = sman.getSessions(".test.org").iterator();
		sman.isValidToken("3", "host.test.org");
		itr.next();
		sman.generateSessionToken("6", "host.test.org");
		sman.terminateSession("5", sman.getCookieDomainForHost("host.test.org"));
		sman.generateSessionToken("7", "host.test.org");
		sman.terminateSession("6", sman.getCookieDomainForHost("host.test.org"));
		itr.next();
	}
}
