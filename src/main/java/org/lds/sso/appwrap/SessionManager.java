package org.lds.sso.appwrap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public class SessionManager {
	private static final Logger cLog = Logger.getLogger(SessionManager.class);

	protected Map<String, Session> sessions = new TreeMap<String, Session>();

	public static final int DEFAULT_INACTIVITY_TIMEOUT_SECONDS = 300;

	private int sessionInactivityTimeoutSeconds = DEFAULT_INACTIVITY_TIMEOUT_SECONDS;

	long sessionInactivityTimeoutMillis = DEFAULT_INACTIVITY_TIMEOUT_SECONDS * 1000;

	public SessionManager(){
		startSessionTimeoutSentry();
	}
	
	/**
	 * Starts a thread that checks every ten seconds for sessions that have
	 * passed the allowed inactivity point and terminates them by removing them
	 * from the active sessions.
	 */
	protected void startSessionTimeoutSentry() {
		Runnable sentry = new Runnable() {

			public void run() {
				while (true) {
					try {
						Map<String, Session> copy = new HashMap<String, Session>(sessions);
						for (Iterator<Entry<String, Session>> itr = copy.entrySet().iterator(); itr.hasNext();) {
							Entry<String, Session> ent = itr.next();
							Session s = ent.getValue();
							if (!s.testIsActive()) {
								sessions.remove(ent.getKey());
								if (cLog.isInfoEnabled()) {
									cLog.info("Session " + s.token + " expired.");
								}
							}
						}
						Thread.sleep(10000);
					}
					catch (Exception e) {

					}
				}
			}
		};
		Thread runit = new Thread(sentry);
		runit.setDaemon(true);
		runit.setName("session-timeout-sentry");
		runit.start();
	}

	/**
	 * Sets the number of seconds of inactivity allowed before a session is
	 * terminated. Will only affect new sessions and any sessions updated for
	 * current activity.
	 * 
	 * @param seconds
	 */
	public void setSessionInactivityTimeoutSeconds(int seconds) {
		this.sessionInactivityTimeoutSeconds = seconds;
		this.sessionInactivityTimeoutMillis = 1000 * seconds;
	}

	/**
	 * Returns the number of seconds of inactivity allowed before a session is
	 * terminated.
	 * 
	 * @return
	 */
	public int getSessionInactivityTimeoutSeconds() {
		return this.sessionInactivityTimeoutSeconds;
	}

	public long getSessionInactivityTimeoutMillis() {
		return sessionInactivityTimeoutMillis;
	}

	/**
	 * Starts a new session and returns a token representing that session.
	 * 
	 * @param usr
	 * @return
	 */
	public String generateSessionToken(String usr) {
		Session s = new Session(this, usr);
		this.sessions.put(s.token, s);
		return s.token;
	}

	/**
	 * Returns true if the passed-in token matches an unexpired session.
	 * 
	 * @param token
	 * @return
	 */
	public boolean isValidToken(String token) {
		if (token == null) {
			return false;
		}
		Session s = sessions.get(token);
		return s != null && s.testIsActive();
	}

	/**
	 * Updates a session marking it as having been active and hence resetting
	 * its inactivity timeout counter. Not completely thread safe in terms of
	 * sessions expiring but we don't care for app wrap.
	 * 
	 * @param token
	 */
	public void markSessionAsActive(String token) {
		Session s = sessions.get(token);
		if (s != null) {
			s.markAsActive();
		}
	}

	/**
	 * Terminates the session associated with the indicated token.
	 * 
	 * @param token
	 */
	public void terminateSession(String token) {
		sessions.remove(token);
	}

	/**
	 * Terminates all current sessions.
	 */
	public void terminateAllSessions() {
		sessions.clear();
	}

	public Collection<Session> getSessions() {
		return sessions.values();
	}

}
