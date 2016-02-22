package org.lds.sso.appwrap.identity;

import org.lds.sso.appwrap.XmlConfigLoader2.Path;

import java.io.IOException;
import java.util.Properties;


/**
 * An external source of {@link User} objects created or loaded at sign-in 
 * and injected into {@link UserManager} by {@link #loadExternalUser(String, String)}.
 * 
 * @author BoydMR
 *
 */
public interface ExternalUserSource {

	/**
	 * Fixed value for creating string arrays from lists via List.toArray(STRING_ARRY).
	 */
	public static final String[] STRING_ARRAY = new String[] {};

	/**
	 * Called immediately after instantiation to hand the wamulator's container
	 * of users {@link UserManager} to the store for injecting users as they
	 * are loaded.
	 * 
	 * @param umgr
	 */
	public void setUserManager(UserManager umgr);
	
	/**
	 * Sets the Properties object loaded from the character content found within
	 * the source's declaring &lt;user-source&gt; element allowing the source to
	 * embed character content configuration within the wamulator's
	 * configuration file provided it isn't xml content so that it doesn't
	 * conflict with the wamulator's schemas. However, macros embedded within
	 * the property values will be resolved after loading into the Properties
	 * object allowing XML to be injected via a property.
	 * 
	 * @param config
	 */
	public void setConfig(Path path, Properties config) throws ConfigurationException;
	
	/**
	 * Delegates to whatever external source is supported by the implementation
	 * to potentially authenticate the user and load the user's attributes. The
	 * UserManager is not checked beforehand to determine if the user is 
	 * already loaded. If cached values are desired rather than delegating to
	 * the external source for every request that must be implemented in the
	 * provider. Furthermore, a single instance of the implementation may be used by 
	 * multiple threads so thread safety should be carefully considered.
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	public Response loadExternalUser(String username, String password)
	throws IOException;
	
	
	/**
	 * Possible responses from calls to  
	 * {@link ExternalUserSource#loadExternalUser(String, String)}.
	 * 
	 * @author BoydMR
	 *
	 */
	public enum Response {
		ErrorAccessingSource,
		UnableToAuthenticate,
		UserNotFound,
		UserInfoLoaded;
	}

	
	/**
	 * Exception thrown when the store is unable to
	 * configure itself.
	 * 
	 * @author BoydMR
	 *
	 */
	public static class ConfigurationException extends Exception {

		public ConfigurationException(String msg, Exception e) {
			super(msg, e);
		}
		
		public ConfigurationException(String msg) {
			super(msg);
		}
	}
}
