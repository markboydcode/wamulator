package org.lds.sso.appwrap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XmlConfigLoader2 {

	public static final String MACRO_PREFIX = "{{";
	public static final String MACRO_SUFFIX = "}}";
	
	public static void load(String xml) throws Exception {
		load(new StringReader(xml), "from String '" + xml + "'");
	}
	
	public static void load(Reader reader, String sourceInfo) throws Exception {
		XMLReader rdr;
		try {
			rdr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		}
		catch (Exception e) {
			throw new Exception("Unable to create parser for loading configuration '" 
					+ sourceInfo + "'.", e);
		}
		CfgContentHandler hndlr = new CfgContentHandler(); 
		rdr.setContentHandler(hndlr);
		InputSource src = new InputSource(reader);
		try {
			rdr.parse(src);
		}
		catch (Exception e) {
			throw new Exception("Unable to parse configuration '" 
					+ sourceInfo + "'.", e);
		}
	}
	
	/**
	 * Utility class that allows for simple testing for the full path of an 
	 * element during parsing.
	 * 
	 * @author Mark Boyd
	 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
	 *
	 */
	public static class Path {
		List<String> steps = new ArrayList<String>();
		private String compositePath = "/";
		
		/**
		 * Adds a step to the path being traversed.
		 * @param step
		 */
		public synchronized void add(String step) {
			steps.add(step);
			buildPath();
		}

		/**
		 * Builds a String of the steps currently in the path for speeding 
		 * comparisons.
		 * 
		 * @param step
		 */
		private synchronized void buildPath() {
			StringBuffer bfr = new StringBuffer();
			for (String step : steps) {
				bfr.append('/');
				bfr.append(step);
			}
			compositePath = bfr.toString();
			if ("".equals(compositePath)) {
				compositePath = "/";
			}
		}
		/**
		 * Removes the step from the end of the path if found there.
		 */
		public synchronized void remove(String step) {
			if (steps.size() == 0) {
				throw new IllegalStateException("Attempt to remove non-existent element from empty list.");
			}
			String last = steps.get(steps.size()-1); 
			if (! last.equals(step)) {
				throw new IllegalArgumentException("Step '" + step 
						+ "' does not match the last step in the path '" 
						+ last + "'. Can't remove.");
			}
			steps.remove(steps.size()-1);
			buildPath();
		}
		/**
		 * Tests to see if the passed-in path matches the value of this path.
		 * 
		 * @param path
		 * @return
		 */
		public boolean matches(String path) {
			return compositePath.equals(path);
		}
		
		/**
		 * Returns the composite path represented by this Path object.
		 */
		public String toString() {
			return compositePath;
		}
	}
	
	public static class CfgContentHandler implements ContentHandler {
		
		Map<String,String> aliases = new HashMap<String,String>();
		Map<String,String> conditions = new HashMap<String,String>();
		String currentConditionId = null; 
		StringBuffer characters = null;
		Config cfg = null;
		String site = null;
		protected Path path = new Path();

		public CfgContentHandler() {
			cfg = Config.getInstance();
		}
		
		/**
		 * Enables processing instructions in the XML having the following two
		 * forms. The value of the alias can be used in any attribute value by
		 * including the macro "{{name}}" within the attribute's string of 
		 * characters or within the value portion of any later occurring alias 
		 * declarations.
		 * 
		 * <pre>
		 * [1] &lt;?alias name=value?&gt;
		 * [2] &lt;?alias name=classpath:resource-file-path?&gt;
		 * </pre>
		 * 
		 * The first simply adds a named value pair to the map of alias. The 
		 * second form adds a named value to the map of alias where the value is
		 * the character content of the referenced resource. Making such file
		 * based content available is not accomplished by conceptually embedding
		 * such content within the XML declaring the alias and hence won't effect
		 * the xml processing. This allows for alias values to contain XML 
		 * sensitive characters and hence XML constructs. Such character content
		 * can also include macro references for earlier defined alias and will 
		 * be resolved upon loading.
		 */
		public void processingInstruction(String target, String data) throws SAXException {
			if (target.equals("alias")) {
				int eqIdx = data.indexOf('=');
				if (eqIdx >= 0) {
					String name = data.substring(0, eqIdx);
					String val = data.substring(eqIdx+1);
					if (val.toLowerCase().startsWith("classpath:")) {
						String resrc = val.substring("classpath:".length());
						ClassLoader cldr = this.getClass().getClassLoader();
						InputStream src = cldr.getResourceAsStream(resrc);
						
						if (src == null) {
							throw new IllegalArgumentException("Classpath alias resource '"
									+ resrc + "' not found.");
						}
						else {
							ByteArrayOutputStream bfr = new ByteArrayOutputStream();
							byte[] bytes = new byte[1024];
							
							
							int read;
							try {
								while ((read = src.read(bytes)) != -1) {
									bfr.write(bytes, 0, read);
								}
								bfr.flush();
							}
							catch (IOException e) {
								throw new SAXException("Unable to load content for alias '"
										+ name + "' from classpath resource '" 
										+ resrc + "'.", e);
							}
							val = bfr.toString().trim();
						}
					}
					val = resolveAliases(val);
					aliases.put(name, val);
				}
			}
		}

		public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
			characters = new StringBuffer();
			
			path.add(name);
			if (path.matches("/config")) {
				cfg.setConsolePort(getIntegerAtt("console-port", path, atts));
				cfg.setProxyPort(getIntegerAtt("proxy-port", path, atts));
			}
			else if (path.matches("/config/sso-cookie")) {
				cfg.setCookieName(getStringAtt("name", path, atts));
				cfg.setCookieDomain(getStringAtt("domain", path, atts));
			}
			else if (path.matches("/config/sso-sign-in-url")) {
				cfg.setSignInPage(getStringAtt("value", path, atts));
			}
			else if (path.matches("/config/sso-header")) {
				String hdrNm = getStringAtt("name", path, atts);
				String hdrVl = getStringAtt("value", path, atts);
				cfg.addGlobalHeader(hdrNm, hdrVl);
			}
			else if (path.matches("/config/sso-traffic/by-site")) {
				String scheme = atts.getValue("scheme");
				if (scheme == null || scheme.equals("")) {
					scheme = "http";
				}
				String host = getStringAtt("host", path, atts);
				int port = getIntegerAtt("port", path, atts);
				TrafficManager trafficMgr = cfg.getTrafficManager();
				SiteMatcher m = new SiteMatcher(scheme, host, port);
				trafficMgr.addMatcher(m);
			}
			else if (path.matches("/config/sso-traffic/by-site/cctx-mapping")) {
				String cctx = getStringAtt("cctx", path, atts); 
				String thost = getStringAtt("thost", path, atts);
				int tport = getIntegerAtt("tport", path, atts);
				String tpath = getStringAtt("tpath", path, atts);
				// enforce terminating asterisk solely for reminding readers of
				// the config file that these are uri root contexts that will be
				// rewritten. We could add support for regex like mod-rewrite.
				if (! cctx.endsWith("*") || ! tpath.endsWith("*")) {
					throw new IllegalArgumentException("cctx and tpath values must end with '*' in " + path);
				}
				cctx = cctx.substring(0, cctx.length()-1);
				tpath = tpath.substring(0, tpath.length()-1);
				TrafficManager trafficMgr = cfg.getTrafficManager();
				SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
				sm.addMapping(cctx, thost, tport, tpath);
			}
			else if (path.matches("/config/sso-traffic/by-site/unenforced")) {
				TrafficManager trafficMgr = cfg.getTrafficManager();
				SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
				String[] ur = getRelUriAtt("cpath", path, atts);
				UnenforcedUri uu = new UnenforcedUri(sm.getScheme(), sm.getHost(), sm.getPort(), ur[0], ur[1]);
				sm.addUnenforcedUri(uu);
			}
			else if (path.matches("/config/sso-traffic/by-site/allow")) {
				String cpath = getStringAtt("cpath", path, atts);
				String actionAtt = getStringAtt("action", path, atts);
				actionAtt = actionAtt.replace(" ", "");
				String[] actions = actionAtt.split(",");
				String cond = getCondition(path, atts);

				TrafficManager trafficMgr = cfg.getTrafficManager();
				SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
				// TODO inject condition name into TrafficManager,
				// TODO have TM parse condition using syntax engine,
				// TODO inject condition name into AllowedUri 
				// TODO have AllowedUri get compiled condition in isPermitted
				// call if condition name is found within it and evaluate 
				// condition for user.
				String[] ru = getRelUriAtt("cpath", path, atts);
				AllowedUri au = new AllowedUri(sm.getScheme(), sm.getHost(), sm.getPort(), ru[0], ru[1], actions);
				sm.addAllowedUri(au);
			}
			else if (path.matches("/config/sso-traffic/by-resource")) {
				URI u=getUriAtt("uri", path, atts);
				String host = u.getHost();
				int port = u.getPort();
				if (port == -1) { 
					port = 80; // will have to change to support 443 if ever
				}
				SiteMatcher sm = null;
				TrafficManager trafficMgr = cfg.getTrafficManager();
				String unenforcedAtt = atts.getValue("unenforced");

				if (unenforcedAtt != null) {
					UnenforcedUri uu = new UnenforcedUri(u.getScheme(), host, port, u.getPath(), u.getQuery());
					sm = new SiteMatcher(u.getScheme(), host, port, uu);
				}
				else {
					String actionAtt = atts.getValue("allow");
					if (actionAtt == null) {
						sm = new SiteMatcher(u.getScheme(), host, port);
					}
					else {
						actionAtt = actionAtt.replace(" ", "");
						String[] actions = actionAtt.split(",");
						AllowedUri au = new AllowedUri(u.getScheme(), host, port, u.getPath(), u.getQuery(), actions);
						sm = new SiteMatcher(host, port, au);
					}
				}
				trafficMgr.addMatcher(sm);
			}
			else if (path.matches("/config/users/user")) {
				String usrNm = getStringAtt("name", path, atts);
				String usrPwd = getStringAtt("pwd", path, atts);
				UserManager mgr = cfg.getUserManager();
				mgr.setUser(usrNm, usrPwd);
			}
			else if (path.matches("/config/users/user/sso-header")) {
				String hdrNm = getStringAtt("name", path, atts);
				String hdrVl = getStringAtt("value", path, atts);
				cfg.getUserManager().addHeaderForLastUserAdded(hdrNm, hdrVl);
			}
		}

		/**
		 * Validates condition attributes are a single macro for a defined 
		 * alias and returns the alias name.
		 *  
		 * @param pathToElement
		 * @param atts
		 * @return
		 */
	private String getCondition(Path pathToElement, Attributes atts) {
		String con = atts.getValue("condition");
		if (con == null || "".equals(con)) {
			return null;
		}
		if (! con.startsWith(MACRO_PREFIX) || ! con.endsWith(MACRO_SUFFIX)) {
			throw new IllegalArgumentException("Attribute 'condition' for "
					+ pathToElement 
					+ " must be a single macro for a defined alias. ex: '{{alias-name}}'.") ;
		}
		con = con.substring(MACRO_PREFIX.length(), con.length()-MACRO_SUFFIX.length());
		if (con.contains(MACRO_PREFIX) || 
				con.contains(MACRO_SUFFIX) ||
				this.aliases.get(con) == null) {
			throw new IllegalArgumentException("Attribute 'condition' for "
					+ pathToElement 
					+ " must be a single macro for a defined alias. ex: '{{alias-name}}'.") ;
		}
		return con;
	}

	private URI getUriAtt(String attName, Path pathToElement, Attributes atts) {
		String val = getStringAtt(attName,pathToElement,atts);
		val = resolveAliases(val);
		URI u = null;
		try {
			u = new URI(val);
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Unable to parse '" + attName 
					+ "' in " + path, e);
		}
		return u;
	}

	/**
	 * Gets an attribute purported to be a relative path url with optional
	 * query string parameter and returns a two element array with the path in
	 * index 0 and query in index 1. Path may be an empty string for a url of
	 * "?some/path" and query may be a null value for a url of "/some/path?"
	 * or "/some/path".
	 *  
	 * @param attName
	 * @param pathToElement
	 * @param atts
	 * @return
	 */
	private String[] getRelUriAtt(String attName, Path pathToElement, Attributes atts) {
		String val = getStringAtt(attName,pathToElement,atts);
		val = resolveAliases(val);
		String[] ur = new String[2];
		// ur[0] will hold path portion or null if not found
		// ur[1] will hold query portion or null if not found
		String[] vals = val.split("\\?");
		/* split returns the following:
		 * 1 "hello?there"  --> [hello, there]  path=hello, query=there
		 * 2 "hello-there"  --> [hello-there]   path=hello-there, query=null
		 * 3 "?hello-there" --> [, hello-there] path="", query=hello-there
		 * 4 "hello-there?" --> [hello-there]   path=hello-there, query=null
		 */
		ur[0] = vals[0];
		if (vals.length != 1) { // is 1 and 3, null otherwise
			ur[1]=vals[1];
		}
		return ur;
	}

	private String getStringAtt(String attName, Path pathToElement, Attributes atts) {
		String val = atts.getValue(attName);
		if ("".equals(val)) {
			throw new IllegalArgumentException("Attribute '" + attName + "' must be specified for " + pathToElement);
		}
		return resolveAliases(val);
	}

		public void endElement(String uri, String localName, String name) throws SAXException {
			path.remove(name);
			if (path.matches("")) {
				
			}
		}

		private int getIntegerAtt(String name, Path pathToElement, Attributes atts) {
			String val = atts.getValue(name);
			if (val == null) {
				throw new IllegalArgumentException("Attribute '" + name 
						+ "' must be specified for element " + pathToElement 
						+ ".");

			}
			val = resolveAliases(val);
			try {
				return Integer.parseInt(val);
			} catch(NumberFormatException n) {
				throw new IllegalArgumentException("Attribute '" + name 
						+ "' for element " + pathToElement 
						+ " must be an integer.");
			}
		}

		/**
		 * Resolves all references to {{token}} to values stored in the aliases
		 * map by "token" key. If not found then an illegal argument exception
		 * is thrown.
		 * 
		 * @param val
		 * @return
		 */
		String resolveAliases(String val) {
			int curIdx = 0;
			int leftCurlys = val.indexOf("{{");
			StringBuffer resolved = new StringBuffer();
			boolean foundEnd = false;
			
			while(leftCurlys != -1) {
				String text = val.substring(curIdx, leftCurlys);
				resolved.append(text);
				int rightCurlys = val.indexOf("}}", leftCurlys+2);
				if (rightCurlys == -1) {
					throw new IllegalArgumentException("Unmatched '}}' for alias in " + val);
				}
				String alias = val.substring(leftCurlys+2, rightCurlys);
				String value = aliases.get(alias);
				if (value == null) {
					throw new IllegalArgumentException("Can't resolve alias '" + alias + "' in " + val);
				}
				resolved.append(value);
				curIdx = rightCurlys + 2;
				if (curIdx >= val.length()) {
					foundEnd = true;
					break;
				}
				else {
					leftCurlys = val.indexOf("{{", curIdx);
				}
			}
			if (!foundEnd) {
				resolved.append(val.substring(curIdx));
			}
			
			return resolved.toString();
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			characters.append(ch, start, length);
		}

		public void endDocument() throws SAXException {
		}

		public void endPrefixMapping(String prefix) throws SAXException {
		}

		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		}

		public void setDocumentLocator(Locator locator) {
		}

		public void skippedEntity(String name) throws SAXException {
		}

		public void startDocument() throws SAXException {
		}

		public void startPrefixMapping(String prefix, String uri) throws SAXException {
		}
	}
}
