package org.lds.sso.appwrap.identity.legacy;

import static org.lds.sso.appwrap.XmlConfigLoader2.getStringAtt;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.XmlConfigLoader2;
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.identity.ExternalUserSource;
import org.lds.sso.appwrap.identity.User;
import org.lds.sso.appwrap.identity.UserManager;
import org.lds.sso.appwrap.io.SimpleErrorHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * User source that is created if the configuration file contains any of the
 * legacy &lt;user&gt; declarations nested within the &lt;users&gt; element
 * in the configuration file.
 * 
 * @author BoydMR
 *
 */
public class WamulatorUserSource implements ExternalUserSource, ContentHandler {
	private static final Logger cLog = Logger.getLogger(WamulatorUserSource.class.getName());
	
	/**
	 * The {@link XmlConfigLoader2.Path} beneath which events are routed to
	 * this object for backwards compatibility of legacy user config.
	 */
	public static final String LEGACY_USER_DECL_PATH = "/config/users/user";

	private UserManager userManager;
	protected Map<String, List<String>> uniqueAttVals = new HashMap<String, List<String>>();

	private Path path = new Path();

	@Override
	public void setUserManager(UserManager umgr) {
		this.userManager = umgr;
	}

	@Override
	public void setConfig(Path path, Properties props) throws ConfigurationException {
		String uniques = (String) props.get("enforce-uniqueness");
		if (! StringUtils.isEmpty(uniques)) {
			for(String att : uniques.split(",")) {
				att = att.trim();
				if (! StringUtils.isEmpty(att)) {
					uniqueAttVals.put(att, new ArrayList<String>());
				}
			}
		}
        String xml = props.getProperty("xml");
        
        if (StringUtils.isEmpty(xml)) {
        	cLog.log(Level.WARNING, "No Configuration for xml user-source at " 
        			+ WamulatorUserSource.class.getSimpleName()
        			+ " at " + path
        			+ ". No Users will be available from this source.");
        	return;
        }
        parseXml(path, xml);
	}

	/**
	 * Parses the legacy user declaration format.
	 * 
	 * @param path
	 * @param xml
	 * @throws ConfigurationException 
	 */
	private void parseXml(Path path, String xml) throws ConfigurationException {
        XMLReader rdr;
        try {
        	SAXParserFactory factory = SAXParserFactory.newInstance();
        	SAXParser parser = factory.newSAXParser();
            rdr = parser.getXMLReader();
        } catch (Exception e) {
            cLog.log(Level.SEVERE, "Unable to create parser for loading user source users.", e);
            return;
        }
        rdr.setContentHandler(this);
        InputSource src = new InputSource(new StringReader(xml));
        try {
        	rdr.setErrorHandler(new SimpleErrorHandler(cLog));
            rdr.parse(src);
        } catch (Exception e) {
        	throw new ConfigurationException("Unable to load users for user-source of type 'xml' at " + path, e);
        }
	}

	@Override
	public Response loadExternalUser(String username, String password) throws IOException {
        User usr = userManager.getUser(username);
        
        if (usr != null) {
        	return Response.UserInfoLoaded;
        }
        return Response.UserNotFound;
	}
	
	/////////////////// ContentHandler Methods /////////////////////////
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
		Config cfg = Config.getInstance();
        path.add(name);

		if (path.matches("/users/user")) {
			String usrNm = getStringAtt("name", path, atts);
			String usrPwd = getStringAtt("pwd", path, atts, false);
			UserManager mgr = cfg.getUserManager();
			mgr.setUser(usrNm, usrPwd);
		}
		else if (path.matches("/users/user/sso-header") 
				|| path.matches("/users/user/ldsApplications")) {
        	// break backwards compatibility of legacy /config/users/sso-header 
        	// indicate use of /users/user/att in combination with 
			// /config/cctx-mapping/headers/profile-att instead
        	throw new IllegalArgumentException("The <users> element for "
        			+ "the xml user-source does not support the <sso-header> " 
        			+ "or <ldsApplications> elements. Use <att> instead to inject attributes into "
        			+ "users and <cctx-mapping>/<headers>/<profile-att> to "
        			+ "inject a header for that attribute for "
        			+ "traffic headed to the application fronted by that "
        			+ "cctx-mapping directive. See wiki documentation for details.");
		}
		else if (path.matches("/users/user/att")) {
			String nl = getStringAtt("name", path, atts);
			String vl = getStringAtt("value", path, atts);
			List<String> attValues = uniqueAttVals.get(nl);
			if (attValues != null) { // so enforcing uniqueness, are we unique?
				if (attValues.contains(vl)) {
					throw new IllegalArgumentException("Uniqueness Constraint for attribute '" + nl
							+ "' violated. A second value of '" + vl + "' was encountered.");
				}
				attValues.add(vl);
			}

			cfg.getUserManager().addAttributeForLastUserAdded(nl, vl);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
        path.remove(qName);
	}

	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	public void startDocument() throws SAXException {
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
	}
}
