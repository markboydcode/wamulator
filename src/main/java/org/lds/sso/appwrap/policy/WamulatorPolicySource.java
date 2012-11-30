package org.lds.sso.appwrap.policy;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.lds.sso.appwrap.AllowedUri;
import org.lds.sso.appwrap.AppEndPoint;
import org.lds.sso.appwrap.AppEndPoint.InboundScheme;
import org.lds.sso.appwrap.AppEndPoint.OutboundScheme;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.DeniedUri;
import org.lds.sso.appwrap.EndPoint;
import org.lds.sso.appwrap.SiteMatcher;
import org.lds.sso.appwrap.TrafficManager;
import org.lds.sso.appwrap.UnenforcedUri;
import org.lds.sso.appwrap.XmlConfigLoader2.Path;
import org.lds.sso.appwrap.identity.ExternalUserSource.ConfigurationException;
import org.lds.sso.appwrap.io.SimpleErrorHandler;
import org.lds.sso.appwrap.model.AuthNScheme;
import org.lds.sso.appwrap.model.AuthZRule;
import org.lds.sso.appwrap.model.Condition;
import org.lds.sso.appwrap.model.Header;
import org.lds.sso.appwrap.model.Policy;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class WamulatorPolicySource implements ContentHandler {
	private static final Logger cLog = Logger.getLogger(WamulatorPolicySource.class.getName());
	private ThreadLocal<Map<String,Object>> parsingContextAccessor;
	private String cctxCctx, cctxThost, cctxCscheme, cctxTscheme, cctxHostHdr;
    String cctxPolicyServiceGateway, cctxSchemeHeaderOvrd, cctxInjectSchemeHeader;
    boolean cctxPreserveHost, cctxInjectScheme;
    OutboundScheme cctxOutgoingScheme;
    InboundScheme cctxIncomingScheme;
    int cctxTsslPort, cctxTport;
	
	
	public static final String PARSING_CONFIG = "config-instance";
	
	private Path path = new Path();
	private StringBuffer buffer;
	private Attributes curAtts;
	
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
	private AuthZRule curPolicyAuthZRule;
	private Header curPolicyAuthZHeader;
	
	private String defAuthNScheme;
	private String defAuthNName;
	
	
	public WamulatorPolicySource(ThreadLocal<Map<String,Object>> parsingContextAccessor) {
		this.parsingContextAccessor = parsingContextAccessor;
	}
	
	public WamulatorPolicySource(ThreadLocal<Map<String,Object>> parsingContextAccessor, String cctxCctx, 
			String cctxThost, String cctxCscheme, String cctxTscheme, String cctxHostHdr,
			String cctxPolicyServiceGateway, String cctxSchemeHeaderOvrd, String cctxInjectSchemeHeader,
			boolean cctxPreserveHost, boolean cctxInjectScheme, OutboundScheme cctxOutgoingScheme, 
			InboundScheme cctxIncomingScheme, int cctxTsslPort, int cctxTport) {
		this.parsingContextAccessor = parsingContextAccessor;
		this.cctxCctx = cctxCctx;
		this.cctxThost = cctxThost;
		this.cctxCscheme = cctxCscheme;
		this.cctxTscheme = cctxTscheme;
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
	}
	
	public void setConfig(Path path, Properties props) throws ConfigurationException {
		String xml = props.getProperty("xml");
        
        if (StringUtils.isEmpty(xml)) {
        	cLog.log(Level.WARNING, "No Configuration for xml policy-source at " 
        			+ WamulatorPolicySource.class.getSimpleName()
        			+ " at " + path
        			+ ". No Policies will be available from this source.");
        	return;
        }
        parseXml(path, xml);
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
        	throw new ConfigurationException("Unable to load policies for policy-source at " + path, e);
        }
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		Config cfg = (Config) parsingContextAccessor.get().get(PARSING_CONFIG);
		path.add(qName);
        curAtts = atts;
        
        if (path.matches("/deployment/application")) {
    		String cctx = atts.getValue("cctx");
    		if (!cctx.equals(cctxCctx)) {
    			throw new IllegalArgumentException(
    					"ERROR: The cctx attribute on the cctx-mapping element in the config file " + 
    					"(" + cctxCctx + ") " + "must match the cctx attribute on the application " + 
    					"element in the Policy Exposee export config file (" + cctx + ").");
    		}
    	} else if (path.matches("/deployment/application/authentication")) { 
    		defAuthNScheme = atts.getValue("scheme");
    		defAuthNName = atts.getValue("name");
		} else if (path.matches("/deployment/application/authorization/default")) {
        	defAuthZRuleName = atts.getValue("value");
        } else if (path.matches("/deployment/application/authorization/rule")) {
        	String name = atts.getValue("name");
        	String enabled = atts.getValue("enabled");
        	String precedence = atts.getValue("allow-takes-precedence");
        	
        	if (name.equals(defAuthZRuleName)) {
        		defAuthZRule = new AuthZRule("~~DefaultRule~~", Boolean.parseBoolean(enabled), Boolean.parseBoolean(precedence));
        	}
        	
        	if (!name.equals("~~default-headers~~")) {
        		curAuthZRule = new AuthZRule(name, Boolean.parseBoolean(enabled), Boolean.parseBoolean(precedence));
        	} else {
        		defAuthZProfileHeaders = new HashMap<String,String>();
            	defAuthZFixedHeaders = new HashMap<String,List<String>>();
        	}
        } else if (path.matches("/deployment/application/authorization/rule/allow/condition") ||
        		   path.matches("/deployment/application/authorization/rule/deny/condition")) {
        	
        	String type = atts.getValue("type");
        	String value = atts.getValue("value");
        	if (value == null || value.isEmpty()) {
        		buffer = new StringBuffer();
        	}
        	curAuthZCondition = new Condition(type, value);
        } else if (path.matches("/deployment/application/authorization/rule/headers/success/profile-att")) {
        	String name = atts.getValue("name");
        	String attribute = atts.getValue("attribute");
        	defAuthZProfileHeaders.put(name, attribute);
        } else if (path.matches("/deployment/application/authorization/rule/headers/success/fixed-value")) {
        	String name = atts.getValue("name");
        	String value = atts.getValue("value");
        	List<String> vals = curAuthZFixedHeaders.get(name);
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
        	AuthZRule rule = authorizationRules.get(name);
        	if (rule == null) {
        		System.out.println("The authorization rule needs to be defined before it is used: " + name);
        	} else {
        		curPolicyAuthZRule = rule.clone();
        	}
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
			Policy defPolicy = new Policy("Default");
			defPolicy.setUrl(cctxCctx + "{/.../*,*}");
			String[] operations = {"HEAD","GET","POST","PUT","DELETE","TRACE","OPTIONS","CONNECT"}; 
			defPolicy.setOperations(Arrays.asList(operations));
			AuthNScheme authNScheme = new AuthNScheme(defAuthNScheme, defAuthNName);
	        defPolicy.setAuthNScheme(authNScheme);
			
			TrafficManager trafficMgr = cfg.getTrafficManager();
            SiteMatcher sm = (SiteMatcher) trafficMgr.getLastMatcherAdded();
            
            EndPoint ep = new AppEndPoint(cctxIncomingScheme, sm.getHost(), defPolicy.getUrl(), 
            		defPolicy.getQueryString(), cctxThost, cctxTport, cctxOutgoingScheme, 
            		cctxTsslPort, cctxPreserveHost, cctxHostHdr, cctxPolicyServiceGateway, 
            		cctxInjectScheme, cctxSchemeHeaderOvrd);
            
            sm.addMapping(ep);
            
            addPolicyToSiteMatcher(sm, defPolicy.getUrl(), defPolicy);
            
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
            		cctxInjectScheme, cctxSchemeHeaderOvrd);
            sm.addMapping(ep);
            
            addPolicyToSiteMatcher(sm, curPolicy.getUrl(), curPolicy);
            
            if (ep instanceof AppEndPoint) {
            	AppEndPoint aep = (AppEndPoint) ep;
            	aep.setFixedHeaders(curAuthZFixedHeaders);
            	aep.setProfileHeaders(curAuthZProfileHeaders);
            }
		} else if (path.matches("/deployment/application/policy/url")) {
        	String url = buffer.toString();
			curPolicy.setUrl(cctxCctx + "/" + url);
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
        } else if (path.matches("/deployment/application/policy/authorization")) {
        	curPolicy.setAuthZRule(curPolicyAuthZRule);
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
	
	private void addPolicyToSiteMatcher(SiteMatcher sm, String url, Policy policy) {
		String[] actions = policy.getOperations().toArray(new String[] {});
		
		if ("anonymous".equals(policy.getAuthNScheme().getScheme())) {
        	UnenforcedUri ue = new UnenforcedUri(sm.getScheme(), sm.getHost(), sm.getPort(), url, 
            		policy.getQueryString(), url);
        	sm.addUnenforcedUri(ue);
        } else {
            String cond = policy.getAuthZRule() != null ? policy.getAuthZRule().getName() : null;
        	AuthZRule ar = authorizationRules.get(cond);
        	if (ar != null) {
        		sm.addAllowTakesPrecedence(url, ar.allowTakesPrecedence());
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
        			AllowedUri au = new AllowedUri(sm.getScheme(), sm.getHost(), sm.getPort(), url, 
        					policy.getQueryString(), actions, url);
        			
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
        			DeniedUri du = new DeniedUri(sm.getScheme(), sm.getHost(), sm.getPort(), url, 
        					policy.getQueryString(), actions, url);
        			
        			sm.addDeniedUri(du, cond, deny);
        		}
        	}
        }
	}
}
