package org.lds.sso.appwrap;

import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.xml.sax.Attributes;

public class XmlConfigLoader2 {

	
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
		Config cfg = new Config();
		String site = null;
		protected Path path = new Path();

		public CfgContentHandler() {
			
		}
		
		public void processingInstruction(String target, String data) throws SAXException {
			if (target.equals("alias")) {
				int eqIdx = data.indexOf('=');
				if (eqIdx >= 0) {
					String name = data.substring(0, eqIdx);
					String val = data.substring(eqIdx+1);
					val = resolveAliases(val);
					aliases.put(name, val);
				}
			}
		}

		public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
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
				String host = getStringAtt("host", path, atts);
				int port = getIntegerAtt("port", path, atts);
				TrafficManager trafficMgr = cfg.getTrafficManager();
				SiteMatcher m = new SiteMatcher(host, port);
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
				String cpath = getStringAtt("cpath", path, atts);
				TrafficManager trafficMgr = cfg.getTrafficManager();
				SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
				sm.getHost();
				sm.getPort();
				UnenforcedUri uu = new UnenforcedUri(sm.getHost(), sm.getPort(), cpath);
				sm.addUnenforcedUri(uu);
			}
			else if (path.matches("/config/sso-traffic/by-site/allow")) {
				String cpath = getStringAtt("cpath", path, atts);
				String actionAtt = getStringAtt("action", path, atts);
				actionAtt = actionAtt.replace(" ", "");
				String[] actions = actionAtt.split(",");
				TrafficManager trafficMgr = cfg.getTrafficManager();
				SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
				AllowedUri au = new AllowedUri(sm.getHost(), sm.getPort(), cpath, actions);
				sm.addAllowedUri(au);
			}
			else if (path.matches("/config/sso-traffic/by-resource")) {
				String url = getStringAtt("url", path, atts);
				URL u;
				try {
					u = new URL(url);
				}
				catch (MalformedURLException e) {
					throw new IllegalArgumentException("MalFormed URL '" + url + "' in " + path);
				}
				String host = u.getHost();
				int port = u.getPort();
				if (port == -1) { 
					port = 80; // will have to change to support 443 if ever
				}
				SiteMatcher sm = null;
				TrafficManager trafficMgr = cfg.getTrafficManager();
				String unenforcedAtt = atts.getValue("unenforced");

				/*
				 * Needs work. Do we need site matchers that lock to only one
				 * url versus others that allow all traffic and how does that 
				 * affect the clarity of the config file syntax?
				 */
				if (unenforcedAtt != null) {
					UnenforcedUri uu = new UnenforcedUri(host, port, u.getPath());
					sm = new SiteMatcher(host, port, uu);
				}
				else {
					String actionAtt = atts.getValue("allowed");
					if (actionAtt == null) {
						sm = new SiteMatcher(host, port);
					}
					else {
						actionAtt = actionAtt.replace(" ", "");
						String[] actions = actionAtt.split(",");
						AllowedUri au = new AllowedUri(host, port, u.getPath(), actions);
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
			else if (path.matches("/config/users/user/allow")) {
				String actionAtt = getStringAtt("action", path, atts);
				actionAtt = actionAtt.replace(" ", "");
				String[] actions = actionAtt.split(",");
				String url = getStringAtt("url", path, atts);
				URL u;
				try {
					u = new URL(url);
				}
				catch (MalformedURLException e) {
					throw new IllegalArgumentException("MalFormed URL '" + url + "' in " + path);
				}
				String host = u.getHost();
				int port = u.getPort();
				if (port == -1) { 
					port = 80; // will have to change to support 443 if ever
				}
				AllowedUri au = new AllowedUri(host, port, u.getPath(), actions);
				UserManager mgr = cfg.getUserManager();
				mgr.addPermissionForLastUserAdded(au);
			}
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
