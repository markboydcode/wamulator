package org.lds.sso.appwrap.identity.coda;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.identity.ExternalUserSource;
import org.lds.sso.appwrap.identity.User;
import org.lds.sso.appwrap.identity.UserManager;
import org.lds.sso.appwrap.io.LogUtils;

/**
 * Loads user headers from CODA to support backwards compatibility. The new
 * ldap approach is a better alternative since it loads attributes and not
 * headers directly.
 * 
 * @author BoydMR
 *
 */
public class CodaUserSource implements ExternalUserSource {
	private static final Logger cLog = Logger.getLogger(CodaUserSource.class.getName());

	/**
	 * The coda URI as outlined at http://tech.lds.org/wiki/SSO_Simulator_Configuration_Files#.3Cusers.3E.
	 */
	private String sourceUri;

	private UserManager userManager;
	
	/**
	 * Calls to CODA each time called and replaces any existing headers
	 * previously loaded for the user if any. If the user does not yet exists
	 * then a new user is created in UserManager. Returns true if the user was
	 * found regardless of how many headers were loaded.
	 */
	@Override
	public Response loadExternalUser(String username, String password) throws IOException {
        HttpClient client = new HttpClient();
        String urlencUser = URLEncoder.encode(username, "utf-8");
        String uri = sourceUri.replaceAll("\\{username\\}", urlencUser);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        method.setRequestHeader("Accept", "application/xml");
        int status = client.executeMethod(method);
        String content = method.getResponseBodyAsString();

        if (status != 200) {
            LogUtils.severe(cLog, "Non 200 response code recieved from {0}, content returned: {1}", uri, content);
        	return Response.ErrorAccessingSource;
        }
        CodaServiceResponseParser parser = new CodaServiceResponseParser(content);
        Map<String, String> userAtts = parser.getValues();
        userAtts.put("policy-coda-status", "200-coda user atts retrieved");

        User user = userManager.setUser(username, password);
        for (Iterator<Map.Entry<String, String>> itr = userAtts.entrySet().iterator(); itr.hasNext();) {
            Map.Entry<String, String> ent = itr.next();
            user.addAttribute(ent.getKey(), ent.getValue());
        }
        method.releaseConnection();
		return Response.UserInfoLoaded;
	}

	@Override
	public void setUserManager(UserManager umgr) {
		this.userManager = umgr;
	}

	@Override
	public void setConfig(Path path, Properties config) throws ConfigurationException {
		boolean hasUrl = config.containsKey("url");
		String url = config.getProperty("url");
		boolean hasMacro = url.contains("{username}");
		boolean usesHttp = url.startsWith("http://") || url.startsWith("https://");
		
		if (! hasUrl || ! hasMacro || ! usesHttp) {
			throw new ConfigurationException("Configuration for CODA user-source at "
					+ path + " must have form "
					+ "url=<fully-qualified-coda-http-URL-with-embedded-{username}-macro>.");
		}
		this.sourceUri = config.getProperty("url");
	}
}
