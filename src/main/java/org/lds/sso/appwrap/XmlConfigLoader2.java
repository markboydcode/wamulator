package org.lds.sso.appwrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.lds.sso.appwrap.AppEndPoint.InboundScheme;
import org.lds.sso.appwrap.AppEndPoint.OutboundScheme;
import org.lds.sso.appwrap.AppEndPoint.Scheme;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.GlobalHeaderNames;
import org.lds.sso.appwrap.conditions.evaluator.LogicalSyntaxEvaluationEngine;
import org.lds.sso.appwrap.identity.ExternalUserSource;
import org.lds.sso.appwrap.identity.ExternalUserSource.ConfigurationException;
import org.lds.sso.appwrap.identity.SessionManager;
import org.lds.sso.appwrap.identity.coda.CodaUserSource;
import org.lds.sso.appwrap.identity.ldap.LdapUserSource;
import org.lds.sso.appwrap.identity.legacy.WamulatorUserSource;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.io.SimpleErrorHandler;
import org.lds.sso.appwrap.policy.WamulatorPolicySource;
import org.lds.sso.appwrap.rest.RestVersion;
import org.lds.sso.appwrap.xml.Alias;
import org.lds.sso.appwrap.xml.AliasHolder;
import org.lds.sso.appwrap.xml.PlaintextAlias;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

public class XmlConfigLoader2 {
    public static final String WAMULATOR_SCHEMA_NAMESPACE_URI = "http://code.lds.org/schema/wamulator";

	private static final Logger cLog = Logger.getLogger(XmlConfigLoader2.class.getName());

    public static final String MACRO_PREFIX = "{{";
    public static final String MACRO_SUFFIX = "}}";

    public static final String SRC_SYSTEM = "system:";
    public static final String SRC_EMBEDDED = "embedded:";
    public static final String SRC_CLASSPATH = "classpath:";
    public static final String SRC_FILE = "file:";
    public static final String SRC_STRING = "string:";

    public static final String PARSING_SYNTAXES = "alias-syntaxes";
    public static final String PARSING_CONFIG = "config-instance";
    public static final String PARSING_PATH = "path";
    public static final String PARSING_CURR_SITE = "by-site-host";
	public static final String PARSING_USR_SRC_CONTENT = "user-source-content";
	public static final String PARSING_CURR_EXT_USR_SRC = "user-source";
	public static final String PARSING_POL_SRC_CONTENT = "policy-source-content";
	public static final String PARSING_CURR_EXT_POL_SRC = "policy-source";

    public static final String PROP_DEFAULT = "property";
    private static final String XSD_LOCATION = "org/lds/sso/appwrap/wamulator-5.0.xsd";
    private static final Schema schema;
    
    static {
    	SchemaFactory fact = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		InputStream schemaLocation = XmlConfigLoader2.class.getClassLoader().getResourceAsStream(XSD_LOCATION);
		schema = newSchema(fact, new StreamSource(schemaLocation));
    }
    
	private static final Schema newSchema(SchemaFactory fact, Source source) {
		Schema schema = null;
		try {
			schema = fact.newSchema(source);
		} catch ( SAXException e ) {
			LogUtils.throwing(cLog, "Couldn't find validation schema... start up state may be unpredictable.", e);
		}
		return schema;
	}
	
    public static final class ConfigProperty<T> {
    	public final String name;
    	
    	public ConfigProperty(String name) {
    		this.name = name;
    	}
    }
    
    // here is a way to type properties in order to ease the parsingContextAccessor.get().get(...) madness.  See the get() and put() methods.
    public static final ConfigProperty<AliasHolder> PARSING_ALIASES = new ConfigProperty<AliasHolder>("aliases");
    
