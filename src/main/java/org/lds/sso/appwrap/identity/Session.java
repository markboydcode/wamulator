package org.lds.sso.appwrap.identity;

import java.net.URLEncoder;

/**
 * Simple class for keeping track of a session id and the point in time 
 * when the session should expire.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class Session {
	/**
	 * 
	 */
	private SessionManager mgr = null;
	public long scheduledMillisTimeoutPoint = 0;
	public String token = null;
	public String username = null;
	
	private Session() {
	    // for copying only
	}
	
	Session(SessionManager mgr, String username) {
		
		this.mgr = mgr;
		this.username = username;
		this.token = URLEncoder.encode(username + "-" + this.hashCode());
		markAsActive();
	}
	
	public String getUsername() {
		return username;
	}
	
	public Session copy() {
	    Session s = new Session();
	    s.mgr = this.mgr;
	    s.token = this.token;
	    s.markAsActive();
	    return s;
	}
	
	public String getToken() {
		return token;
	}
	
	public int getRemainingSeconds() {
		return (int)((scheduledMillisTimeoutPoint - System.currentTimeMillis())/1000L);
	}
	
	/**
	 * Returns true if the session timeout point is greater than the 
	 * current point in time.
	 * 
	 * @return
	 */
	public boolean isActive() {
		long current = System.currentTimeMillis();
		long timeoutPoint = scheduledMillisTimeoutPoint;
		if (current < timeoutPoint) {
			return true;
		}
		return false;
	}
	
	/**
	 * Tests for session still being active and if true updates its timeout point
	 * to be the current time plus the inactivity timeout period.
	 *  
	 * @return
	 */
	public void markAsActive() {
		scheduledMillisTimeoutPoint = System.currentTimeMillis()
			+ mgr.getSessionInactivityTimeoutMillis();
	}
}