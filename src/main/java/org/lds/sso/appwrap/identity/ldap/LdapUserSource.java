package org.lds.sso.appwrap.identity.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.identity.ExternalUserSource;
import org.lds.sso.appwrap.identity.User;
import org.lds.sso.appwrap.identity.UserManager;

/**
 * Loads user attributes from the ldap store pointed to by {@link LdapStore}. 
 * 
 * @author BoydMR
 *
 */
public class LdapUserSource implements ExternalUserSource {
	private static final Logger cLog = Logger.getLogger(LdapUserSource.class.getName());
	
	private UserManager users = null;

	/**
	 * Authenticates the user and Loads the configured set of attributes
	 * replacing old values and creating a new {@link User} if the user is not
	 * yet found.
	 */
	@Override
	public Response loadExternalUser(String username, String password) throws IOException {
		Map<String, List<String>> atts;
		try {
			atts = LdapStore.getUserAttributes(username, password);
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
		User user = users.setUser(username, password);
		user.clearAttributes();
		
        for (Map.Entry<String, List<String>> ent : atts.entrySet()) {
        	for(String val : ent.getValue()) {
                user.addAttribute(ent.getKey(), val);
        	}
        }
        return Response.UserInfoLoaded;
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

        LdapStore.setEnv(searchBase, dn,pwd, url, list);
        
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
