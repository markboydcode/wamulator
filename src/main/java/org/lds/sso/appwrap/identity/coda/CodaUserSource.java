package org.lds.sso.appwrap.identity.coda;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
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
	 * Facilitates testing by isolating calls to coda and allowing use to return
	 * both the status code and the raw payload content.
	 *  
	 * @author BoydMR
	 *
	 */
	public static class Results {
		public String content = null;
		public int status = -1;
		
		public Results(int status, String content) {
			this.status = status;
			this.content = content;
		}
	}
	
	/**
	 * Calls to CODA each time called and replaces any existing headers
	 * previously loaded for the user if any. If the user does not yet exists
	 * then a new user is created in UserManager. Returns true if the user was
	 * found regardless of how many headers were loaded.
	 */
	@Override
	public Response loadExternalUser(String username, String password) throws IOException {
        String urlencUser = URLEncoder.encode(username, "utf-8");
        String uri = sourceUri.replaceAll("\\{username\\}", urlencUser);

        Results res = callCoda(uri);

		// TODO: implement aggregation strategy per attribute heres
        // TODO: implement stopOnFound here

        if (res.status != 200) {
            LogUtils.severe(cLog, "Non 200 response code recieved from {0}, content returned: {1}", uri, res.content);
        	return Response.ErrorAccessingSource;
        }
        CodaServiceResponseParser parser = new CodaServiceResponseParser(res.content);
        Map<String, String> userAtts = parser.getValues();
        
        /**
         * verify the user was found via failed requests having:
         * 
         * <org.lds.community.data.ws.dto.ResponseStatus>
         *   <status>200</status>
         *   <good>false</good>
         *   <message>ngienglishbishop</message>
         * </org.lds.community.data.ws.dto.ResponseStatus>
         * 
         * Whereas successful responses look like:
         * 
         * <org.lds.community.data.ws.dto.OssoMemberDto>
         *   <birthdate>1980-03-31</birthdate>
         *   <cn>pholder</cn>
         *   <gender>M</gender>
         *   <givenName>Perry</givenName>
         *   <individualId>196591</individualId>
         *   <ldsAccountId>196591</ldsAccountId>
         *   <ldsMrn>8890019004079</ldsMrn>
         *   <positions>p57/7u555005/5u555001/</positions>
         *   <preferredLanguage>en</preferredLanguage>
         *   <preferredName>Perry Holder</preferredName>
         *   <sn>Holder</sn>
         *   <status>200</status>
         *   <units>/7u555005/5u555001/</units>
         * </org.lds.community.data.ws.dto.OssoMemberDto>
         * 
         */
        if ("false".equalsIgnoreCase(userAtts.get("good"))) {
    		return Response.UserNotFound;
        }
        userAtts.put("policy-coda-status", "200-coda user atts retrieved");

        User user = userManager.setUser(username, password);
        for (Entry<String, String> ent : userAtts.entrySet()) {
            user.addAttributeValues(ent.getKey(), new String[] {ent.getValue()});
        }
		return Response.UserInfoLoaded;
	}

	/**
	 * Encapsulates the call to coda and getting the raw results enabling unit
	 * testing without having a remote coda instance or socket service.
	 * 
	 * @param uri
	 * @return
	 * @throws IOException 
	 * @throws HttpException 
	 */
	protected Results callCoda(String uri) throws HttpException, IOException {
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        method.setRequestHeader("Accept", "application/xml");
        int status = client.executeMethod(method);
        String content = method.getResponseBodyAsString();
        method.releaseConnection();
		return new Results(status, content);
	}

	@Override
	public void setUserManager(UserManager umgr) {
		this.userManager = umgr;
	}

	@Override
	public void setConfig(Path path, Properties config) throws ConfigurationException {
		boolean hasUrl = config.containsKey("url");
		String url = config.getProperty("url");
		if (url == null) {
			throw new ConfigurationException("Configuration for CODA user-source at "
					+ path + " is missing the 'url' property. Configuration must have form "
					+ "url=<fully-qualified-coda-http-URL-with-embedded-{username}-macro>.");
		}
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
