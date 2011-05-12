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
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.LogicalSyntaxEvaluationEngine;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.io.SimpleErrorHandler;
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
    public static final String PARSING_IS_IN_CONDITION = "is-in-condition";
    public static final String PARSING_CONDITION_CONTENT = "condition-content";
    public static final String PARSING_CONDITION_ALIAS = "condition-alias";

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
    static final ThreadLocal<Map<String,Object>> parsingContextAccessor =
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
        private String compositePath = "/";

        /**
         * Adds a step to the path being traversed.
         * 
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
    static String getStringAtt(String attName, Path pathToElement,
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
    static String getStringAtt(String attName, Path pathToElement,
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
        OrderedUri ou = sm.getManagerOfUri(sm.getScheme(), sm.getHost(), sm
                .getPort(), cp.path, cp.query);
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
                // prevents log of unsupported element below since captured here
            } else if (path.matches("/config/conditions/condition")) {
                // allows nested elements to be ignored without warning message of unsupported elements
                parsingContextAccessor.get().put(PARSING_IS_IN_CONDITION, Boolean.TRUE);
            } else if (path.matches("/config/proxy-timeout")) {
                cfg.setProxyInboundSoTimeout(getIntegerAtt("inboundMillis",
                        path, atts));
                cfg.setProxyOutboundSoTimeout(getIntegerAtt("outboundMillis",
                        path, atts));
            } else if (path.matches("/config/sso-cookie")) {
                cfg.setCookieName(getStringAtt("name", path, atts));
                cfg.getSessionManager().setMasterCookieDomain(getStringAtt("domain", path, atts));
                cfg.getSessionManager().addCookieDomain(getStringAtt("domain", path, atts));
            } else if (path.matches("/config/sso-cookie/cdsso")) {
                cfg.getSessionManager().addCookieDomain(getStringAtt("domain", path, atts));
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
            } else if (path.matches("/config/sso-header")) {
                String hdrNm = getStringAtt("name", path, atts);
                String hdrVl = getStringAtt("value", path, atts);
                if (hdrNm.equals(UserHeaderNames.SERVICE_URL)) {
                    String msg = "NOTE: Global sso-header declaration for '"
                            + hdrNm
                            + "' is now ignored since it is automatically generated.";
                } else {
                    cfg.addGlobalHeader(hdrNm, hdrVl);
                }
            } else if (path.matches("/config/sso-traffic")) {
                cfg.setStripEmptyHeaders(Boolean.parseBoolean(getStringAtt(
                        "strip-empty-headers", path, atts, false)));
                if (cfg.getStripEmptyHeaders()) {
                    LogUtils.info(cLog, "Proxy is configured to strip empty headers.");
                }
            } else if (path.matches("/config/sso-traffic/by-site")) {
                String scheme = atts.getValue("scheme");
                if (scheme == null || scheme.equals("")) {
                    scheme = "http";
                }
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
                SiteMatcher m = trafficMgr.getSite(host, port);
                // causes reuse of existing site matcher but this will need to
                // change if/when we support any other protocol besides http 
                // since SiteMatcher constructor takes scheme but 
                // trafficMgr.getSite() only uses host and port to distinguish 
                // between them.
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
                    throw new IllegalArgumentException(
                            "cctx must end with '*' in " + path);
                }
                cctx = cctx.substring(0, cctx.length() - 1);
                String type = getStringAtt("content-type", path, atts);
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
                                    + ep.getCanonicalContextRoot()
                                    + "*' which precedes it in document order and hence "
                                    + "will never receive any requests.");
                }
                sm.addFileMapping(cctx, file, type);
            } else if (path.matches("/config/sso-traffic/by-site/cctx-mapping")) {
                String cctx = getStringAtt("cctx", path, atts);
                String thost = getStringAtt("thost", path, atts);
                String policyServiceGateway = getStringAtt("policy-service-url-gateway", path, atts, false);
                String hostHdr = getStringAtt("host-header", path, atts, false);
                String scheme = getStringAtt("scheme", path, atts,
                        false);
                String preserveHost = getStringAtt("preserve-host", path, atts,
                        false);
                boolean preserve = (preserveHost == null ? true : Boolean
                        .parseBoolean(preserveHost));
                int tport = -1;
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

                    tport = 0;
                } else {
                    tport = getIntegerAtt("tport", path, atts);
                }
                String tpath = getStringAtt("tpath", path, atts);
                // enforce terminating asterisk solely for reminding readers of
                // the config file that these are uri root contexts that will be
                // rewritten. We could add support for regex like mod-rewrite.
                if (!cctx.endsWith("*") || !tpath.endsWith("*")) {
                    throw new IllegalArgumentException(
                            "cctx and tpath values must end with '*' in "
                                    + path);
                }
                cctx = cctx.substring(0, cctx.length() - 1);
                tpath = tpath.substring(0, tpath.length() - 1);
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
                                    + ep.getCanonicalContextRoot()
                                    + "*' which precedes it in document order and hence "
                                    + "will never receive any requests.");
                }
                ep = new AppEndPoint(sm.getHost(), cctx, tpath, thost, tport, scheme, preserve, hostHdr, policyServiceGateway);
                sm.addMapping(ep);
            } else if (path.matches("/config/sso-traffic/by-site/unenforced")) {
                TrafficManager trafficMgr = cfg.getTrafficManager();
                SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
                CPathParts cp = getRelUriAtt("cpath", path, atts);
                isDeclarationUseful(sm, cp, path);
                UnenforcedUri uu = new UnenforcedUri(sm.getScheme(), sm
                        .getHost(), sm.getPort(), cp.path, cp.query,
                        cp.rawValue);
                sm.addUnenforcedUri(uu);
            } else if (path.matches("/config/sso-traffic/by-site/allow")) {
                String actionAtt = getStringAtt("action", path, atts);
                actionAtt = actionAtt.replace(" ", "");
                String[] actions = actionAtt.split(",");
                String cond = null;
                try {
                    cond = getCondition(path, atts, true);
                } catch (EvaluationException ee) {
                    throw new SAXException(ee);
                }
                String syntax = get(PARSING_ALIASES).getAliasValue(cond);

                TrafficManager trafficMgr = cfg.getTrafficManager();
                SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
                CPathParts cp = getRelUriAtt("cpath", path, atts);
                isDeclarationUseful(sm, cp, path);
                AllowedUri au = new AllowedUri(sm.getScheme(), sm.getHost(), sm
                        .getPort(), cp.path, cp.query, actions, cp.rawValue);
                sm.addAllowedUri(au, cond, syntax);
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
                String source = getStringAtt("source", path, atts, false);
                cfg.setExternalUserSource(source);

                int timeout = getIntegerAtt("session-timeout-seconds", path,
                        atts, false);
                if (timeout != -1) {
                    SessionManager sman = cfg.getSessionManager();
                    sman.setSessionInactivityTimeoutSeconds(timeout);
                }
            } else if (path.matches("/config/users/user")) {
                String usrNm = getStringAtt("name", path, atts);
                String usrPwd = getStringAtt("pwd", path, atts, false);
                UserManager mgr = cfg.getUserManager();
                mgr.setUser(usrNm, usrPwd);
            } else if (path.matches("/config/users/user/sso-header")) {
                String hdrNm = getStringAtt("name", path, atts);
                String hdrVl = getStringAtt("value", path, atts);
                cfg.getUserManager().addHeaderForLastUserAdded(hdrNm, hdrVl);
            } else if (path.matches("/config/users/user/ldsApplications")) {
                String vl = getStringAtt("value", path, atts);
                cfg.getUserManager().addAttributeForLastUserAdded(
                        User.LDSAPPS_ATT, vl);
            } else if (path.matches("/config/users/user/att")) {
                String nl = getStringAtt("name", path, atts);
                String vl = getStringAtt("value", path, atts);
                cfg.getUserManager().addAttributeForLastUserAdded(
                        nl, vl);
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
            } else {
                if (parsingContextAccessor.get().get(PARSING_IS_IN_CONDITION) != Boolean.TRUE) {
                    LogUtils.severe(cLog, "Unsupported element at {0} ignoring.", path);
                }
            }
        }

        public void endElement(String uri, String localName, String name)
                throws SAXException {
            Path path = (Path) parsingContextAccessor.get().get(PARSING_PATH);
            Config cfg = (Config) parsingContextAccessor.get().get(
                    PARSING_CONFIG);

            if (path.matches("/config/conditions/condition")) {
                parsingContextAccessor.get().put(PARSING_IS_IN_CONDITION,
                        Boolean.FALSE);
            } else if (path.matches("/config")) {
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

        public void characters(char[] ch, int start, int length)
                throws SAXException {
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
            Config cfg = (Config) parsingContextAccessor.get().get(
                    PARSING_CONFIG);
            Path path = (Path) parsingContextAccessor.get().get(PARSING_PATH);

            path.add(name);
            if (path.matches("/config/conditions")) {
                // prevents log of unsupported element below since captured here
            } else if (path.matches("/config/conditions/condition")) {
                String alias = getStringAtt("alias", path, atts);
                parsingContextAccessor.get().put(PARSING_IS_IN_CONDITION, Boolean.TRUE);
                parsingContextAccessor.get().put(PARSING_CONDITION_CONTENT, new StringBuffer());
                parsingContextAccessor.get().put(PARSING_CONDITION_ALIAS, alias);
            } else {
                if (parsingContextAccessor.get().get(PARSING_IS_IN_CONDITION) == Boolean.TRUE) {
                    StringBuffer element = new StringBuffer().append("<")
                            .append(name);
                    for (int i = 0; i < atts.getLength(); i++) {
                        String nm = atts.getQName(i);
                        String vl = atts.getValue(i);
                        vl = vl.replace("'", "&apos;");
                        vl = vl.replace("\"", "&quot;");
                        element.append(" ").append(nm).append("='").append(vl)
                                .append("'");
                    }
                    element.append(">");

                    StringBuffer chars = (StringBuffer) parsingContextAccessor
                            .get().get(PARSING_CONDITION_CONTENT);
                    chars.append(element.toString());
                }
            }
        }

        public void endElement(String uri, String localName, String name)
                throws SAXException {
            Path path = (Path) parsingContextAccessor.get().get(PARSING_PATH);

            if (path.matches("/config/conditions/condition")) {
                StringBuffer chars = (StringBuffer) parsingContextAccessor
                        .get().get(PARSING_CONDITION_CONTENT);
                String alias = (String) parsingContextAccessor.get().get(
                        PARSING_CONDITION_ALIAS);
                String content = chars.toString();
                get(PARSING_ALIASES).addAlias(new Alias(alias, content));
                LogicalSyntaxEvaluationEngine engine = LogicalSyntaxEvaluationEngine.getSyntaxEvalutionInstance();
                try {
                    engine.getEvaluator(alias, content);
                } catch (EvaluationException e) {
                    throw new SAXException(
                            "Invalid condition syntax detected.", e);
                }
                parsingContextAccessor.get().put(PARSING_IS_IN_CONDITION,
                        Boolean.FALSE);
            }
            if (parsingContextAccessor.get().get(PARSING_IS_IN_CONDITION) == Boolean.TRUE) {
                StringBuffer element = new StringBuffer().append("</").append(
                        name).append(">");

                StringBuffer chars = (StringBuffer) parsingContextAccessor
                        .get().get(PARSING_CONDITION_CONTENT);
                chars.append(element.toString());
            }
            path.remove(name);
        }

        public void characters(char[] ch, int start, int length)
                throws SAXException {
            Boolean isInCondition = (Boolean) parsingContextAccessor.get().get(
                    PARSING_IS_IN_CONDITION);
            if (isInCondition == Boolean.TRUE) {
                StringBuffer chars = (StringBuffer) parsingContextAccessor
                        .get().get(PARSING_CONDITION_CONTENT);
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
}