    /**
     * Threadlocal that creates a thread specific map used during parsing of the
     * xml configuration file.
     */
    public static final ThreadLocal<Map<String,Object>> parsingContextAccessor =
        new ThreadLocal<Map<String,Object>> () {

        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<String, Object>();
        }
    };
    
    /**
     * Get a value from the parsingContextAccessor
     * 
     * @param <T>
     * @param key - The ConfigProperty that keys this value in the parsingContextAccessor
     * @return - The value keyed by the ConfigProperty passed
     */
    @SuppressWarnings("unchecked")
	public static <T> T get(ConfigProperty<T> key) {
    	return (T)parsingContextAccessor.get().get(key.name);
    }
    
    /**
     * Put a value into the parsingContextAccessor
     * 
     * @param <T>
     * @param key - The ConfigProperty to key this value by in the parsingContextAccessor
     * @param value - The value to key
     */
    public static <T> void put(ConfigProperty<T> key, T value) {
    	parsingContextAccessor.get().put(key.name, value);
    }
    
    /**
     * Loads configuration from a String.
     * 
     * @param xml
     * @throws Exception
     */
    public static void load(String xml) throws Exception {
        load(new StringReader(xml), "from String '" + xml + "'");
    }

    /**
     * Loads configuration from a Reader.
     * 
     * @param reader
     * @param sourceInfo
     * @throws Exception
     */
    public static void load(Reader reader, String sourceInfo) throws Exception {
        load(reader, sourceInfo, new CfgContentHandler());
    }

    /**
     * USE WITH CAUTION: Loads configuration using a custom content handler.  
     * Initializes memory for parsing and implements two pass parsing delegating
     * to _rawload for actual parsing by a single content handler.
     * 
     * @param reader
     * @param sourceInfo
     * @param hndlr
     * @throws Exception
     */
    public static void load(Reader reader, String sourceInfo,
            ContentHandler hndlr) throws Exception {
        // first cache the raw content
        String content = getContentAsString(reader);
        parsingContextAccessor.get().put(PARSING_CONFIG,
                Config.getInstance());
        put(PARSING_ALIASES, new AliasHolder());
        
        boolean validate = content.contains(WAMULATOR_SCHEMA_NAMESPACE_URI);
        // now run alias and embedded condition parser
        parsingContextAccessor.get().put(PARSING_PATH, new Path());
        _rawload(new StringReader(content), sourceInfo, new CfgAliasesHandler(), validate);
        
        // now parse non alias/condition directives
        parsingContextAccessor.get().put(PARSING_PATH, new Path());
        _rawload(new StringReader(content), sourceInfo, hndlr, validate);
    }

    /**
     * Launches the parsing with suitable exception handling.
     * 
     * @param reader
     * @param sourceInfo
     * @param hndlr
     * @throws Exception
     */
    private static void _rawload(Reader reader, String sourceInfo,
            ContentHandler hndlr, boolean validate) throws Exception {
        XMLReader rdr;
        try {
        	SAXParserFactory factory = SAXParserFactory.newInstance();
        	if(validate) {
        		factory.setNamespaceAware(true);
        		factory.setSchema(schema);
        	}
        	SAXParser parser = factory.newSAXParser();
            rdr = parser.getXMLReader();
        } catch (Exception e) {
            throw new Exception(
                    "Unable to create parser for loading configuration '"
                            + sourceInfo + "'.", e);
        }
        rdr.setContentHandler(hndlr);
        InputSource src = new InputSource(reader);
        try {
        	rdr.setErrorHandler(new SimpleErrorHandler(cLog));
            rdr.parse(src);
        } catch (Exception e) {
        	if(e instanceof SAXParseException) {
        		SAXParseException parseException = (SAXParseException)e;
                throw new Exception("Parse Error, Line: " 
                        + parseException.getLineNumber() + ", Column: " 
                        + parseException.getColumnNumber() + ", Message: "
                        + parseException.getMessage() + " in source '" + sourceInfo
                        + "'.");
        	}
            throw new Exception("Unable to parse configuration '" + sourceInfo
                    + "'.", e);
        }
    }
    
    /**
     * Utility method for reading config content in and returning as a String.
     * 
     * @param reader
     * @return
     * @throws IOException
     */
    public static String getContentAsString(Reader reader) throws IOException {
        char[] chars = new char[1024];
        StringWriter sw = new StringWriter();
        int read = reader.read(chars);

        while (read != -1) {
            sw.write(chars, 0, read);
            read = reader.read(chars);
        }
        return sw.toString();
    }

    /**
     * Utility class that allows for simple testing for the full path of an
     * element during parsing.
     * 
     * @author Mark Boyd
     * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day
     *             Saints
     * 
     */
    public static class Path {
        List<String> steps = new ArrayList<String>();
        Map<String, Integer> depths = new HashMap<String, Integer>();  
        private String compositePath = "/";

        /**
         * Adds a step to the path being traversed.
         * 
         * @param step
         */
        public synchronized void add(String step) {
            steps.add(step);
            buildTypePath();
            incrementInstanceCount();
        }

        /**
         * Keeps track of current node index within sibling group for each level
         * penetrated for building the instance path. For example, if I was looking
         * in the second level1 node's children at content within the first level3
         * child I would have an instance path of:
         * 
         * /root/level1[2]/level3[1]
         * 
         * @param compositePath2
         */
        private void incrementInstanceCount() {
        	// ignore root
        	if (! compositePath.equals("/")) {
        		Integer count = depths.get(compositePath);
        		if (count == null) {
        			depths.put(compositePath, 1);
        		}
        		else {
        			depths.put(compositePath, count.intValue() + 1);
        		}
        	}
    	}

        /**
         * Builds a String of the steps currently in the path for speeding
         * comparisons.
         * 
         * @param step
         */
        private synchronized void buildTypePath() {
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
         * Builds a String of the steps currently in the path including instance
         * counts for sibling groups. 
         * 
         * ie: /root/level1[2]/level2[1]/level3[6]
         * 
         * @param step
         */
        private synchronized String buildInstancePath() {
            StringBuffer noIndicesBfr = new StringBuffer();
            StringBuffer bfr = new StringBuffer();
            int depth = 0;
            
            for (String step : steps) {
                noIndicesBfr.append('/');
                noIndicesBfr.append(step);
                bfr.append('/');
                bfr.append(step);
                Integer count = depths.get(noIndicesBfr.toString());
                
                // don't include index for root node, will always be 1
                if (depth != 0 && count != null) {
                	bfr.append('[');
                	bfr.append("" + count.intValue());
                	bfr.append(']');
                }
                depth++;
            }
            String instPath = bfr.toString();
            if ("".equals(instPath)) {
                instPath = "/";
            }
            return instPath;
        }

        /**
         * Returns true only if a single step exists meaning that we are
         * residing within the document's root element.
         * 
         * @return
         */
        public boolean isAtTopLevelElement() {
            return steps.size() == 1;
        }

        /**
         * Shortcut for {@link #isAtTopLevelElement()}.
         * 
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
                throw new IllegalStateException(
                        "Attempt to remove non-existent element from empty list.");
            }
            String last = steps.get(steps.size() - 1);
            if (!last.equals(step)) {
                throw new IllegalArgumentException("Step '" + step
                        + "' does not match the last step in the path '" + last
                        + "'. Can't remove.");
            }
            steps.remove(steps.size() - 1);
            buildTypePath();
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
            return buildInstancePath();
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
     * Used during parsing to validate condition attributes are a single macro
     * for a defined alias and returns the alias name.
     * 
     * @param pathToElement
     * @param atts
     * @return
     * @throws EvaluationException
     */
    static String getCondition(Path pathToElement, Attributes atts,
            boolean verifySyntax) throws EvaluationException {
        String con = atts.getValue("condition");
        if (con == null || "".equals(con)) {
            return null;
        }
        if (!con.startsWith(MACRO_PREFIX) || !con.endsWith(MACRO_SUFFIX)) {
            throw new IllegalArgumentException(
                    "Attribute 'condition' for "
                            + pathToElement
                            + " must be a single macro for a defined alias. ex: '{{alias-name}}'.");
        }
        con = con.substring(MACRO_PREFIX.length(), con.length()
                - MACRO_SUFFIX.length());
        if (con.contains(MACRO_PREFIX) || con.contains(MACRO_SUFFIX)
                || get(PARSING_ALIASES).getAliasValue(con) == null) {
            throw new IllegalArgumentException(
                    "Attribute 'condition' for "
                            + pathToElement
                            + " must be a single macro for a defined alias. ex: '{{alias-name}}'.");
        }
        String content = get(PARSING_ALIASES).getAliasValue(con);
        if (content == null) {
            throw new IllegalArgumentException(
                    "Macro in 'condition' attribute for " + pathToElement
                            + " does not match any declared alias.");
        }
        Alias alias = get(PARSING_ALIASES).getAlias(con);
        if ( alias instanceof PlaintextAlias ) {
            throw new IllegalArgumentException(
                    "Attribute 'condition' for "
                            + pathToElement
                            + " must be custom condition syntax loaded from:\n"
                            + "file (ex: <?alias is-bishop=file:bishop-only-syntax.xml?>),\n" 
                            + "System properties (ex: <?alias is-bishop=system:bishop-only-syntax?>),\n" 
                            + "classpath file (ex: <?alias is-bishop=file:bishop-only-syntax.xml?>),\n"
                            + "or embedded element. (ex: <condition alias='is-bishop'><HasPosition id='4'/></condition>).");
        }
        // verify validity of condition syntax
        if (verifySyntax) {
            LogicalSyntaxEvaluationEngine engine = LogicalSyntaxEvaluationEngine.getSyntaxEvalutionInstance();
            engine.getEvaluator(con, content);
        }
        return con;
    }

    /**
     * Delegates to {@link #getIntegerAtt(String, Path, Attributes, boolean)}
     * with required set to true requiring the existence of the attribute or an
     * exception is thrown.
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
     * 
     * @param name
     * @param pathToElement
     * @param atts
     * @param required
     * @return
     */
    static int getIntegerAtt(String name, Path pathToElement, Attributes atts,
            boolean required) {
        String val = atts.getValue(name);
        if (val == null) {
            if (required) {
                throw new IllegalArgumentException("Attribute '" + name
                        + "' must be specified for element " + pathToElement
                        + ".");
            } else {
                return -1;
            }
        }
        val = Alias.resolveAliases(val);
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException n) {
            throw new IllegalArgumentException("Attribute '" + name
                    + "' for element " + pathToElement + " must be an integer.");
        }
    }

    static URI getUriAtt(String attName, Path pathToElement, Attributes atts) {
        String val = getStringAtt(attName, pathToElement, atts);
        val = Alias.resolveAliases(val);
        URI u = null;
        try {
            u = new URI(val);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to parse '" + attName
                    + "' in " + pathToElement, e);
        }
        return u;
    }

    /**
     * Gets an attribute purported to be a relative path url with optional query
     * string parameter and returns a three element array with the path in index
     * 0 and query in index 1 and original raw declaration value in index 3.
     * Path may be an empty string for a url of "?some/path" and query may be a
     * null value for a url of "/some/path?" or "/some/path".
     * 
     * @param attName
     * @param pathToElement
     * @param atts
     * @return
     */
    static CPathParts getRelUriAtt(String attName, Path pathToElement,
            Attributes atts) {
        String val = getStringAtt(attName, pathToElement, atts);
        val = Alias.resolveAliases(val);
        CPathParts parts = new CPathParts();
        parts.rawValue = val;
        // parts.path will hold path portion or null if not found
        // parts.query will hold query portion or null if not found
        String[] vals = val.split("\\?");
        /*
         * split returns the following: 1 "hello?there" --> [hello, there]
         * path=hello, query=there 2 "hello-there" --> [hello-there]
         * path=hello-there, query=null 3 "?hello-there" --> [, hello-there]
         * path="", query=hello-there 4 "hello-there?" --> [hello-there]
         * path=hello-there, query=null
         */
        parts.path = vals[0];
        if (vals.length != 1) { // is result 1 and 3 above, null otherwise
            parts.query = vals[1];
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
    public static String getStringAtt(String attName, Path pathToElement,
            Attributes atts) {
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
    public static String getStringAtt(String attName, Path pathToElement,
            Attributes atts, boolean required) {
        String val = atts.getValue(attName);
        if (val == null || "".equals(val)) {
            if (required) {
                throw new IllegalArgumentException("Attribute '" + attName
                        + "' must be specified for " + pathToElement);
            } else {
                return null;
            }
        }
        return Alias.resolveAliases(val);
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
    	InboundScheme newSmScheme = sm.getScheme();
    	OrderedUri ou =null;
    	
    	if (newSmScheme == InboundScheme.BOTH) {
    		ou = sm.getManagerOfUri(Scheme.HTTP, sm.getHost(), sm
                    .getPort(), cp.path, cp.query);
    		if (ou != null) {
                throw new IllegalArgumentException("URLs matching cpath attribute value '" 
                        + cp.rawValue + "' of " + path + " on http will be consumed by '"
                        + (ou instanceof UnenforcedUri ? "unenforced" : "allow")
                        + "' declaration with cpath value of '" 
                        + ou.getCpathDeclaration() 
                        + "' which precedes it in document order. "
                        + "Declare elements for nested URLs first.");
    		}
			ou = sm.getManagerOfUri(Scheme.HTTPS, sm.getHost(), sm
                    .getPort(), cp.path, cp.query);
    		if (ou != null) {
                throw new IllegalArgumentException("URLs matching cpath attribute value '" 
                        + cp.rawValue + "' of " + path + " on https will be consumed by '"
                        + (ou instanceof UnenforcedUri ? "unenforced" : "allow")
                        + "' declaration with cpath value of '" 
                        + ou.getCpathDeclaration() 
                        + "' which precedes it in document order. "
                        + "Declare elements for nested URLs first.");
    		}
    	}
    	else {
    		Scheme s = Scheme.fromMoniker(newSmScheme.moniker);
			ou = sm.getManagerOfUri(s, sm.getHost(), sm
                    .getPort(), cp.path, cp.query);
	        if (ou != null) {
	            throw new IllegalArgumentException("URLs matching cpath attribute value '" 
	                    + cp.rawValue + "' of " + path + " on " 
	                    + s.moniker + " will be consumed by '"
	                    + (ou instanceof UnenforcedUri ? "unenforced" : "allow")
	                    + "' declaration with cpath value of '" 
	                    + ou.getCpathDeclaration() 
	                    + "' which precedes it in document order. "
	                    + "Declare elements for nested URLs first.");
	        }
    	}
    }

    public static class CfgContentHandler implements ContentHandler {

    	// cctx-mapping attributes needed when processing policy-source file
        String curCctx, curThost, curCscheme, curTscheme, curHostHdr;
        String curPolicyServiceGateway,curSchemeHeaderOvrd, curInjectSchemeHeader;
        boolean curPreserveHost, curInjectScheme;
        OutboundScheme curOutgoingScheme;
        InboundScheme curIncomingScheme;
        int curTsslPort, curTport;
    	
		public CfgContentHandler() {
        }


        public void processingInstruction(String paramString1,
                String paramString2) throws SAXException {
        }

        public void startElement(String uri, String localName, String name,
                Attributes atts) throws SAXException {
            Config cfg = (Config) parsingContextAccessor.get().get(
                    PARSING_CONFIG);
            Path path = (Path) parsingContextAccessor.get().get(PARSING_PATH);

            path.add(name);
            
            if (path.matches("/config")) {
                String cpVal = atts.getValue(Config.CONSOLE_PORT_ALIAS);
                if ("auto".equals(cpVal)) {
                    cfg.setConsolePort(0);
                } else {
                    cfg.setConsolePort(getIntegerAtt(Config.CONSOLE_PORT_ALIAS, path, atts));
                }

                String ppVal = atts.getValue(Config.PROXY_PORT_ALIAS);
                if ("auto".equals(ppVal)) {
                    cfg.setProxyPort(0);
                } else {
                    cfg.setProxyPort(getIntegerAtt(Config.PROXY_PORT_ALIAS, path, atts));
                }

                String allow_proxying = atts.getValue("allow-non-sso-traffic");
                cfg.setAllowForwardProxying(Boolean
                        .parseBoolean(allow_proxying));
                String restVersion = atts.getValue("rest-version");
                if (restVersion != null) {
                    RestVersion ver = RestVersion
                            .findRestVersionById(restVersion);
                    if (ver == null) {
                        throw new IllegalArgumentException(
                                "rest-version must be one of "
                                        + RestVersion.getValidIdentifiers()
                                        + " in " + path + " not '"
                                        + restVersion + "'");
                    }
                    cfg.setRestVersion(ver);
                }
            } else if (path.matches("/config/conditions")) {
            	// break backwards compatibility of legacy /config/conditions construct and
            	// indicate use of new /config/sso-traffic/by-sity/cctx-mapping/policy-source
            	// objects instead
            	throw new IllegalArgumentException("The <conditions> element and "
            			+ "nested <condition> declarations are no longer supported "
            			+ "directly within "
            			+ "the wamulator config file's XML. <conditions> has been "
            			+ "replaced by one-to-many <policy-source> elements within "
            			+ "the <cctx-mapping> element."
            			+ "See wiki documentation for how to move your existing "
            			+ "<conditions> element and its content to one or more "
            			+ "<policy-source> declarations. The newer policy-source "
            			+ "configuration should point to an xml file that is in the "
            			+ "same format as the policy exposee export. This greatly "
            			+ "simplifies wamulator configuration.");
            } else if (path.matches("/config/proxy-timeout")) {
                cfg.setProxyInboundSoTimeout(getIntegerAtt("inboundMillis",
                        path, atts));
                cfg.setProxyOutboundSoTimeout(getIntegerAtt("outboundMillis",
                        path, atts));
            } else if (path.matches("/config/sso-cookie")) {
                cfg.setCookieName(getStringAtt("name", path, atts));
                cfg.getSessionManager().setMasterCookieDomain(getStringAtt("domain", path, atts));
                cfg.getSessionManager().addCookieDomain(getStringAtt("domain", path, atts));
                int timeout = getIntegerAtt("session-timeout-seconds", path,
                        atts, false);
                if (timeout != -1) {
                    SessionManager sman = cfg.getSessionManager();
                    sman.setSessionInactivityTimeoutSeconds(timeout);
                }
            } else if (path.matches("/config/sso-cookie/cdsso")) {
                cfg.getSessionManager().addCookieDomain(getStringAtt("domain", path, atts));
            } else if (path.matches("/config/port-access")) {
            	cfg.setAllowingLocalTrafficOnly(Boolean.parseBoolean(getStringAtt("local-traffic-only", path, atts)));
            } else if (path.matches("/config/proxy-tls")) {
                cfg.setProxyHttpsPort(getIntegerAtt("https-port", path, atts));
                cfg.setProxyHttpsCertHost(getStringAtt("cert-host", path, atts));
                cfg.setProxyHttpsEnabled(true);
            } else if (path.matches("/config/console-recording")) {
                boolean sso = Boolean.parseBoolean(getStringAtt("sso", path,
                        atts));
                cfg.getTrafficRecorder().setRecording(sso);
                boolean rest = Boolean.parseBoolean(getStringAtt("rest", path,
                        atts));
                String consoleTitle = getStringAtt("title", path, atts, false);
                
                if (consoleTitle != null && ! "".equals(consoleTitle)) {
                	cfg.setConsoleTitle(consoleTitle);
                }
                cfg.getTrafficRecorder().setRecordingRest(rest);
                Integer maxEntries = Config.MAX_TRAFFIC_ENTRIES;
                try {
                    maxEntries = Integer.parseInt(getStringAtt("max-entries",
                            path, atts));
                } catch (NumberFormatException e) {
                    // Just let the default be set
                }
                cfg.setMaxEntries(maxEntries);
                boolean debugLoggingEnabled = Boolean
                        .parseBoolean(getStringAtt("enable-debug-logging",
                                path, atts));
                cfg.setDebugLoggingEnabled(debugLoggingEnabled);
            } else if (path.matches("/config/sso-sign-in-url")) {
                String signin = atts.getValue("value");
                if (signin != null && 
                    (
                     (
                      signin.contains(Config.CONSOLE_PORT_MACRO) && 
                      cfg.getConsolePort() == 0 && // auto binding
                      !get(PARSING_ALIASES).containsAlias(Config.CONSOLE_PORT_ALIAS)
                     ) || 
                     (
                      signin.contains(Config.PROXY_PORT_MACRO) && 
                      cfg.getProxyPort() == 0 && // auto binding
                      !get(PARSING_ALIASES).containsAlias(Config.PROXY_PORT_ALIAS)
                     )
                    )
                   ) {
                    cfg.setSignInRequiresResolution();
                    cfg.setSignInPage(signin); // resolve alias after start-up
                } else {
                    cfg.setSignInPage(getStringAtt("value", path, atts));
                }
                cfg.setSignInPageAction(getStringAtt("formAction", path, atts, false));
                
            } else if (path.matches("/config/sso-header")) {
            	// break backwards compatibility of legacy /config/sso-header 
            	// construct and indicate use of new 
            	// <cctx-mapping>/headers/fixed-value objects instead
            	throw new IllegalArgumentException("The /config/sso-header element "
            			+ "is no longer supported. Use "
            			+ "cctx-mapping/headers/fixed-value elements instead. "
            			+ GlobalHeaderNames.SERVICE_URL 
            			+ " is injected automatically.");
            } else if (path.matches("/config/sso-traffic")) {
                cfg.setStripEmptyHeaders(Boolean.parseBoolean(getStringAtt(
                        "strip-empty-headers", path, atts, false)));
                if (cfg.getStripEmptyHeaders()) {
                    LogUtils.info(cLog, "Proxy is configured to strip empty headers.");
                }
            } else if (path.matches("/config/sso-traffic/by-site")) {
                InboundScheme scheme = InboundScheme.fromMoniker(atts.getValue("scheme"));
                String host = getStringAtt("host", path, atts);
                int port = -1;
                String portS = atts.getValue("port");
                // check for auto proxy-port being used and allow port to follow
                // if indicated
                if (cfg.getProxyPort() == 0 && portS != null
                        && portS.equals("{{proxy-port}}")) {
                    // for letting host choose the port to bind to for the proxy
                    // we still want to be able to define sites with a port
                    // matching
                    // that of the proxy catching the traffic in the by-site
                    // config element. hence we allow for a macro matching the
                    // attribute name for the proxy port on the config element
                    // and figure out the port at runtime.

                    port = 0;
                } else {
                    port = getIntegerAtt("port", path, atts);
                }
                TrafficManager trafficMgr = cfg.getTrafficManager();
                SiteMatcher m = trafficMgr.getSite(scheme, host, port);
                // causes reuse of existing site matcher
                if (m == null) {
                    m = new SiteMatcher(scheme, host, port, trafficMgr);
                    trafficMgr.addMatcher(m);
                }
                parsingContextAccessor.get().put(PARSING_CURR_SITE, host);
            } else if (path.matches("/config/sso-traffic/by-site/cctx-file")) {
                String cctx = getStringAtt("cctx", path, atts);
                String file = getStringAtt("file", path, atts);
                // for now require * termination to convey only prefix matching
                // is currently supported. remove this once backwards references
                // and env macros are added.
                if (!cctx.endsWith("*")) {
                    throw new IllegalArgumentException("cctx must end with '*' in " + path);
                }
                String type = getStringAtt("content-type", path, atts, false);
                TrafficManager trafficMgr = cfg.getTrafficManager();
                SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
                EndPoint ep = sm.getEndpointForCanonicalUrl(cctx);
                if (ep != null) {
                    throw new IllegalArgumentException(
                            "Mapping for cctx='"
                                    + cctx
                                    + "*' in "
                                    + path
                                    + " is consumed by cctx mapping for '"
                                    + ep.getContextRoot()
                                    + "*' which precedes it in document order and hence "
                                    + "will never receive any requests.");
                }
                sm.addFileMapping(cctx, file, type);
                UnenforcedUri ue = new UnenforcedUri(sm.getScheme(), sm.getHost(), sm.getPort(), cctx, null, cctx);
            	sm.addUnenforcedUri(ue);
            } else if (path.matches("/config/sso-traffic/by-site/cctx-mapping")) {
                curCctx = getStringAtt("cctx", path, atts, false);
                if (curCctx != null && !curCctx.isEmpty()) {
                	LogUtils.warning(cLog, "The cctx attribute on the cctx-mapping element is deprecated.");
                }
                curThost = getStringAtt("thost", path, atts);
                curOutgoingScheme = OutboundScheme.fromMoniker(getStringAtt("tscheme", path, atts, false));
                curPolicyServiceGateway = getStringAtt("policy-service-url-gateway", path, atts, false);
                curSchemeHeaderOvrd = getStringAtt("scheme-header-name", path, atts, false);
                curInjectSchemeHeader = getStringAtt("inject-scheme-header", path, atts, false);
                curInjectScheme = (curInjectSchemeHeader == null ? true : Boolean.parseBoolean(curInjectSchemeHeader));
                curHostHdr = getStringAtt("host-header", path, atts, false);
                if (getStringAtt("scheme", path, atts, false) != null) {
                	throw new SAXException("Attribute 'scheme' of " + path
                			+ " has been replaced by 'tscheme'. Please replace "
                			+ "'scheme' with 'tscheme'.");
                }
                curIncomingScheme = InboundScheme.fromMoniker(getStringAtt("cscheme", path, atts, false));
                String preserveHost = getStringAtt("preserve-host", path, atts, false);
                if (curHostHdr != null) {
                	preserveHost = "false";
                }
                curPreserveHost = (preserveHost == null ? true : Boolean.parseBoolean(preserveHost));
                curTport = -1;
                String tportS = atts.getValue("tport");
                // check for auto console-port being used and allow tport to
                // follow if indicated
                if (cfg.getConsolePort() == 0 && tportS != null
                        && tportS.equals(Config.CONSOLE_PORT_MACRO)) {
                    // for letting host choose the port to bind to for the
                    // console
                    // we still want to be able to direct to the console port in
                    // cctw-mappings. hence we allow for a macro matching the
                    // attribute name for the console port on the config element
                    // and figure out the port at runtime.

                    curTport = 0;
                } else {
                    curTport = getIntegerAtt("tport", path, atts);
                }

                curTsslPort = getIntegerAtt("tSslPort", path, atts, false);
                if (curTsslPort == -1) {
                    if (curOutgoingScheme == OutboundScheme.SAME) {
                    	throw new IllegalArgumentException("tscheme of " 
                    			+ path + " specifies 'same' but no tSslPort is specified for outbound https traffic.");
                    }
                    if (curOutgoingScheme == OutboundScheme.HTTPS) {
                    	throw new IllegalArgumentException("tscheme of " 
                    			+ path + " specifies 'https' but no tSslPort is specified for outbound https traffic.");
                    }
                }
            } else if (path.matches("/config/sso-traffic/by-site/cctx-mapping/policy-source")) {    
            	WamulatorPolicySource src = new WamulatorPolicySource(parsingContextAccessor, curThost, curHostHdr, 
            			curPolicyServiceGateway,curSchemeHeaderOvrd, curInjectSchemeHeader, curPreserveHost, 
            			curInjectScheme, curOutgoingScheme, curIncomingScheme, curTsslPort, curTport);
            	parsingContextAccessor.get().put(PARSING_CURR_EXT_POL_SRC, src);
                parsingContextAccessor.get().put(PARSING_POL_SRC_CONTENT, new StringBuffer());
            } else if (path.matches("/config/sso-traffic/by-site/cctx-mapping/headers")) {
            	// break backwards compatibility of legacy /config/sso-traffic/by-site/cctx-mapping/headers 
            	// construct and indicate use of new /config/sso-traffic/by-sity/cctx-mapping/policy-source
            	// objects instead
            	throw new IllegalArgumentException("The <headers> element and "
            			+ "nested <fixed-value>, <profile-att>, <purge-header> "
            			+ "declarations are no longer supported directly within "
            			+ "the wamulator config file's XML. <headers> has been "
            			+ "replaced by one-to-many <policy-source> elements within "
            			+ "the <cctx-mapping> element."
            			+ "See wiki documentation for how to move your existing "
            			+ "<headers> element and its content to one or more "
            			+ "<policy-source> declarations. The newer policy-source "
            			+ "configuration should point to an xml file that is in the "
            			+ "same format as the policy exposee export. This greatly "
            			+ "simplifies wamulator configuration.");
            } else if (path.matches("/config/sso-traffic/by-site/allow")) {
            	// break backwards compatibility of legacy /config/sso-traffic/by-site/allow construct
            	// and indicate use of new /config/sso-traffic/by-sity/cctx-mapping/policy-source
            	// objects instead
            	throw new IllegalArgumentException("The <allow> and <unenforced elements are no "
            			+ "longer supported directly within the wamulator config file's XML. "
            			+ "The <allow> and <unenforced elements have been "
            			+ "replaced by one-to-many <policy-source> elements within "
            			+ "the <cctx-mapping> element."
            			+ "See wiki documentation for how to move your existing "
            			+ "<allow> and <unenforced> elements to one or more "
            			+ "<policy-source> declarations. The newer policy-source "
            			+ "configuration should point to an xml file that is in the "
            			+ "same format as the policy exposee export. This greatly "
            			+ "simplifies wamulator configuration.");
            } else if (path.matches("/config/sso-traffic/by-site/entitlements")) {
                // used only for containment of nested <allow> elements to
                // differentiate
                // between course grained (URL) policies and fine grained
                // permissions
                // (entitlements).
            } else if (path
                    .matches("/config/sso-traffic/by-site/entitlements/allow")) {
                String actionAtt = null;
                actionAtt = getStringAtt("action", path, atts);
                actionAtt = actionAtt.replace(" ", "");
                String[] actions = actionAtt.split(",");
                String cond = null;
                actionAtt = getStringAtt("condition", path, atts);
                try {
                    cond = getCondition(path, atts, true);
                } catch (EvaluationException ee) {
                    throw new SAXException(ee);
                }
                String syntax = get(PARSING_ALIASES).getAliasValue(cond);

                String urn = null;
                String policyDomain = (String) parsingContextAccessor.get()
                        .get(PARSING_CURR_SITE);

                urn = getStringAtt("urn", path, atts);
                urn = Alias.resolveAliases(urn);
                if (!urn.startsWith("/")) {
                    throw new IllegalArgumentException(
                            "Attribute 'urn' with value "
                                    + urn
                                    + " for "
                                    + path
                                    + " must start with a slash '/' character since "
                                    + "they are relative to the by-site host "
                                    + policyDomain
                                    + " as the urn's policy domain.");
                }
                Entitlement ent = new Entitlement(policyDomain, urn, actions, cond, syntax);
                EntitlementsManager entMgr = cfg.getEntitlementsManager();
                entMgr.addEntitlement(ent);
            } else if (path.matches("/config/users")) {
            	// break backwards compatibility of legacy /config/users 
            	// construct and indicate use of new /config/user-source
            	// objects instead
            	throw new IllegalArgumentException("The <users> element and "
            			+ "nested <user> declarations are no longer supported "
            			+ "directly within "
            			+ "the wamulator config file's XML. <users> has been "
            			+ "replaced by one-to-many <user-source> elements. And its "
            			+ "session-timeout-seconds attribute has been moved to "
            			+ "the <sso-cookie> element. "
            			+ "See wiki documentation for how to move your existing "
            			+ "<users> element and its content to one or more "
            			+ "<user-soure type='xml'> declarations or leverage "
            			+ "the new CODA and LDAP user sources.");
            } else if (path.matches("/config/user-source")) {
                String type = getStringAtt("type", path, atts);
                ExternalUserSource src = null;
                
                if (type.equals("xml")) {
                	src = new WamulatorUserSource();
                }
                else if (type.equals("coda")) {
                	src = new CodaUserSource();
                }
                else if (type.equals("ldap")) {
                	src = new LdapUserSource();
                }
// for when/if we allow flowing of users from one wamulator to another such as
// when having a wamulator per domain secure.lds.org versus lds.org with users
// only defined in one and pulled to the other.
//                
//                else if (type.equals("wamulator")) {
//                	src = new OtherWamulatorUserSource();
//                }
                else {
                	throw new IllegalArgumentException("user-source at "
                			+ path + " does not have a valid type '"
                			+ type + "'. Must be one of 'xml', 'coda', or 'ldap'.");
//        			+ type + "'. Must be one of 'xml', 'coda', 'ldap', or 'wamulator'.");
                }
                String stopOnFoundS = getStringAtt("stopOnFound", path, atts, false);
                if (stopOnFoundS == null) {
                	stopOnFoundS = "true"; // default
                }
                src.setUserManager(cfg.getUserManager());
                parsingContextAccessor.get().put(PARSING_CURR_EXT_USR_SRC, src);
                parsingContextAccessor.get().put(PARSING_USR_SRC_CONTENT, new StringBuffer());
            } else if (path.matches("/config/sso-traffic/rewrite-redirect")) {
                String from = getStringAtt("from", path, atts);
                String to = getStringAtt("to", path, atts);
                TrafficManager mgr = cfg.getTrafficManager();
                mgr.addRewriteForRedirect(from, to);
            } else if (path.matches("/config/sso-traffic/rewrite-cookie")) {
                String from = getStringAtt("from-path", path, atts);
                String to = getStringAtt("to-path", path, atts);
                TrafficManager mgr = cfg.getTrafficManager();
                mgr.addRewriteForCookie(from, to);
            } else if (path.matches("/config/sso-entitlements")) {
                throw new IllegalArgumentException(
                        "ERROR: The sso-entitlements element "
                                + "must now reside within its corresponding "
                                + "/config/sso-traffic/by-site element and be renamed to just 'entitlements'. "
                                + "Its policy-domain attribute is no longer used but its nested content "
                                + "remains unchanged. Please apply these changes and restart.");
            }
        }

        public void endElement(String uri, String localName, String name)
                throws SAXException {
            Path path = (Path) parsingContextAccessor.get().get(PARSING_PATH);
            Config cfg = (Config) parsingContextAccessor.get().get(
                    PARSING_CONFIG);

            if (path.matches("/config/user-source")) {
                ExternalUserSource src = (ExternalUserSource) parsingContextAccessor.get().get(PARSING_CURR_EXT_USR_SRC);
                StringBuffer chars = (StringBuffer) parsingContextAccessor.get().get(PARSING_USR_SRC_CONTENT);
                try {
					src.setConfig(path, parseSourceContent(path, chars.toString()));
				}
				catch (ConfigurationException e) {
					throw new SAXException("Unable to load external source at " + path, e);
				}
                cfg.addExternalUserSource(src);
            } else if (path.matches("/config/sso-traffic/by-site/cctx-mapping/policy-source")) {
            	WamulatorPolicySource src = (WamulatorPolicySource) parsingContextAccessor.get().get(PARSING_CURR_EXT_POL_SRC);
            	StringBuffer chars = (StringBuffer) parsingContextAccessor.get().get(PARSING_POL_SRC_CONTENT);
            	try {
            		src.setConfig(path, parseSourceContent(path, chars.toString()));
            	} catch (ConfigurationException e) {
            		throw new SAXException("Unable to load external source at " + path, e);
            	}
            } else if (path.matches("/config")) {
            	if (cfg.getExternalUserSources().size() == 0) {
            		cLog.log(Level.WARNING, "No User Sources were specified. User Authenitcation can NOT take place.");
            	}
        		// verify that by-site host was declared for master cookie domain
        		// so that we have a place to house the sign-in page
        		// TODO alias the sign-in page to the current domain when redirecting
        		// ie: to all domains and use cookies instead of goto query parm
                if (cfg.getTrafficManager().getMasterSite() == null){
                    throw new IllegalStateException("The master cookie domain was set to '"
                            + cfg.getSessionManager().getMasterCookieDomain() 
                            + "' but no site was defined that resides in that " 
                            + "domain. The sign-in page requires that such "
                            + "a site be defined so that it can be presented "
                            + "within that site's host and thus be able to set the cookie " 
                            + "for that domain.");
                }
            }
            path.remove(name);
        }

        /**
         * Parses textual content of a /config/user-source element which should
         * be in the form of a single macro once leading and trailing white 
         * space is removed or if not that them java.util.Properties textual
         * format. For the former the macro is resolved and is assumed to be 
         * in java.util.Properites forms.
         * In text
         * @param path 
         * @param string
         * @return
         */
        private Properties parseSourceContent(Path path, String content) {
        	// see if we are injecting all config via a macro
        	content = content.trim();
        	if (content.startsWith(Alias.MACRO_START) && content.endsWith(Alias.MACRO_END)) {
        		content = Alias.resolveAliases(content);
        	}
        	// now parse first then resolve aliases afterward allowing for
        	// injection of xml for a single property
        	Properties props = new Properties();
        	try {
				props.load(new StringReader(content));
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Unable to load Properties  configuration for "
						+ path + ". Content: " + content, e);
			}
        	
        	// now go through each property and resolve any aliases in its value
        	Properties resolved = new Properties();
        	for(Entry<Object, Object> ent : props.entrySet()) {
        		String nm = (String) ent.getKey();
        		String val = (String) ent.getValue();
        		val = Alias.resolveAliases(val);
        		resolved.setProperty(nm, val);
        	}
			return resolved;
		}


		public void characters(char[] ch, int start, int length)
                throws SAXException {
            Path path = (Path) parsingContextAccessor.get().get(PARSING_PATH);

            if (path.matches("/config/user-source")) {
                StringBuffer chars = (StringBuffer) parsingContextAccessor.get().get(PARSING_USR_SRC_CONTENT);
                chars.append(ch, start, length);
            } else if (path.matches("/config/sso-traffic/by-site/cctx-mapping/policy-source")) {
            	StringBuffer chars = (StringBuffer) parsingContextAccessor.get().get(PARSING_POL_SRC_CONTENT);
            	chars.append(ch, start, length);
            }
        }

        public void endDocument() throws SAXException {
        }

        public void endPrefixMapping(String prefix) throws SAXException {
        }

        public void ignorableWhitespace(char[] ch, int start, int length)
                throws SAXException {
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void skippedEntity(String name) throws SAXException {
        }

        public void startDocument() throws SAXException {
        }

        public void startPrefixMapping(String prefix, String uri)
                throws SAXException {
        }
    }

    
    /**
     * Handles processing of alias processing instruction and condition elements
     * allowing for these to be located anywhere in the file and not necessarily 
     * at the top. This is for allowing conditions to be placed at the end of 
     * the file but be referenced anywhere else.
     * 
     * @author BOYDMR
     *
     */
    public static class CfgAliasesHandler implements ContentHandler {

        public CfgAliasesHandler() {
        }

        /**
         * Enables processing instructions in the XML having the following three
         * forms. Each declares an alias that can then be referenced in any
         * configuration file attribute value by including the macro "{{name}}"
         * within the attribute's string of characters or within the value
         * portion of any later occurring alias declarations including classpath
         * files or resolved resources. Each has a specific use.
         * 
         * <pre>
         * [1] &lt;?alias name=[value]?&gt;
         * [2] &lt;?alias name=classpath:[resource-file-path]?&gt;
         * [3] &lt;?alias name=system:[resource-name]?&gt;
         * </pre>
         * 
         * [1] adds a named value pair to the map of aliases where the value is
         * the literal text although that text could be a macro causing this
         * alias to have the same value as the other alias or it could have one
         * or more alias embedded within it surrounded by literal text.
         * 
         * [2] adds a named value pair to the map of alias where the value is
         * the character content of the referenced resource file that must be
         * available from the classpath. Making such file based content
         * available is not accomplished by conceptually embedding such content
         * within the XML declaring the alias and hence won't effect the xml
         * processing. This allows for alias values to contain XML sensitive
         * characters and hence XML constructs. Such character content can also
         * include macro references for earlier defined alias which will be
         * resolved upon loading.
         * 
         * [3] adds a named value pair to the map of aliases where the value is
         * the character content of a java.lang.System property. This version is
         * used to emulate the classpath version during unit tests without
         * having to create such files. In particular, this enables condition
         * syntax to be used in unit tests.
         */
        public void processingInstruction(String target, String data)
                throws SAXException {
            if (target.equals("alias")) {
            	Alias alias = Alias.fromString(null, data);
            	get(PARSING_ALIASES).addAlias(alias);
            } else if ( target.endsWith("alias") ) {
            	get(PARSING_ALIASES).addAlias(Alias.fromString(target, data));
            }
        }

        public void startElement(String uri, String localName, String name,
                Attributes atts) throws SAXException {
            
        	Path path = (Path) parsingContextAccessor.get().get(PARSING_PATH);
            path.add(name);
        }

        public void endElement(String uri, String localName, String name)
                throws SAXException {
            Path path = (Path) parsingContextAccessor.get().get(PARSING_PATH);
            path.remove(name);
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void endPrefixMapping(String prefix) throws SAXException {
        }

        public void ignorableWhitespace(char[] ch, int start, int length)
                throws SAXException {
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void skippedEntity(String name) throws SAXException {
        }

        public void startDocument() throws SAXException {
        }

        public void startPrefixMapping(String prefix, String uri)
                throws SAXException {
        }
    }
}