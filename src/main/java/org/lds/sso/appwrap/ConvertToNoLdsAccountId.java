package org.lds.sso.appwrap;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.lds.sso.appwrap.Service.SourceAndReader;
import org.lds.sso.appwrap.XmlConfigLoader2.CPathParts;
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.lds.sso.appwrap.conditions.evaluator.syntax.HasLdsAccountId;
import org.lds.sso.appwrap.conditions.evaluator.syntax.LdsAccount;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.xml.Alias;
import org.lds.sso.appwrap.xml.AliasHolder;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class ConvertToNoLdsAccountId {
	private static final Logger cLog = Logger.getLogger(ConvertToNoLdsAccountId.class.getName());
	
    public static final String CURR_USER = "current-user";
    public static final String CONDITIONS = "conditions";
    private static final String USER_BY_LDSACCT = "username-by-ldsacc";
    public static final String LDS_ACCT_GROUPING = "lds-account-grouping";
    
	/**
	 * Records where a condition is used in the xml configuration file and the
	 * type of use as either sso-entitlement or by-site/allow.
	 */
	public static class Use {
	    public String urn = null; // only if sso-entitlement
	    public String cpath = null; // only if by-site/allow
	    public String xmlpath = null;
	    
	    private Use(String xmlpath, String urn, String cpath) {
	        this.xmlpath = xmlpath;
	        this.urn = urn;
	        this.cpath = cpath;
	    }
	    
	    static Use entitlement(String path, String urn) {
	        return new Use(path, urn, null);
	    }
	    static Use allow(String path, String cpath) {
	        return new Use(path, null, cpath);
	    }
	}
	
	/**
	 * Holds info about a condition, where it was declared in the XML, and 
	 * any lds account grouping discovered in its syntax.
	 * 
	 * @author BOYDMR
	 *
	 */
	private static class Condition {
	    public final Alias alias;
	    public final List<Use> uses;
        public final Map<String, Set<String>> groupings = new TreeMap<String, Set<String>>();
	    
	    public Condition(Alias syntax) {
	        this.alias = syntax;
	        this.uses = new ArrayList<Use>();
	    }
	}

	/**
	 * Parses config file gathering alias, identifying those that are condition
	 * references, and captures the users having lds account ids and lds 
	 * application declarations.
	 * 
	 * @author BOYDMR
	 *
	 */
	public static class ConversionContentHandler extends XmlConfigLoader2.CfgContentHandler {
        private ThreadLocal<Map<String,Object>> pca = XmlConfigLoader2.parsingContextAccessor;

        public ConversionContentHandler() {
            super();
        }

        public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
            AliasHolder aliases = XmlConfigLoader2.get(XmlConfigLoader2.PARSING_ALIASES);
            //Map<String,String> aliasValues = (Map<String, String>) pca.get().get(XmlConfigLoader2.PARSING_SYNTAXES);
            Map<String, Condition>conditions = (Map<String, Condition>) pca.get().get(CONDITIONS);
            Config cfg = (Config) pca.get().get(XmlConfigLoader2.PARSING_CONFIG);
            Path path = (Path) pca.get().get(XmlConfigLoader2.PARSING_PATH);
            
            path.add(name);
            
            // identify which aliases are condition syntax references and 
            // create:
            // + map of Condition objects by aliasname
            // + list of Uses within each Condition object indicating all 
            //   points where the condition alias is used.
            if (path.matches("/config/sso-traffic/by-site/allow")) {
                CPathParts cp = XmlConfigLoader2.getRelUriAtt("cpath", path, atts);
                String aliasName = null;
                try {
                    aliasName = XmlConfigLoader2.getCondition(path, atts, false);
                } catch (EvaluationException e) {
                    // won't happen since we don't validate
                }
                Alias alias = aliases.getAlias(aliasName);
                Condition c = conditions.get(aliasName);
                
                if (c == null) {
                    LogUtils.fine(cLog, "Identified alias {0} as a condition.", aliasName);
                    c = new Condition(alias);
                    conditions.put(aliasName, c);
                }
                c.uses.add(Use.allow(path.toString(), cp.rawValue));
            }
            else if (path.matches("/config/sso-entitlements/allow")) {
                String urn = XmlConfigLoader2.getStringAtt("urn",path,atts);
                String aliasName = null;
                try {
                    aliasName = XmlConfigLoader2.getCondition(path, atts, false);
                } catch (EvaluationException e) {
                    // won't happen since we don't validate
                }
                Alias alias = aliases.getAlias(aliasName);
                Condition c = conditions.get(aliasName);
                
                if (c == null) {
                	LogUtils.fine(cLog, "Identified alias {0} as a condition.", aliasName);
                    c = new Condition(alias);
                    conditions.put(aliasName, c);
                }
                c.uses.add(Use.entitlement(path.toString(), urn));
            }
            
            // from here down capture: 
            // + declared ldsapplications for a username
            // + username for an ldsaccountid
            else if (path.matches("/config/users/user")) {
                String usrNm = XmlConfigLoader2.getStringAtt("name", path, atts);
                pca.get().put(CURR_USER, usrNm);
            }
            else if (path.matches("/config/users/user/sso-header")) {
                String hdrNm = XmlConfigLoader2.getStringAtt("name", path, atts);
                
                if (hdrNm.equals(UserHeaderNames.LDS_ACCOUNT_ID)) {
                    String hdrVl = XmlConfigLoader2.getStringAtt("value", path, atts);
                    String username = (String) pca.get().get(CURR_USER);
                    Map<String,String> users = (Map<String, String>) pca.get().get(USER_BY_LDSACCT);
                    users.put(hdrVl, username);
                }
            }
        }

        public void endElement(String uri, String localName, String name) throws SAXException {
            Path path = (Path) pca.get().get(XmlConfigLoader2.PARSING_PATH);
            path.remove(name);
        }
	}
	
	/**
	 * Parses a custom condition syntax watching for groupings of users by  
	 * use of now removed HasLdsAccountId elements (HLAIs) and their optional nested
	 * LdsAccountId (LAI) elements. A grouping can be defined by a top level HLAI which
	 * is the trivial case and defines a single grouping with all users that match
	 * the ldsaccountid being members of the grouping.
	 * 
	 * When HLAIs are seen as children of other elements all HLAIs within a single
	 * parent make up a grouping. The goal with identifying each such grouping is
	 * to replace the entire grouping, ie all HLAIs and their nested LAIs with 
	 * a single HasLdsApplication element and define an LdsApplications value
	 * for that condition that can be assigned to each user of the grouping thus
	 * maintaining the grouping without reference to lds account ids. 
	 */
	private static class ConditionContentHandler implements ContentHandler {
        private ThreadLocal<Map<String,Object>> pca = XmlConfigLoader2.parsingContextAccessor;
        private Condition condition = null;
        private int groupingsCount = 0;
        private Stack<Set<String>> ldsAccountIdContainersStack = new Stack<Set<String>>();

	    public ConditionContentHandler(Condition condition) {
            pca.get().put(XmlConfigLoader2.PARSING_PATH, new Path());
            this.condition  = condition;
	    }
        public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
            Path path = (Path) pca.get().get(XmlConfigLoader2.PARSING_PATH);
            path.add(name);
            
            if (name.equals(HasLdsAccountId.class.getSimpleName())) {
                HashSet<String> ldsAccountIds = null;
                
                if (path.isAtTLE()) {
                    ldsAccountIds = new HashSet<String>();
                    pca.get().put(LDS_ACCT_GROUPING, ldsAccountIds);
                }
                else {
                    ldsAccountIds = (HashSet<String>) pca.get().get(LDS_ACCT_GROUPING);
                }
                String id = XmlConfigLoader2.getStringAtt("id", path, atts, false);
                // catch id if had
                if (id != null) {
                    ldsAccountIds.add(id);
                }
            }
            else if (name.equals(LdsAccount.class.getSimpleName())) {
                HashSet<String> ldsAccountIds = (HashSet<String>) pca.get().get(LDS_ACCT_GROUPING);
                String id = XmlConfigLoader2.getStringAtt("id", path, atts, false);
                // catch id
                if (id != null) {
                    ldsAccountIds.add(id);
                }
            }
            else {
                // other parent being entered, create new grouping to catch
                // ldsAccountIds while preserving existing one for when we
                // step back out of this element and back into the parent
                HashSet<String> ldsAccountIds = (HashSet<String>) pca.get().get(LDS_ACCT_GROUPING);
                if (ldsAccountIds != null) {
                    ldsAccountIdContainersStack.push(ldsAccountIds);
                }
                pca.get().put(LDS_ACCT_GROUPING, new HashSet<String>());
            }
        }

        public void endElement(String uri, String localName, String name)
                throws SAXException {
            Path path = (Path) pca.get().get(XmlConfigLoader2.PARSING_PATH);
            
            if (path.isAtTLE() && name.equals(HasLdsAccountId.class.getSimpleName())) {
                // stepping out of HasLdsAccountId TLE
                Set<String> ldsAccountIds = (HashSet<String>) pca.get().get(LDS_ACCT_GROUPING);

                if (ldsAccountIds.size() > 0) {
                    LogUtils.fine(cLog, "Creating grouping {0} for lds account id cluster in {1} at {2}", condition.alias.name, condition.alias, path.toString());
                    condition.groupings.put(condition.alias.name, ldsAccountIds);
                }
            }
            else if (! name.equals(HasLdsAccountId.class.getSimpleName()) &&
                    ! name.equals(LdsAccount.class.getSimpleName())) {
                // stepping out of non-HasLdsAccountId or related nested class
                // see if we have a grouping
                Set<String> ldsAccountIds = (HashSet<String>) pca.get().get(LDS_ACCT_GROUPING);

                if (ldsAccountIds.size() > 0) {
                    String grpName = condition.alias.name + "-" + (++groupingsCount);
                    LogUtils.fine(cLog, "Creating grouping {0} for lds account id cluster in {1} at {2}", grpName, condition.alias, path.toString());
                    condition.groupings.put(grpName, ldsAccountIds);
                }
                // now restore ldsaccountid container of parent in case it has
                // more children that are HLAIs.
                if (!ldsAccountIdContainersStack.empty()) {
                    Set<String> ids = ldsAccountIdContainersStack.pop();
                    pca.get().put(LDS_ACCT_GROUPING, ids);
                }
            }
            path.remove(name);
        }

        public void endDocument() throws SAXException {
        }

        public void characters(char[] arg0, int arg1, int arg2)
        throws SAXException {
        }

        public void endPrefixMapping(String arg0) throws SAXException {
        }

        public void ignorableWhitespace(char[] arg0, int arg1, int arg2)
                throws SAXException {
        }

        public void processingInstruction(String arg0, String arg1)
                throws SAXException {
        }

        public void setDocumentLocator(Locator arg0) {
        }

        public void skippedEntity(String arg0) throws SAXException {
        }

        public void startDocument() throws SAXException {
        }

        public void startPrefixMapping(String arg0, String arg1)
                throws SAXException {
        }
	}
	
	public static class Grouping {
	    public String message = null;
	    public Map<String,String> userMessages = new TreeMap<String,String>();
        private String name = null;
	    
	    public Grouping(String name, String message) {
	        this.name  = name;
	        this.message = message;
	    }
	}
	
	public static void main(String[] args) throws Exception {
	    Map<String, Grouping> directives = getGroupingDirectives(args);
	    
	    for(Grouping g : directives.values()) {
	        System.out.println();
	        System.out.println(g.message);
	        for ( String usrMsg : g.userMessages.values()) {
	            System.out.println(usrMsg);
	        }
	    }
	}

    public static Map<String, Grouping> getGroupingDirectives(String[] args) throws Exception {
        // create dataset output containers that will be filled during parsing
        Map<String, Condition> conditions = new TreeMap<String, Condition>();
        Map<String, String> userById = new HashMap<String, String>();
        
        Map<String, Object> pCtx = XmlConfigLoader2.parsingContextAccessor.get();
        pCtx.put(USER_BY_LDSACCT, userById); // for user names given ldsaccountid
        pCtx.put(CONDITIONS, conditions); // for condition info objects givent an alias name
        
        // process config file and referenced condition syntax sources
        Service.verifyArgs(args);
        SourceAndReader sar = Service.getCfgReader(args[0]);
        Reader reader = sar.getReader();
        XmlConfigLoader2.load(reader, args[0], new ConversionContentHandler());
        Map<String, Grouping> directives = new TreeMap<String, Grouping>();
        
        for(String aliasname : conditions.keySet()) {
            Condition c = conditions.get(aliasname);
            XmlConfigLoader2.load(new StringReader(c.alias.value), "conditionAlias:" + aliasname, new ConditionContentHandler(c));
            
            
            if (c.groupings.size() == 0) {
                LogUtils.warning(cLog, "No use of HasLdsAccountId in Condition '{0}'.", c.alias.name);
            }
            else {
                for(String grpNm : c.groupings.keySet()) {
                    Grouping dir = new Grouping(grpNm, "--->>> For grouping '" 
                            + grpNm + "' found in '" + c.alias 
                            + "' replace all <HasLdsAccountId/> elements with a single <HasLdsApplication value='" 
                            + grpNm + "'/> element.");
                    directives.put(grpNm, dir);
                    
                    for (String id : c.groupings.get(grpNm)) {
                        String username = userById.get(id);
                        if (username != null) {
                            dir.userMessages.put(username, "--- for user " + username
                                    + " add a nested element <ldsapplications value='" + grpNm + "'/>");
                        }
                    }
                }
            }
        }
        return directives;
    }
}
