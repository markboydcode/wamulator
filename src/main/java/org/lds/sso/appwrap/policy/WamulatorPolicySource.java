package org.lds.sso.appwrap.policy;

import org.apache.commons.lang.StringUtils;
import org.lds.sso.appwrap.*;
import org.lds.sso.appwrap.AppEndPoint.InboundScheme;
import org.lds.sso.appwrap.AppEndPoint.OutboundScheme;
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.exception.ConfigParserException;
import org.lds.sso.appwrap.identity.ExternalUserSource.ConfigurationException;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.io.SimpleErrorHandler;
import org.lds.sso.appwrap.model.*;
import org.xml.sax.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles parsing and interpretation of the policy file associated with a cctx-mapping element in the config file.
 */
public class WamulatorPolicySource implements ContentHandler {
	private static final Logger cLog = Logger.getLogger(WamulatorPolicySource.class.getName());
	private ThreadLocal<Map<String,Object>> parsingContextAccessor;
	private String cctxThost, cctxHostHdr;
    private String cctxPolicyServiceGateway, cctxSchemeHeaderOvrd, cctxInjectSchemeHeader;
    private boolean cctxPreserveHost, cctxInjectScheme;
    private OutboundScheme cctxOutgoingScheme;
    private InboundScheme cctxIncomingScheme;
    private int cctxTsslPort, cctxTport;
	
	
	public static final String PARSING_CONFIG = "config-instance";
	
	private Path path = new Path();
	private StringBuffer buffer;
	private Attributes curAtts;
	private String cctx;
	
	private Map<String, AuthZRule> authorizationRules = new HashMap<String, AuthZRule>();
	
	// Authorization Rule Variables
	private AuthZRule defAuthZRule;
	private String defAuthZRuleName;
	private Map<String,String> defAuthZProfileHeaders;
	private Map<String,List<String>> defAuthZFixedHeaders;
	private AuthZRule curAuthZRule;
	private Condition curAuthZCondition;
	private Map<String,String> curAuthZProfileHeaders;
	private Map<String,List<String>> curAuthZFixedHeaders;
	
	// Policy Variables
	private Policy curPolicy;
	private Header curPolicyAuthZHeader;
	
	private Policy defPolicy;
	private Header defPolicyAuthZHeader;
	
	private String defAuthNScheme;
	private String defAuthNName;

    // String that holds the name of the policy source file
    // for convenience of logging and debugging.
    private String sourceName;
	
	
	public WamulatorPolicySource(ThreadLocal<Map<String,Object>> parsingContextAccessor, String cctxThost, 
			String cctxHostHdr, String cctxPolicyServiceGateway, String cctxSchemeHeaderOvrd, 
			String cctxInjectSchemeHeader,boolean cctxPreserveHost, boolean cctxInjectScheme, 
			OutboundScheme cctxOutgoingScheme, InboundScheme cctxIncomingScheme, int cctxTsslPort, 
            int cctxTport, String sourceName) {
		this.parsingContextAccessor = parsingContextAccessor;
		this.cctxThost = cctxThost;
		this.cctxHostHdr = cctxHostHdr;
		this.cctxPolicyServiceGateway = cctxPolicyServiceGateway;
		this.cctxSchemeHeaderOvrd = cctxSchemeHeaderOvrd;
		this.cctxInjectSchemeHeader = cctxInjectSchemeHeader;
		this.cctxPreserveHost = cctxPreserveHost;
		this.cctxInjectScheme = cctxInjectScheme;
		this.cctxOutgoingScheme = cctxOutgoingScheme; 
		this.cctxIncomingScheme = cctxIncomingScheme;
		this.cctxTsslPort = cctxTsslPort;
		this.cctxTport = cctxTport;
        this.sourceName = sourceName;
	}

