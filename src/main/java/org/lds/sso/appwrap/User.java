package org.lds.sso.appwrap;

import java.net.InetAddress;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.lds.sso.appwrap.opensso.AssignmentsChainInjector;
import org.lds.sso.appwrap.opensso.LegacyPropsInjectorDefs;
import org.lds.sso.appwrap.opensso.UnitsChainInjector;
import org.lds.sso.appwrap.proxy.RequestHandler;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenListener;

public class User implements SSOToken {

	private static final Map<String,String> defaultHeaders;
	private static final Map<String,String> defaultTokenProps;
	
	private static final UnitsChainInjector unitsChainInjector;
	private static final AssignmentsChainInjector assignmentsInjector;
	
	protected String password = null;
	protected String username = null;
	private Map<String, String> headers = new TreeMap<String, String>(defaultHeaders);
	private Map<String, String> tokenProps = new TreeMap<String, String>(defaultTokenProps);
	Principal principal = null;

	/**
	 * Sets up default header and sso session (token) values that will always
	 * be made available to application if no values are had by the user in lds
	 * systems.
	 */
	static {
		Map<String,String> hdr = new HashMap<String,String>(); 
		Map<String,String> tks = new HashMap<String,String>();
	
		hdr.put(LegacyPropsInjectorDefs.CP_UNITS_SESSION_PROPERTY, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		tks.put(LegacyPropsInjectorDefs.CP_UNITS_SESSION_PROPERTY, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		tks.put(LegacyPropsInjectorDefs.CP_UNITS_SESSION_PROPERTY_FOR_POLICY_EVAL, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		unitsChainInjector = new UnitsChainInjector();
		
		hdr.put(LegacyPropsInjectorDefs.CP_STATUS_PROPERTY, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		tks.put(LegacyPropsInjectorDefs.CP_STATUS_PROPERTY, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		
		hdr.put(LegacyPropsInjectorDefs.CP_POSITIONS_SESSION_PROPERTY, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		tks.put(LegacyPropsInjectorDefs.CP_POSITIONS_SESSION_PROPERTY, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		tks.put(LegacyPropsInjectorDefs.CACHE_TYPE, LegacyPropsInjectorDefs.NONE);
		assignmentsInjector = new AssignmentsChainInjector();
		
		hdr.put(LegacyPropsInjectorDefs.CP_LDS_ACCOUNT_ID_PROPERTY, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		tks.put(LegacyPropsInjectorDefs.CP_LDS_ACCOUNT_ID_PROPERTY, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		
		hdr.put(LegacyPropsInjectorDefs.CP_DN, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		tks.put(LegacyPropsInjectorDefs.CP_DN, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		
		hdr.put(LegacyPropsInjectorDefs.CP_EMAIL, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		tks.put(LegacyPropsInjectorDefs.CP_EMAIL, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		
		hdr.put(LegacyPropsInjectorDefs.CP_LDS_MRN, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		tks.put(LegacyPropsInjectorDefs.CP_LDS_MRN, LegacyPropsInjectorDefs.EMPTY_VALUE_INDICATOR);
		
		defaultHeaders = hdr;
		defaultTokenProps = tks;
	}
	
	public User(String username, String pwd) {
		this.password = pwd;
		this.username = username;
		this.principal = new Principal() {
			
			private String name = "sso.appwrap.user." + User.this.username;
			
			public String getName() {
				return this.name;
			}
		};
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	public void addHeader(String name, String value) throws SSOException {
		if (LegacyPropsInjectorDefs.CP_POSITIONS_SESSION_PROPERTY.equals(name)) {
			assignmentsInjector.clearOld(name, tokenProps);
			assignmentsInjector.inject(name, value, this);
		}
		else if (LegacyPropsInjectorDefs.CP_UNITS_SESSION_PROPERTY.equals(name)) {
			unitsChainInjector.clearOld(name, tokenProps);
			unitsChainInjector.inject(name, value, this);
		}
		else {
			// custom syntax uses headers as is so inject into both the headers
			// buffer (for injecting into requests) and the tokenProps buffer
			// (for condition syntax evaluation).
			this.tokenProps.remove(name);
			this.tokenProps.put(name, value);
		}
		this.headers.remove(name);
		this.headers.put(name, value);
	}

	public void injectUserHeaders(StringBuffer headersBfr) {
		for(Entry<String, String> e : headers.entrySet()) {
			headersBfr.append(e.getKey())
				.append(": ").append(e.getValue()).append(RequestHandler.CRLF);
		}
	}

	public NvPair[] getHeaders() {
		NvPair[] hdr = null;
		
		if (headers.size() > 0) {
			hdr = new NvPair[headers.size()];
			int idx = 0;
			for(Entry<String, String> e : headers.entrySet()) {
				hdr[idx++] = new NvPair(e.getKey(), e.getValue());
			}
			
		}
		return hdr;
	}

	////////////// SSOToken Interface methods
	
	public void addSSOTokenListener(SSOTokenListener listener) throws SSOException {
		throw new UnsupportedOperationException();
	}

	public String encodeURL(String url) throws SSOException {
		throw new UnsupportedOperationException();
	}

	public int getAuthLevel() throws SSOException {
		throw new UnsupportedOperationException();
	}

	public String getAuthType() throws SSOException {
		throw new UnsupportedOperationException();
	}

	public String getHostName() throws SSOException {
		throw new UnsupportedOperationException();
	}

	public InetAddress getIPAddress() throws SSOException {
		throw new UnsupportedOperationException();
	}

	public long getIdleTime() throws SSOException {
		throw new UnsupportedOperationException();
	}

	public long getMaxIdleTime() throws SSOException {
		throw new UnsupportedOperationException();
	}

	public long getMaxSessionTime() throws SSOException {
		throw new UnsupportedOperationException();
	}

	public Principal getPrincipal() throws SSOException {
		// TODO Auto-generated method stub
		return principal; 
	}

	public String getProperty(String name) throws SSOException {
		return tokenProps.get(name);
	}

	public long getTimeLeft() throws SSOException {
		throw new UnsupportedOperationException();
	}

	public SSOTokenID getTokenID() {
		throw new UnsupportedOperationException();
	}

	public void setProperty(String name, String value) throws SSOException {
		tokenProps.put(name, value);
	}
}
