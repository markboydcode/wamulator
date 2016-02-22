package org.lds.sso.appwrap.identity.ldap;

import org.apache.commons.lang.StringUtils;
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.identity.ExternalUserSource;
import org.lds.sso.appwrap.identity.User;
import org.lds.sso.appwrap.identity.UserManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads user attributes from the ldap store pointed to by {@link LdapStore} fully replacing any such user already
 * found in UserManager.
 * 
 * @author BoydMR
 *
 */
public class LdapUserSource implements ExternalUserSource {
	private static final Logger cLog = Logger.getLogger(LdapUserSource.class.getName());
	
	private UserManager users = null;

	/**
	 * Defines the aggregation strategy for loading user attributes. Defaults to MERGE.
	 */
	private UserManager.Aggregation aggregationStrategy = UserManager.Aggregation.MERGE;

	/**
	 * Authenticates the user and Loads the configured set of attributes
	 * replacing old values and creating a new {@link User} if the user is not
	 * yet found.
	 */
	@Override
	public Response loadExternalUser(String username, String password) throws IOException {
		Map<String, List<String>> atts;
		try {
			atts = callLdap(username, password);
		}
		catch (UnableToConnecToLdap e) {
			cLog.log(Level.SEVERE, "Unable to load external user attributes.", e);
			return Response.ErrorAccessingSource;
		}
		catch (UnableToBindSearchUser e) {
			cLog.log(Level.SEVERE, "Unable to load external user attributes.", e);
			return Response.ErrorAccessingSource;
		}
		catch (UserNotFound e) {
			return Response.UserNotFound;
		}
		catch (UnableToSearchForUser e) {
			cLog.log(Level.SEVERE, "Unable to load external user attributes.", e);
			return Response.ErrorAccessingSource;
		}
		catch (UnableToBindEndUser e) {
			return Response.UnableToAuthenticate;
		}
		catch (UnableToGetUserAttributes e) {
			cLog.log(Level.SEVERE, "Unable to load external user attributes.", e);
			return Response.ErrorAccessingSource;
		}
		catch (UnableToLoadUserAttributes e) {
			cLog.log(Level.SEVERE, "Unable to load external user attributes.", e);
			return Response.ErrorAccessingSource;
		}
		users.setUser(username, password, atts, aggregationStrategy);
        return Response.UserInfoLoaded;
	}

	/**
	 * Provides for testability by a subclass mocking up responses without 
	 * incurring calls to ldap infrastructure.
	 * 
	 * @param username
	 * @param password
	 * @return
	 * @throws UnableToLoadUserAttributes 
	 * @throws UnableToGetUserAttributes 
	 * @throws UnableToBindEndUser 
	 * @throws UnableToSearchForUser 
	 * @throws UserNotFound 
	 * @throws UnableToBindSearchUser 
	 * @throws UnableToConnecToLdap 
	 */
	protected Map<String, List<String>> callLdap(String username, String password) throws UnableToConnecToLdap,
			UnableToBindSearchUser, UserNotFound, UnableToSearchForUser, UnableToBindEndUser,
			UnableToGetUserAttributes, UnableToLoadUserAttributes {
		return LdapStore.getUserAttributes(username, password);
	}

	/**
	 * Provides for testability by a subclass mocking up responses without 
	 * incurring calls to ldap infrastructure.
	 * 
	 * @param searchBase
	 * @param dn
	 * @param pwd
	 * @param url
	 * @param enableTls
	 * @param list
	 */
	protected void testLdap(String searchBase, String dn, String pwd, String url, boolean enableTls, String[] list) {
        LdapStore.setEnv(searchBase, dn,pwd, url, enableTls, list);
	}


	@Override
	public void setUserManager(UserManager umgr) {
		this.users = umgr;
	}

	@Override
	public void setConfig(Path path, Properties props) throws ConfigurationException {
        String searchBase = validate(path, props, "search-base-dn");
        String dn = validate(path, props, "search-bind-dn");
        String pwd = validate(path, props, "search-bind-pwd");
        String url = validate(path, props, "url");
        // tls is used by default. must proactively disable by including this property.
        boolean disableTls = Boolean.parseBoolean(props.getProperty("disable-tls", "false"));

        String atts = props.getProperty("attributes");
        String[] list = null;
        
        if (! StringUtils.isEmpty(atts)) {
        	list = atts.split(",");
        	List<String> trimmed = new ArrayList<String>();
        	for (String raw : list) {
        		String clean = raw.trim();
        		if (StringUtils.isNotEmpty(clean)) {
        			trimmed.add(clean);
        		}
        	}
        	list = trimmed.toArray(new String[] {});
        	if (list.length == 0) {
        		list = null;
        	}
        }

		// see if we are overwriting the default aggregationStrategy for attribute loading
		String aggregation = props.getProperty("aggregation");

		if (UserManager.Aggregation.REPLACE.name().equals(aggregation)) {
			this.aggregationStrategy = UserManager.Aggregation.REPLACE;
		}

        testLdap(searchBase, dn, pwd, url, ! disableTls, list);
	}

	/**
	 * Extracts from the properties file the attribute throwing a 
	 * {@link ExternalUserSource.ConfigurationException} if not found or if it
	 * is empty.
	 * @param path 
	 * 
	 * @param props
	 * @param propName
	 * @return
	 * @throws ConfigurationException 
	 */
	private String validate(Path path, Properties props, String propName) throws ConfigurationException {
		String val = (String) props.get(propName);
		if (StringUtils.isEmpty(val)) {
			throw new ConfigurationException("Ldap user-source at " + path
					+ " requires configuration property "  + propName + ".");
		}
		return val;
	}
}
