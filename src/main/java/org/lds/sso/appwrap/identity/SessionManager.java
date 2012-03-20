package org.lds.sso.appwrap.identity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.io.LogUtils;

public class SessionManager {
	private static final Logger cLog = Logger.getLogger(SessionManager.class.getName());

	/**
	 * Map of domain specific session maps.
	 */
    protected Map<String, DomainSessionsMapHolder> domainSessions = new TreeMap<String, DomainSessionsMapHolder>();
    
    /**
     * The list of declared cookie domains in documentation order.
     */
    protected List<String> cookieDomains = new ArrayList<String>();

	public static final int DEFAULT_INACTIVITY_TIMEOUT_SECONDS = 300;

	private int sessionInactivityTimeoutSeconds = DEFAULT_INACTIVITY_TIMEOUT_SECONDS;

	long sessionInactivityTimeoutMillis = DEFAULT_INACTIVITY_TIMEOUT_SECONDS * 1000;

	/**
	 * The domain in which the sign-in page will be served and act as the 
	 * single authentication authority among all configured cookie domains.
	 */
    private String masterCookieDomain = Config.DEFAULT_COOKIE_DOMAIN;

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
						synchronized(this) {
                            for (Iterator<Entry<String, DomainSessionsMapHolder>> itr = domainSessions.entrySet().iterator(); itr.hasNext();) {
                                Entry<String, DomainSessionsMapHolder> ent = itr.next();
                                DomainSessionsMapHolder holder = ent.getValue();
                                Map<String, Session> copy = copySessionContainer(ent.getKey());
                                
                                for (Iterator<Entry<String, Session>> sItr = copy.entrySet().iterator(); sItr.hasNext();) {
                                    Entry<String, Session> sEnt = sItr.next();
                                    Session s = sEnt.getValue();
                                    
                                    if (!s.isActive()) {
                                        sItr.remove();
                                        LogUtils.info(cLog, "Session {0} in domain {1} expired.", s.token, (String) ent.getKey());
                                    }
                                }
                                holder.sessions = copy;
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
     * Starts a new session in the specified domain and returns a token 
     * representing that session.
     * 
     * @param usr
     * @return
     */
    public synchronized String generateSessionToken(String usr, String host) {
        String ckDomain = getCookieDomainForHost(host);
        Map<String, Session> copy = copySessionContainer(ckDomain);
        Session s = new Session(this, usr);
        copy.put(s.token, s);
        domainSessions.get(ckDomain).sessions = copy;
        return s.token;
    }

    /**
     * Starts a new session in the master cookie domain for backwards compatibility.
     * Use generateSessionToken(user, domain) instead.
     * 
     * @deprecated
     * @param usr
     * @return
     */
    public synchronized String generateSessionToken(String usr) {
        Map<String, Session> copy = copySessionContainer(getMasterCookieDomain());
        Session s = new Session(this, usr);
        copy.put(s.token, s);
        domainSessions.get(getMasterCookieDomain()).sessions = copy;
        return s.token;
    }

	/**
	 * For the given host returns the cookie domain in which that host resides
	 * or which it matches exactly or throws an IllegalArgumentException.
	 * 
	 * @param host
	 * @return
	 * @throws IllegalArgumentException if a host is specifed that does not match
	 * or reside within a configured cookie domain. 
	 * 
	 */
	public String getCookieDomainForHost(String host) {
        for(String domain:cookieDomains) {
            if (domain.startsWith(".")) {
                if (host.endsWith(domain)) {
                    return domain;
                }
            }
            else if (host.equals(domain)) {
                return domain;
            }
        }
        throw new IllegalArgumentException("No cookie domain is configured for host '" + host + "'");
    }

    /**
     * Returns true if the passed-in token matches an unexpired session.
     * 
     * @param token
     * @return
     */
    public boolean isValidToken(String token, String host) {
        if (token == null) {
            return false;
        }
        try {
            String domain = getCookieDomainForHost(host);
            Session s = domainSessions.get(domain).sessions.get(token);
            return s != null && s.isActive();
        }
        catch (IllegalArgumentException a) {
            // can't find cookie domain for specified host
            return false;
        }
    }

    /**
     * Returns true if the passed-in token matches an unexpired session in the
     * master cookie domain for backwards compatibility. Use 
     * isValidToken(token, host) instead.
     * 
     * @deprecated
     * @param token
     * @return
     */
    public boolean isValidToken(String token) {
        if (token == null) {
            return false;
        }
        for(String domain : cookieDomains) {
            Session s = domainSessions.get(domain).sessions.get(token);
            return s != null && s.isActive();
        }
        return false;
    }

    /**
     * Updates a session marking it as having been active and hence resetting
     * its inactivity timeout counter. Not completely thread safe in terms of
     * sessions expiring but we don't care for app wrap.
     * 
     * @param token
     */
    public void markSessionAsActive(String token, String host) {
        try {
            String domain = getCookieDomainForHost(host);
            markSessionAsActiveInDomain(token, domain);
        } catch (IllegalArgumentException a) {
            cLog.info("Unable to mark session active since can't find configured "
                + "cookie domain for host '" + host + "'");
        }
    }

    /**
     * Updates a session marking it as having been active and hence resetting
     * its inactivity timeout counter. Not completely thread safe in terms of
     * sessions expiring but we don't care for app wrap.
     * 
     * @param token
     */
    public void markSessionAsActiveInDomain(String token, String cookieDomain) {
        Map<String, Session> sessions = domainSessions.get(cookieDomain).sessions;
        if (sessions == null) {
            cLog.info("Unable to mark session active since cookie domain '" 
                    + cookieDomain + "' is not defined.");
        }
        Session s = domainSessions.get(cookieDomain).sessions.get(token);
        if (s != null) {
            s.markAsActive();
        }
    }

	/**
	 * Makes a deep copy of the domainSessions object.
	 * 
	 * @return
	 */
	Map<String, Session> copySessionContainer(String cookieDomain) {
		Map<String, Session> copy = new TreeMap<String, Session>();
		copy.putAll(domainSessions.get(cookieDomain).sessions);
		return copy;
	}

	/**
	 * Terminates the session associated with the indicated token.
	 * 
	 * @param token
	 */
	public synchronized void terminateSession(String token, String cookieDomain) {
		Map<String, Session> copy = copySessionContainer(cookieDomain);
		copy.remove(token);
		domainSessions.get(cookieDomain).sessions = copy;
	}

	/**
	 * Terminates all current sessions by replacing the session container to 
	 * avoid concurrent modification exceptions.
	 */
	public void terminateAllSessions() {
	    for(DomainSessionsMapHolder holder : domainSessions.values()) {
	        holder.sessions = new TreeMap<String, Session>();
	    }
	}

	/**
	 * Returns a collection of active sessions for the given cookie domain.
	 *  
	 * @param cookieDomain
	 * @return
	 */
	public Collection<Session> getSessions(String cookieDomain) {
	    DomainSessionsMapHolder holder = domainSessions.get(cookieDomain);
		return Collections.unmodifiableCollection(holder.sessions.values());
	}
	
	public Collection<String> getCookieDomains() {
	    return Collections.unmodifiableList(cookieDomains);
	}

	/**
	 * Holder of a session map to prevent concurrent modification exceptions
	 * when creating new new sessions.
	 * 
	 * @author BoydMR
	 *
	 */
	public class DomainSessionsMapHolder {
	    public Map<String, Session> sessions = null;
	}

	/**
	 * Used during parsing of configuration file to add a cookie domain as 
	 * declared via sso-cookie directives.
	 * 
	 * @param cookieName
	 * @param cookieDomain
	 */
	public void addCookieDomain(String cookieDomain) {
	    if (domainSessions.containsKey(cookieDomain)) {
	        throw new IllegalArgumentException("Adding cookie domain '" 
	                + cookieDomain + "' that was already added.");
	    }
	    cookieDomains.add(cookieDomain);
	    TreeMap<String, Session> map = new TreeMap<String, Session>();
	    DomainSessionsMapHolder holder = new DomainSessionsMapHolder();
	    holder.sessions = map;
	    domainSessions.put(cookieDomain, holder);
	}

	/**
	 * Sets the cookie domain that is considered the domain in which the single
	 * authentication authority sign-in page will be exposed and to which all
	 * other cross domain single sign-on configured domains will redirect.
	 * 
	 * @param domain
	 */
    public void setMasterCookieDomain(String domain) {
        masterCookieDomain = domain;
        
    }

    /**
     * Gets the cookie domain of the configured master cookie domain.
     * 
     * @return
     */
    public String getMasterCookieDomain() {
        return masterCookieDomain;
        
    }

    public void addSessionViaCdsso(String tokenForCdsso,
            String host) {
        String master = getMasterCookieDomain();
        DomainSessionsMapHolder holder = domainSessions.get(master);
        Session masterSession = holder.sessions.get(tokenForCdsso);
        Session s = masterSession.copy();

        String ckDomain = getCookieDomainForHost(host);
        Map<String, Session> copy = copySessionContainer(ckDomain);
        copy.put(s.token, s);
        domainSessions.get(ckDomain).sessions = copy;
    }

    /**
     * Passes through all domains terminating sessions for this token.
     * 
     * @param token
     */
	public void terminateSessionsForToken(String token) {
		for(DomainSessionsMapHolder holder : domainSessions.values()) {
			Session s = holder.sessions.get(token);
			if (s != null) {
				Map<String, Session> copy = new TreeMap<String, Session>();
				copy.putAll(holder.sessions);
				copy.remove(token);
				holder.sessions = copy;
			}
		}
	}

	/**
	 * Returns the session represented by the token if such a session exists
	 * and if the session is still active. Returns null otherwise. 
	 * 
	 * @param token
	 * @return
	 */
	public Session getSessionForToken(String token) {
        if (token == null) {
            return null;
        }
        for(String domain : cookieDomains) {
            Session s = domainSessions.get(domain).sessions.get(token);
            if (s != null && s.isActive()) {
            	return s;
            }
        }
        return null;
	}
}