    /**
     * Accepts a java.util.Properties object looking for a contained "xml" key whose
     * value should be XML text in Policy Exposee format.
     *
     * @param path
     * @param props
     * @throws ConfigurationException
     */
	public void setConfig(Path path, Properties props) throws ConfigurationException {
		String xml = props.getProperty("xml");
        
        if (StringUtils.isEmpty(xml)) {
        	cLog.log(Level.WARNING, "No Configuration for xml policy-source in "
        			+ WamulatorPolicySource.class.getSimpleName()
        			+ " at " + path
        			+ ". No Policies will be available from this source.");
        	return;
        }
        parseXml(path, xml);
	}
    
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }
	
	/**
	 * Parses the policy exposee xml format.
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
            cLog.log(Level.SEVERE, "Unable to create parser for loading policy source.", e);
            return;
        }
        rdr.setContentHandler(this);
        InputSource src = new InputSource(new StringReader(xml));
        try {
        	rdr.setErrorHandler(new SimpleErrorHandler(cLog));
            rdr.parse(src);
        } catch (Exception e) {
        	throw new ConfigurationException("Unable to load policies for policy-source at " + path
                    + ". Content parsed as well-formed XML is shown between the lines: \n---------------------\n"
                    + xml + "\n---------------------", e);
        }
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		Config cfg = (Config) parsingContextAccessor.get().get(PARSING_CONFIG);
		path.add(qName);
        curAtts = atts;
        
        if (path.matches("/deployment/application")) {
    		cctx = atts.getValue("cctx");
    		defPolicy = new Policy("Default");
    	} else if (path.matches("/deployment/application/authentication")) { 
    		defAuthNScheme = atts.getValue("scheme");
    		defAuthNName = atts.getValue("name");
		} else if (path.matches("/deployment/application/authorization/default")) {
			defAuthZProfileHeaders = new HashMap<String,String>();
        	defAuthZFixedHeaders = new HashMap<String,List<String>>();
			defAuthZRuleName = atts.getValue("value");
		} else if (path.matches("/deployment/application/authorization/default/headers/success/redirect") ||
     		   path.matches("/deployment/application/authorization/default/headers/failure/redirect") ||
     		   path.matches("/deployment/application/authorization/default/headers/inconclusive/redirect")) {
     	
			String value = atts.getValue("value");
			defPolicyAuthZHeader = new Header("redirect", value);
		} else if (path.matches("/deployment/application/authorization/rule")) {
        	String name = atts.getValue("name");
        	String enabled = atts.getValue("enabled");
        	String precedence = atts.getValue("allow-takes-precedence");
        	
        	if (name.equals(defAuthZRuleName)) {
        		defAuthZRule = new AuthZRule("~~DefaultRule~~", Boolean.parseBoolean(enabled), Boolean.parseBoolean(precedence));
        	}
        	
        	if (!name.equals("~~default-headers~~")) {
        		curAuthZRule = new AuthZRule(name, Boolean.parseBoolean(enabled), Boolean.parseBoolean(precedence));
        	}
        } else if (path.matches("/deployment/application/authorization/rule/allow/condition") ||
        		   path.matches("/deployment/application/authorization/rule/deny/condition")) {
        	
        	String type = atts.getValue("type");
        	String value = atts.getValue("value");
        	if (value == null || value.isEmpty()) {
        		buffer = new StringBuffer();
        	}
        	curAuthZCondition = new Condition(type, value);
        } else if (path.matches("/deployment/application/authorization/default/headers/success/profile-att")) {
        	String name = atts.getValue("name");
        	String attribute = atts.getValue("attribute");
        	defAuthZProfileHeaders.put(name, attribute);
        } else if (path.matches("/deployment/application/authorization/default/headers/success/fixed-value")) {
        	String name = atts.getValue("name");
        	String value = atts.getValue("value");
        	List<String> vals = defAuthZFixedHeaders.get(name);
        	if (vals == null) {
        		vals = new ArrayList<String>();
        		defAuthZFixedHeaders.put(name, vals);
        	}
        	vals.add(value);
		} else if (path.matches("/deployment/application/policy")) {
        	String name = atts.getValue("name");
        	curPolicy = new Policy(name);
        } else if (path.matches("/deployment/application/policy/url") ||
        		   path.matches("/deployment/application/policy/operations") ||
        		   path.matches("/deployment/application/policy/query-string")) {
        	
        	buffer = new StringBuffer();
        } else if (path.matches("/deployment/application/policy/authentication")) {
        	String scheme = atts.getValue("scheme");
        	String name = atts.getValue("name");
        	AuthNScheme authNScheme = new AuthNScheme(scheme, name);
        	curPolicy.setAuthNScheme(authNScheme);
        } else if (path.matches("/deployment/application/policy/authorization")) {
        	curAuthZProfileHeaders = new HashMap<String,String>();
        	curAuthZFixedHeaders = new HashMap<String,List<String>>();
        	String name = atts.getValue("value");
        	curPolicy.setAuthZRuleString(name);
        } else if (path.matches("/deployment/application/policy/authorization/headers/success/redirect") ||
        		   path.matches("/deployment/application/policy/authorization/headers/failure/redirect") ||
        		   path.matches("/deployment/application/policy/authorization/headers/inconclusive/redirect")) {
        	
        	String value = atts.getValue("value");
        	curPolicyAuthZHeader = new Header("redirect", value);
        } else if (path.matches("/deployment/application/policy/authorization/headers/success/profile-att")) {
        	String name = atts.getValue("name");
        	String attribute = atts.getValue("attribute");
        	curAuthZProfileHeaders.put(name, attribute);
        }  else if (path.matches("/deployment/application/policy/authorization/headers/success/fixed-value")) {
        	String name = atts.getValue("name");
        	String value = atts.getValue("value");
        	List<String> vals = curAuthZFixedHeaders.get(name);
        	if (vals == null) {
        		vals = new ArrayList<String>();
        		curAuthZFixedHeaders.put(name, vals);
        	}
        	vals.add(value);
        }
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		Config cfg = (Config) parsingContextAccessor.get().get(PARSING_CONFIG);
		
		if (path.matches("/deployment/application")) {
			if (defAuthZRule != null) {
				authorizationRules.put(defAuthZRule.getName(), defAuthZRule);
			}
			
			// create default policy
			defPolicy.setUrl(cctx + "{/.../*,*}");
			String[] operations = {"HEAD","GET","POST","PUT","DELETE","TRACE","OPTIONS","CONNECT"}; 
			defPolicy.setOperations(Arrays.asList(operations));
			AuthNScheme authNScheme = new AuthNScheme(defAuthNScheme, defAuthNName);
	        defPolicy.setAuthNScheme(authNScheme);
	        defPolicy.setAuthZRuleString(defAuthZRuleName);
			
			TrafficManager trafficMgr = cfg.getTrafficManager();
            SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
            
            EndPoint ep = new AppEndPoint(cctxIncomingScheme, sm.getHost(), defPolicy.getUrl(), 
            		defPolicy.getQueryString(), cctxThost, cctxTport, cctxOutgoingScheme, 
            		cctxTsslPort, cctxPreserveHost, cctxHostHdr, cctxPolicyServiceGateway, 
            		cctxInjectScheme, cctxSchemeHeaderOvrd, sourceName, defPolicy.getUrl());
            
            sm.addMapping(ep);
            
            addPolicyToSiteMatcher(sm, defPolicy, path);

            if (ep instanceof AppEndPoint) {
            	AppEndPoint aep = (AppEndPoint) ep;
            	aep.setFixedHeaders(defAuthZFixedHeaders);
            	aep.setProfileHeaders(defAuthZProfileHeaders);
            }
		} else if (path.matches("/deployment/application/authorization/rule")) {
			authorizationRules.put(curAuthZRule.getName(), curAuthZRule);
		} else if (path.matches("/deployment/application/authorization/rule/allow/condition")) {
			if (buffer != null && curAuthZCondition.getValue() == null) {
				curAuthZCondition.setValue(buffer.toString());
			}
			curAuthZRule.setAllowCondition(curAuthZCondition);
		} else if (path.matches("/deployment/application/authorization/rule/deny/condition")) {
			if (buffer != null && curAuthZCondition.getValue() == null) {
				curAuthZCondition.setValue(buffer.toString());
			}
			curAuthZRule.setDenyCondition(curAuthZCondition);
		
		} else if (path.matches("/deployment/application/policy")) {
			TrafficManager trafficMgr = cfg.getTrafficManager();
            SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
            
            EndPoint ep = new AppEndPoint(cctxIncomingScheme, sm.getHost(), curPolicy.getUrl(), 
            		curPolicy.getQueryString(), cctxThost, cctxTport, cctxOutgoingScheme, 
            		cctxTsslPort, cctxPreserveHost, cctxHostHdr, cctxPolicyServiceGateway, 
            		cctxInjectScheme, cctxSchemeHeaderOvrd, sourceName, curPolicy.getName());
            sm.addMapping(ep);
            
            addPolicyToSiteMatcher(sm, curPolicy, path);
            
            if (ep instanceof AppEndPoint) {
            	AppEndPoint aep = (AppEndPoint) ep;
            	aep.setFixedHeaders(curAuthZFixedHeaders);
            	aep.setProfileHeaders(curAuthZProfileHeaders);
            }
		} else if (path.matches("/deployment/application/policy/url")) {
        	String url = buffer.toString();
        	String prefix = cctx.equals("/") ? "" : cctx;
        	if (!url.startsWith("/")) {
        		prefix += "/";
        	}
			curPolicy.setUrl(prefix + url);
        } else if (path.matches("/deployment/application/policy/query-string")) {
        	String queryString = buffer.toString();
			if (queryString != null && !queryString.isEmpty()) {
	        	curPolicy.setQueryString(queryString);
			}
        } else if (path.matches("/deployment/application/policy/operations")) {
        	String opsString = buffer.toString();
        	
        	List<String> operations = new ArrayList<String>();
        	for (String op : opsString.split(",")) {
        		operations.add(op);
        	}
        	
        	curPolicy.setOperations(operations);
        } else if (path.matches("/deployment/application/authorization/default")) {
            	
        } else if (path.matches("/deployment/application/authorization/default/headers/success/redirect")) {
          	defPolicy.addSuccessHeader(curPolicyAuthZHeader);
        } else if (path.matches("/deployment/application/authorization/default/headers/failure/redirect")) {
           	defPolicy.addFailureHeader(curPolicyAuthZHeader);
        } else if (path.matches("/deployment/application/authorization/default/headers/inconclusive/redirect")) {
           	defPolicy.addInconclusiveHeader(curPolicyAuthZHeader);
        } else if (path.matches("/deployment/application/policy/authorization")) {
        	
        } else if (path.matches("/deployment/application/policy/authorization/headers/success/redirect")) {
        	curPolicy.addSuccessHeader(curPolicyAuthZHeader);
        } else if (path.matches("/deployment/application/policy/authorization/headers/failure/redirect")) {
        	curPolicy.addFailureHeader(curPolicyAuthZHeader);
        } else if (path.matches("/deployment/application/policy/authorization/headers/inconclusive/redirect")) {
        	curPolicy.addInconclusiveHeader(curPolicyAuthZHeader);
        }
		
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
		String s = new String(ch, start, length);
		if (buffer != null) {
			buffer.append(s);
		}
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

    /**
     * Translate policy enforcement model to wamulator enforcement model.
     *
     * Summary:
     * If policy does not have an authentication element then the default defined in policy file at
     * /deployment/application/authentication is used for that policy. Currently, this is one of
     * 'anonymous' or 'login'.
     *
     * If a policy has the 'anonymous' authentication scheme then its the scheme, host, port, url, and
     * query string are added as a wamulator UnenforcedUri object.
     *
     *
     * @param sm
     * @param policy
     */
	private void addPolicyToSiteMatcher(SiteMatcher sm, Policy policy, Path path) {
		String[] actions = policy.getOperations().toArray(new String[] {});
        String policyUrl = policy.getUrl();
		
		if (policy.getAuthNScheme() == null) {
			LogUtils.warning(cLog, policy.getName() + " at " + path + " does not have an authentication scheme defined." +
					" Using the Default authentication scheme.");
			policy.setAuthNScheme(new AuthNScheme(defAuthNScheme, defAuthNName));
		}
		
		if ("anonymous".equals(policy.getAuthNScheme().getScheme())) {
        	UnenforcedUri ue = new UnenforcedUri(sm.getScheme(), sm.getHost(), sm.getPort(), policyUrl,
            		policy.getQueryString(), policyUrl, actions);
        	sm.addUnenforcedUri(ue);
        } else {
            String cond = policy.getAuthZRuleString();
        	
            // Make sure this rule exists
            AuthZRule ar = authorizationRules.get(cond);
        	if (ar == null && cond != null && (cond.contains("&") || cond.contains("|") || cond.contains("(") || cond.contains(")"))) {
                // indicate that combincatorial authorization expressions are not supported
        		LogUtils.warning(cLog, "The outcome of having combined Authorization Rules in the Authorization Expression at " + path +
        				" can have an unexpected outcome. For this reason it is not supported in the WAMulator " +
        				"at this time. The production WAM environment does support this functionality. Please fix: " + cond);
        	} else if (ar == null && !"Default".equals(policy.getName())) {
                // specified authorization rule not found and this is not the Default policy object so stop with error
        		throw new ConfigParserException("An authorization rule for name '" + cond + "' referenced in " + path + " was not found and must be defined.");
        	} else {
        		sm.addAllowTakesPrecedence(policyUrl, ar.allowTakesPrecedence());
        		Condition allowCondition = ar.getAllowCondition();
        		if (allowCondition != null) {
        			String allow = allowCondition.getValue();
        			if ("ldap".equals(allowCondition.getType())) {
        				int index = allow.indexOf("??sub?");
        				allow = allow.substring(index + 6, allow.length());
        			} else if ("role".equals(allowCondition.getType())) {
        				if ("Anyone".equals(allow)) {
        					allow = null;
        				}
        			}
        			AllowedUri au = new AllowedUri(sm.getScheme(), sm.getHost(), sm.getPort(), policyUrl,
        					policy.getQueryString(), actions, policyUrl);
        			
        			sm.addAllowedUri(au, cond, allow);
        		}
        		
        		Condition denyCondition = ar.getDenyCondition();
        		if (denyCondition != null) {
        			String deny = denyCondition.getValue();
        			if ("ldap".equals(denyCondition.getType())) {
        				int index = deny.indexOf("??sub?");
        				deny = deny.substring(index + 6, deny.length());
        			} else if ("role".equals(denyCondition.getType())) {
        				if ("Anyone".equals(deny)) {
        					deny = "*";
        				}
        			}
        			DeniedUri du = new DeniedUri(sm.getScheme(), sm.getHost(), sm.getPort(), policyUrl,
        					policy.getQueryString(), actions, policyUrl);
        			
        			sm.addDeniedUri(du, cond, deny);
        		}
        	}
        }
	}
}
