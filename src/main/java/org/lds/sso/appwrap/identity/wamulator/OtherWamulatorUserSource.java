package org.lds.sso.appwrap.identity.wamulator;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.identity.ExternalUserSource;
import org.lds.sso.appwrap.identity.UserManager;

public class OtherWamulatorUserSource implements ExternalUserSource {

	private static final Logger cLog = Logger.getLogger(OtherWamulatorUserSource.class.getName());
	
	/**
	 * Endpoint location of handler in http console where this source obtains
	 * its user and session information.
	 */
	public static final String WAMULATOR_SOURCE_REST_ENDPOINT = "/admin/rest/wamulator/user-source";

	private static final String PROPER_IS_ALIVE_RESPONSE = "Yes";
	
	/**
	 * String representing the location of this user-source in the configuration
	 * XML in xpath-like syntax including position. For example, if this is
	 * the third user-source object beneath the config element the path would
	 * be:
	 * <pre>
	 *   /config/user-source[3]
	 * </pre>
	 */
	private String path;

	/**
	 * The URI of the remote wamulator user-source endpoint from which session
	 * data is validated and user attributes propagated.
	 */
	private String uri;
	
	@Override
	public void setUserManager(UserManager umgr) {

	}

	@Override
	public void setConfig(Path path, Properties config) throws ConfigurationException {
		this.path = path.toString();
		String hostAndPort = config.getProperty("location");
		if (hostAndPort == null) {
			throw new ConfigurationException("Configuration for wamulator user-source at "
					+ this.path + " must have form "
					+ "location=<host:port> where location points to the " 
					+ "console port of the remote wamulator instance.");
		}
		int colonIdx = hostAndPort.indexOf(":");
		if (colonIdx == -1) {
			throw new ConfigurationException("Configuration for wamulator user-source at "
					+ this.path + " must have form "
					+ "location=host:port where location points to the " 
					+ "console port of the remote wamulator instance.");
		}
        HttpClient client = new HttpClient();
        this.uri = "http://" + hostAndPort + WAMULATOR_SOURCE_REST_ENDPOINT;
        HttpMethod method = new GetMethod(uri + "?action=isAlive");
        method.setFollowRedirects(false);
        int status = -1;
		try {
			status = client.executeMethod(method);
		}
		catch (HttpException e1) {
        	cLog.log(Level.SEVERE, "Unable to access "
        			+ uri + " for wamulator user-source at "
        			+ this.path + ". User/Sessions may not be available.", e1);
        	return;
		}
		catch (IOException e1) {
        	cLog.log(Level.SEVERE, "Unable to access "
        			+ uri + " for wamulator user-source at "
        			+ this.path + ". User/Sessions may not be available.", e1);
        	return;
		}
        method.releaseConnection();
        
        if (status != 200) {
        	cLog.log(Level.WARNING, "Received http response code "
        			+ status + " when connecting to "
        			+ uri + " for wamulator user-source at "
        			+ this.path + ".");
        }
        else {
            String content;
			try {
				content = method.getResponseBodyAsString();
			}
			catch (IOException e) {
            	cLog.log(Level.WARNING, "Unable to access response content of "
            			+ uri + " for wamulator user-source at "
            			+ this.path + ". User/Sessions may not be available.", e);
            	return;
			}
            if (content.equalsIgnoreCase(PROPER_IS_ALIVE_RESPONSE)) {
            	cLog.log(Level.INFO, "Successfully Connected to "
            			+ hostAndPort + " for wamulator user-source at "
            			+ this.path + ".");
            }
            else {
            	cLog.log(Level.WARNING, "Unrecognized Response '"
            			+ content + "' from "
            			+ uri + " when connecting for wamulator user-source at "
            			+ this.path + ".");
            }
        }
	}

	@Override
	public Response loadExternalUser(String username, String password) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
