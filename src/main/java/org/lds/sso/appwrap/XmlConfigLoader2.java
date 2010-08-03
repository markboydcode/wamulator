package org.lds.sso.appwrap;

import java.io.ByteArrayOutputStream; 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.LogicalSyntaxEvaluationEngine;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.lds.sso.appwrap.rest.RestVersion;
import org.lds.sso.appwrap.rest.oes.v1.ArePermitted;
import org.lds.sso.appwrap.rest.oes.v1.EncodeLinks;
import org.lds.sso.appwrap.rest.oes.v1.EncodeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XmlConfigLoader2 {
    private static final Logger cLog = Logger.getLogger(XmlConfigLoader2.class);
    
	public static final String MACRO_PREFIX = "{{";
	public static final String MACRO_SUFFIX = "}}";
	
    public static final String SRC_SYSTEM = "system:";
    public static final String SRC_CLASSPATH = "classpath:";
    public static final String SRC_FILE = "file:";
	public static final String SRC_STRING = "string:";
	
    public static final String PARSING_ALIASES = "aliases";
    public static final String PARSING_SYNTAXES = "alias-syntaxes";
    public static final String PARSING_CONFIG = "config-instance";
    public static final String PARSING_PATH = "path";
    public static final String PARSING_CURR_SITE = "by-site-host";

    /**
     * Threadlocal that creates a thread specific map used during parsing of the
     * xml configuration file.
     */
    static final ThreadLocal<Map<String,Object>> parsingContextAccessor =
        new ThreadLocal<Map<String,Object>> () {

            @Override
            protected Map<String, Object> initialValue() {
                return new HashMap<String,Object>();
            }
    };
    
    public static void load(String xml) throws Exception {
		load(new StringReader(xml), "from String '" + xml + "'");
	}
	
    public static void load(Reader reader, String sourceInfo) throws Exception {
        load(reader, sourceInfo, new CfgContentHandler());
    }
    
    public static void load(Reader reader, String sourceInfo, ContentHandler hndlr) throws Exception {
        XMLReader rdr;
        try {
            rdr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        }
        catch (Exception e) {
            throw new Exception("Unable to create parser for loading configuration '" 
                    + sourceInfo + "'.", e);
        }
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
		 * Returns true only if a single step exists meaning that we are 
		 * residing within the document's root element.
		 * @return
		 */
		public boolean isAtTopLevelElement() {
		    return steps.size() == 1;
		}
		
		/**
		 * Shortcut for {@link #isAtTopLevelElement()}.
		 * @return
		 */
		public boolean isAtTLE() {
		    return isAtTopLevelElement();
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

	/**
	 * Class used to convey the original cpath attribute value for an unenforced
	 * or allowed declaration and its nested parts if defined.
	 * 
	 * @author BoydMR
	 *
	 */
	public static class CPathParts {
	    public String rawValue = null;
	    public String path = null;
	    public String query = null;
	}
	
    /**
     * User during config file parsing to resolve references to {{token}} to
     * values stored in the aliases map by "token" key. If not found then an
     * illegal argument exception is thrown.
     * 
     * @param val
     * @return
     */
    static String resolveAliases(String val) {
        int curIdx = 0;
        int leftCurlys = val.indexOf("{{");
        StringBuffer resolved = new StringBuffer();
        boolean foundEnd = false;
        Map<String,String> aliases = (Map<String, String>) parsingContextAccessor.get().get(PARSING_ALIASES);
        
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

    /**
     * Used during parsing to validate condition attributes are a single macro for a defined 
     * alias and returns the alias name.
     *  
     * @param pathToElement
     * @param atts
     * @return
     * @throws EvaluationException 
     */
    static String getCondition(Path pathToElement, Attributes atts, boolean verifySyntax) throws EvaluationException {
        Map<String,String> aliases = (Map<String, String>) parsingContextAccessor.get().get(PARSING_ALIASES);
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
                aliases.get(con) == null) {
            throw new IllegalArgumentException("Attribute 'condition' for "
                    + pathToElement 
                    + " must be a single macro for a defined alias. ex: '{{alias-name}}'.") ;
        }
        String content = aliases.get(con);
        if (content == null) {
            throw new IllegalArgumentException("Macro in 'condition' attribute for "
                    + pathToElement 
                    + " does not match any declared alias.") ;
        }
        Map<String,String> aliasSyntaxes = (Map<String, String>) parsingContextAccessor.get().get(PARSING_SYNTAXES);
        String aliasSyntax = aliasSyntaxes.get(con);
        if (aliasSyntax != null &&
                ! aliasSyntax.startsWith(SRC_CLASSPATH) &&
                ! aliasSyntax.startsWith(SRC_FILE) &&
                ! aliasSyntax.startsWith(SRC_SYSTEM)) {
            throw new IllegalArgumentException("Attribute 'condition' for "
                    + pathToElement 
                    + " must be custom condition syntax loaded from file. " 
                    + "ex: <?alias is-bishop=classpath:bishop-only-syntax.xml?>...<allow condition='{{is-bishop}}'...") ;
        }
        // verify validity of condition syntax
        if (verifySyntax) {
            LogicalSyntaxEvaluationEngine engine = new LogicalSyntaxEvaluationEngine();
            engine.getEvaluator(content);
        }
        return con;
    }
    
    /**
     * Delegates to {@link #getIntegerAtt(String, Path, Attributes, boolean)}
     * with required set to true requiring the existence of the attribute
     * or an exception is thrown.
     * 
     * @param name
     * @param pathToElement
     * @param atts
     * @return
     */
    static int getIntegerAtt(String name, Path pathToElement, Attributes atts) {
        return getIntegerAtt(name, pathToElement, atts, true);
    }

    /**
     * Returns 
     * @param name
     * @param pathToElement
     * @param atts
     * @param required
     * @return
     */
    static int getIntegerAtt(String name, Path pathToElement, Attributes atts, boolean required) {
        String val = atts.getValue(name);
        if (val == null) {
            if (required) {
                throw new IllegalArgumentException("Attribute '" + name 
                        + "' must be specified for element " + pathToElement 
                        + ".");
            }
            else {
                return -1;
            }
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

    static URI getUriAtt(String attName, Path pathToElement, Attributes atts) {
        String val = getStringAtt(attName,pathToElement,atts);
        val = resolveAliases(val);
        URI u = null;
        try {
            u = new URI(val);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to parse '" + attName 
                    + "' in " + pathToElement, e);
        }
        return u;
    }
    
    /**
     * Gets an attribute purported to be a relative path url with optional
     * query string parameter and returns a three element array with the path in
     * index 0 and query in index 1 and original raw declaration value in
     * index 3. Path may be an empty string for a url of
     * "?some/path" and query may be a null value for a url of "/some/path?"
     * or "/some/path".
     *  
     * @param attName
     * @param pathToElement
     * @param atts
     * @return
     */
    static CPathParts getRelUriAtt(String attName, Path pathToElement, Attributes atts) {
        String val = getStringAtt(attName,pathToElement,atts);
        val = resolveAliases(val);
        CPathParts parts = new CPathParts();
        parts.rawValue = val;
        // parts.path will hold path portion or null if not found
        // parts.query will hold query portion or null if not found
        String[] vals = val.split("\\?");
        /* split returns the following:
         * 1 "hello?there"  --> [hello, there]  path=hello, query=there
         * 2 "hello-there"  --> [hello-there]   path=hello-there, query=null
         * 3 "?hello-there" --> [, hello-there] path="", query=hello-there
         * 4 "hello-there?" --> [hello-there]   path=hello-there, query=null
         */
        parts.path = vals[0];
        if (vals.length != 1) { // is result 1 and 3 above, null otherwise
            parts.query=vals[1];
        }
        
        return parts;
    }
    
    /**
     * Get required String attribute tossing exception if not found.
     * 
     * @param attName
     * @param pathToElement
     * @param atts
     * @return
     */
    static String getStringAtt(String attName, Path pathToElement, Attributes atts) {
        return getStringAtt(attName, pathToElement, atts, true);
    }
    
    /**
     * Get options string attribute returning null if not found.
     * 
     * @param attName
     * @param pathToElement
     * @param atts
     * @param required
     * @return
     */
    static String getStringAtt(String attName, Path pathToElement, Attributes atts, boolean required) {
        String val = atts.getValue(attName);
        if (val == null || "".equals(val)) {
            if (required) {
                throw new IllegalArgumentException("Attribute '" + attName + "' must be specified for " + pathToElement);
            }
            else {
                return null;
            }
        }
        return resolveAliases(val);
    }

    /**
     * Tests whether or not an 'allow' or 'unenforced' declaration is useful
     * meaning the URLs that match it won't be matching a preceeding declaration
     * and hence never make it to this declaration rendering it irrelevant.
     *  
     * @param sm
     * @param cp
     * @param path
     */
    static void isDeclarationUseful(SiteMatcher sm, CPathParts cp, Path path) {
        OrderedUri ou = sm.getManagerOfUri(sm.getScheme(), sm.getHost(), sm.getPort(), cp.path, cp.query);
        if (ou != null) {
            throw new IllegalArgumentException("URLs matching cpath attribute value '" 
                    + cp.rawValue + "' of " + path + " will be consumed by '"
                    + (ou instanceof UnenforcedUri ? "unenforced" : "allow")
                    + "' declaration with cpath value of '" 
                    + ou.getCpathDeclaration() 
                    + "' which precedes it in document order. "
                    + "Declare elements for nested URLs first.");
        }
    }

    
	public static class CfgContentHandler implements ContentHandler {

		public CfgContentHandler() {
			parsingContextAccessor.get().put(PARSING_CONFIG, Config.getInstance());
            parsingContextAccessor.get().put(PARSING_ALIASES, new HashMap<String,String>());
            parsingContextAccessor.get().put(PARSING_SYNTAXES, new HashMap<String,String>());
            parsingContextAccessor.get().put(PARSING_PATH, new Path());
		}
		
		/**
		 * Enables processing instructions in the XML having the following three
		 * forms. Each declares an alias that can then be referenced
		 * in any configuration file attribute value by
		 * including the macro "{{name}}" within the attribute's string of 
		 * characters or within the value portion of any later occurring alias 
		 * declarations including classpath files or resolved resources. Each 
		 * has a specific use.
		 * 
		 * <pre>
		 * [1] &lt;?alias name=[value]?&gt;
         * [2] &lt;?alias name=classpath:[resource-file-path]?&gt;
         * [3] &lt;?alias name=system:[resource-name]?&gt;
		 * </pre>
		 * 
		 * [1] adds a named value pair to the map of aliases where the value is
		 * the literal text although that text could be a macro causing this
		 * alias to have the same value as the other alias or it could have
		 * one or more alias embedded within it surrounded by literal text.
		 * 
		 * [2] adds a named value pair to the map of alias where the value is
		 * the character content of the referenced resource file that must be
		 * available from the classpath. Making such file
		 * based content available is not accomplished by conceptually embedding
		 * such content within the XML declaring the alias and hence won't effect
		 * the xml processing. This allows for alias values to contain XML 
		 * sensitive characters and hence XML constructs. Such character content
		 * can also include macro references for earlier defined alias which will 
		 * be resolved upon loading.
		 * 
		 * [3] adds a named value pair to the map of aliases where the value is
		 * the character content of a java.lang.System property. This version 
		 * is used to emulate the classpath version during unit tests without 
		 * having to create such files. In particular, this enables condition
		 * syntax to be used in unit tests. 
		 */
		@SuppressWarnings("unchecked")
        public void processingInstruction(String target, String data) throws SAXException {
			if (target.equals("alias")) {
				int eqIdx = data.indexOf('=');
				if (eqIdx >= 0) {
					String name = data.substring(0, eqIdx).trim();
					String val = data.substring(eqIdx+1).trim();
					String rawVal = val;
					//String srcPrefix = SRC_STRING;
					
                    // alias handling for "classpath:..."
                    if (val.toLowerCase().startsWith(SRC_CLASSPATH)) {
                        String resrc = val.substring(SRC_CLASSPATH.length());
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
                    // alias handling for "system:..."
                    else if (val.toLowerCase().startsWith(SRC_SYSTEM)) {
                        String resrc = val.substring(SRC_SYSTEM.length());
                        val = System.getProperty(resrc);
                        
                        if (val == null) {
                            throw new IllegalArgumentException("System alias resource '"
                                    + resrc + "' not found in java.lang.System.");
                        }
                    }
                    // alias handling for "file:..."
                    else if (val.toLowerCase().startsWith(SRC_FILE)) {
                        String resrc = val.substring(SRC_FILE.length());
                        File file = new File(resrc);
                        
                        
                        if (! file.exists()) {
                            throw new IllegalArgumentException("File alias resource '"
                                    + resrc + "' not found at '" + file.getAbsolutePath() + "'.");
                        }
                        else {
                            InputStream src;
                            try {
                                src = new FileInputStream(file);
                            } catch (FileNotFoundException e) {
                                throw new SAXException("Unable to load content for alias '"
                                        + name + "' from file resource '" 
                                        + file.getAbsolutePath() + "'.", e);
                            }
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
                                        + name + "' from file resource '" 
                                        + file.getAbsolutePath() + "'.", e);
                            }
                            val = bfr.toString().trim();
                        }
                    }
                    else {
                        // alias handling for "literal-text" use 'val' as-is
                    }
					val = resolveAliases(val);
                    Map<String,String> aliases = (Map<String, String>) parsingContextAccessor.get().get(PARSING_ALIASES);
                    aliases.put(name, val);
                    Map<String,String> aliasSyntax = (Map<String, String>) parsingContextAccessor.get().get(PARSING_SYNTAXES);
					aliasSyntax.put(name, rawVal);
				}
			}
		}

		public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
            Map<String,String> aliases = (Map<String, String>) parsingContextAccessor.get().get(PARSING_ALIASES);
            Config cfg = (Config) parsingContextAccessor.get().get(PARSING_CONFIG);
            Path path = (Path) parsingContextAccessor.get().get(PARSING_PATH);
			
			path.add(name);
			if (path.matches("/config")) {
                String cpVal = atts.getValue("console-port");
                if ("auto".equals(cpVal)) {
                    cfg.setConsolePort(0);
                }
                else {
                    cfg.setConsolePort(getIntegerAtt("console-port", path, atts));
                }

                String ppVal = atts.getValue("proxy-port");
                if ("auto".equals(ppVal)) {
                    cfg.setProxyPort(0);
                }
                else {
                    cfg.setProxyPort(getIntegerAtt("proxy-port", path, atts));
                }
                
				String allow_proxying = atts.getValue("allow-non-sso-traffic");
				cfg.setAllowForwardProxying(Boolean.parseBoolean(allow_proxying));
				String restVersion = atts.getValue("rest-version");
				if (restVersion != null) {
					RestVersion ver = RestVersion.findRestVersionById(restVersion);
					if (ver == null) {
						throw new IllegalArgumentException("rest-version must be one of "
								+ RestVersion.getValidIdentifiers() + " in " + path + " not '" + restVersion + "'");
					}
					cfg.setRestVersion(ver);
				}
			}
			else if (path.matches("/config/sso-cookie")) {
				cfg.setCookieName(getStringAtt("name", path, atts));
				cfg.setCookieDomain(getStringAtt("domain", path, atts));
			}
			else if (path.matches("/config/console-recording")) {
				boolean sso = Boolean.parseBoolean(getStringAtt("sso", path, atts));
				cfg.getTrafficRecorder().setRecording(sso);
				boolean rest = Boolean.parseBoolean(getStringAtt("rest", path, atts));
				cfg.getTrafficRecorder().setRecordingRest(rest);
                                Integer maxEntries = Config.MAX_TRAFFIC_ENTRIES;
                                try {
                                    maxEntries = Integer.parseInt(getStringAtt("max-entries", path, atts));
                                } catch (NumberFormatException e) {
                                    //Just let the default be set
                                }
                                cfg.setMaxEntries(maxEntries);
                                boolean debugLoggingEnabled = Boolean.parseBoolean(getStringAtt("enable-debug-logging", path, atts));
                                cfg.setDebugLoggingEnabled(debugLoggingEnabled);
			}
			else if (path.matches("/config/sso-sign-in-url")) {
                String signin = atts.getValue("value");
                if (signin != null &&
                        signin.contains("{{console-port}}") && 
                        cfg.getConsolePort() == 0 && // auto binding specified
                        ! aliases.containsKey("console-port")) {
                    cfg.setSignInRequiresResolution();
                    cfg.setSignInPage(signin); // resolve alias after start-up
                }
                else {
                    cfg.setSignInPage(getStringAtt("value", path, atts));
                }
			}
			else if (path.matches("/config/sso-header")) {
				String hdrNm = getStringAtt("name", path, atts);
				String hdrVl = getStringAtt("value", path, atts);
				if (hdrNm.equals(UserHeaderNames.SERVICE_URL)) {
				    String msg = "NOTE: Global sso-header declaration for '" + hdrNm
				        + "' is now ignored since it is automatically generated.";
				}
				else {
	                cfg.addGlobalHeader(hdrNm, hdrVl);
				}
			} 
            else if (path.matches("/config/sso-traffic")) {
                cfg.setStripEmptyHeaders(Boolean.parseBoolean(getStringAtt("strip-empty-headers", path, atts, false)));
                if (cfg.getStripEmptyHeaders()) {
                    System.out.println("Proxy is configured to strip empty headers.");
                }
            }
            else if (path.matches("/config/sso-traffic/by-site")) {
				String scheme = atts.getValue("scheme");
				if (scheme == null || scheme.equals("")) {
					scheme = "http";
				}
				String host = getStringAtt("host", path, atts);
                int port = -1;
                String portS = atts.getValue("port");
                // check for auto proxy-port being used and allow port to follow if indicated
                if (cfg.getProxyPort() == 0 && portS != null && portS.equals("{{proxy-port}}")) { 
                    // for letting host choose the port to bind to for the proxy
                    // we still want to be able to define sites with a port matching
                    // that of the proxy catching the traffic in the by-site
                    // config element. hence we allow for a macro matching the 
                    // attribute name for the proxy port on the config element
                    // and figure out the port at runtime.
                    
                    port = 0;
                }
                else {
                    port = getIntegerAtt("port", path, atts);
                }
				TrafficManager trafficMgr = cfg.getTrafficManager();
				SiteMatcher m = trafficMgr.getSite(host, port);
				// causes reuse of existing site matcher but this will need to change
				// if/when we support any other protocol besides http since SiteMatcher
				// constructor takes scheme but trafficMgr.getSite() only uses host
				// and port to distinguish between them.
				if (m == null) {
				    m = new SiteMatcher(scheme, host, port, trafficMgr);
	                trafficMgr.addMatcher(m);
				}
	            parsingContextAccessor.get().put(PARSING_CURR_SITE, host);
			}
			else if (path.matches("/config/sso-traffic/by-site/cctx-file")) {
				String cctx = getStringAtt("cctx", path, atts); 
				String file = getStringAtt("file", path, atts);
				// for now require * termination to convey only prefix matching is currently
				// supported. remove this once backwards references and env macros are
				// added.
				if (! cctx.endsWith("*")) {
					throw new IllegalArgumentException("cctx must end with '*' in " + path);
				}
				cctx = cctx.substring(0, cctx.length()-1);
				String type = getStringAtt("content-type", path, atts);
				TrafficManager trafficMgr = cfg.getTrafficManager();
				SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
				EndPoint ep = sm.getEndpointForCanonicalUrl(cctx);
				if (ep != null) {
				    throw new IllegalArgumentException("Mapping for cctx='" + cctx 
				            + "*' in " + path + " is consumed by cctx mapping for '" 
				            + ep.getCanonicalContextRoot() 
				            + "*' which precedes it in document order and hence " 
				            + "will never receive any requests.");
				}
				sm.addFileMapping(cctx, file, type);
			}
			else if (path.matches("/config/sso-traffic/by-site/cctx-mapping")) {
				String cctx = getStringAtt("cctx", path, atts); 
                String thost = getStringAtt("thost", path, atts);
                String preserveHost = getStringAtt("preserve-host", path, atts, false);
                boolean preserve = (preserveHost == null ? true : Boolean.parseBoolean(preserveHost));
				int tport = -1;
	            String tportS = atts.getValue("tport");
				// check for auto console-port being used and allow tport to follow if indicated
				if (cfg.getConsolePort() == 0 && tportS != null && tportS.equals("{{console-port}}")) { 
				    // for letting host choose the port to bind to for the console
				    // we still want to be able to direct to the console port in
				    // cctw-mappings. hence we allow for a macro matching the 
				    // attribute name for the console port on the config element
				    // and figure out the port at runtime.
				    
				    tport = 0;
				}
				else {
	                tport = getIntegerAtt("tport", path, atts);
				}
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
                EndPoint ep = sm.getEndpointForCanonicalUrl(cctx);
                if (ep != null) {
                    throw new IllegalArgumentException("Mapping for cctx='" + cctx 
                            + "*' in " + path + " is consumed by cctx mapping for '" 
                            + ep.getCanonicalContextRoot() 
                            + "*' which precedes it in document order and hence "  
                            + "will never receive any requests.");
                }
				sm.addMapping(cctx, thost, tport, tpath, preserve);
			}
			else if (path.matches("/config/sso-traffic/by-site/unenforced")) {
				TrafficManager trafficMgr = cfg.getTrafficManager();
				SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
				CPathParts cp = getRelUriAtt("cpath", path, atts);
                isDeclarationUseful(sm, cp, path);
				UnenforcedUri uu = new UnenforcedUri(sm.getScheme(), sm.getHost(), sm.getPort(), cp.path, cp.query, cp.rawValue);
				sm.addUnenforcedUri(uu);
			}
			else if (path.matches("/config/sso-traffic/by-site/allow")) {
				String actionAtt = getStringAtt("action", path, atts);
				actionAtt = actionAtt.replace(" ", "");
				String[] actions = actionAtt.split(",");
				String cond = null;
				try {
				    cond = getCondition(path, atts, true);
				}
				catch(EvaluationException ee) {
				    throw new SAXException(ee);
				}
				String syntax = aliases.get(cond);

				TrafficManager trafficMgr = cfg.getTrafficManager();
				SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
				CPathParts cp = getRelUriAtt("cpath", path, atts);
				isDeclarationUseful(sm, cp, path);
				AllowedUri au = new AllowedUri(sm.getScheme(), sm.getHost(), sm.getPort(), cp.path, cp.query, actions, cp.rawValue);
				sm.addAllowedUri(au, cond, syntax);
			}
            else if (path.matches("/config/sso-traffic/by-site/entitlements")) {
                // used only for containment of nested <allow> elements to differentiate
                // between course grained (URL) policies and fine grained permissions
                // (entitlements).
            }
            else if (path.matches("/config/sso-traffic/by-site/entitlements/allow")) {
                String actionAtt = null;
                actionAtt = getStringAtt("action", path, atts);
                actionAtt = actionAtt.replace(" ", "");
                String[] actions = actionAtt.split(",");
                String cond = null;
                try {
                    cond = getCondition(path, atts, true);
                }
                catch(EvaluationException ee) {
                    throw new SAXException(ee);
                }
                String syntax = aliases.get(cond);
                
                String urn = null;
                String policyDomain = (String) parsingContextAccessor.get().get(PARSING_CURR_SITE);
                
                urn = getStringAtt("urn",path,atts);
                urn = resolveAliases(urn);
                if (! urn.startsWith("/")) {
                    throw new IllegalArgumentException("Attribute 'urn' with value "
                            + urn + " for " + path 
                            + " must start with a slash '/' character since " 
                            + "they are relative to the by-site host " 
                            + policyDomain + " as the urn's policy domain.");
                }
                Entitlement ent = new Entitlement(actions, policyDomain + urn);
                EntitlementsManager entMgr = cfg.getEntitlementsManager();
                entMgr.addEntitlement(ent, cond, syntax);
            }
			else if (path.matches("/config/users")) {
				String source = getStringAtt("source", path, atts, false);
				cfg.setExternalUserSource(source);

				int timeout = getIntegerAtt("session-timeout-seconds", path, atts, false);
				if (timeout != -1) {
					SessionManager sman = cfg.getSessionManager();
					sman.setSessionInactivityTimeoutSeconds(timeout);
				}
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
            else if (path.matches("/config/users/user/ldsApplications")) {
                String vl = getStringAtt("value", path, atts);
                cfg.getUserManager().addAttributeForLastUserAdded(User.LDSAPPS_ATT, vl);
            }
			else if (path.matches("/config/sso-traffic/rewrite-redirect")) {
				String from = getStringAtt("from", path, atts);
				String to = getStringAtt("to", path, atts);
				TrafficManager mgr = cfg.getTrafficManager();
				mgr.addRewriteForRedirect(from, to);
			}
			else if (path.matches("/config/sso-traffic/rewrite-cookie")) {				
			    String from = getStringAtt("from-path", path, atts);
				String to = getStringAtt("to-path", path, atts);
				TrafficManager mgr = cfg.getTrafficManager();
				mgr.addRewriteForCookie(from, to);
			}
            else if (path.matches("/config/sso-entitlements")) {
                throw new IllegalArgumentException("ERROR: The sso-entitlements element " 
                        + "must now reside within its corresponding " 
                        + "/config/sso-traffic/by-site element and be renamed to just 'entitlements'. " 
                        + "Its policy-domain attribute is no longer used but its nested content " 
                        + "remains unchanged. Please apply these changes and restart.");
            }
            else {
                String msg = "Unsupported element at " + path + " ignoring."; 
                System.out.println(msg);
                cLog.error(msg);
            }
		}

		public void endElement(String uri, String localName, String name) throws SAXException {
            Path path = (Path) parsingContextAccessor.get().get(PARSING_PATH);
			path.remove(name);
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
