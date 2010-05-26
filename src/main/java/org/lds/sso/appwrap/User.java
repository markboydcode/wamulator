package org.lds.sso.appwrap;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.lds.sso.appwrap.proxy.Header;
import org.lds.sso.appwrap.proxy.HeaderBuffer;
import org.lds.sso.appwrap.proxy.RequestHandler;

public class User {

	private static Map<String,String> defaultHeaders = new HashMap<String,String>();
	private static final Map<String,String> defaultTokenProps;
	
	protected String password = null;
	protected String username = null;
	private Map<String, String> headers = new TreeMap<String, String>(defaultHeaders);
	Principal principal = null;

	/**
	 * Sets up default header and sso session (token) values that will always
	 * be made available to application if no values are had by the user in lds
	 * systems.
	 */
	static {
		Map<String,String> hdr = new HashMap<String,String>(); 
		Map<String,String> tks = new HashMap<String,String>();
	
		hdr.put(UserHeaderNames.UNITS, UserHeaderNames.EMPTY_VALUE_INDICATOR);
		tks.put(UserHeaderNames.UNITS, UserHeaderNames.EMPTY_VALUE_INDICATOR);
		
		hdr.put(UserHeaderNames.POSITIONS, UserHeaderNames.EMPTY_VALUE_INDICATOR);
		tks.put(UserHeaderNames.POSITIONS, UserHeaderNames.EMPTY_VALUE_INDICATOR);
		
		hdr.put(UserHeaderNames.LDS_ACCOUNT_ID, UserHeaderNames.EMPTY_VALUE_INDICATOR);
		tks.put(UserHeaderNames.LDS_ACCOUNT_ID, UserHeaderNames.EMPTY_VALUE_INDICATOR);
		
		hdr.put(UserHeaderNames.DN, UserHeaderNames.EMPTY_VALUE_INDICATOR);
		tks.put(UserHeaderNames.DN, UserHeaderNames.EMPTY_VALUE_INDICATOR);
		
		hdr.put(UserHeaderNames.EMAIL, UserHeaderNames.EMPTY_VALUE_INDICATOR);
		tks.put(UserHeaderNames.EMAIL, UserHeaderNames.EMPTY_VALUE_INDICATOR);
		
		hdr.put(UserHeaderNames.LDS_MRN, UserHeaderNames.EMPTY_VALUE_INDICATOR);
		tks.put(UserHeaderNames.LDS_MRN, UserHeaderNames.EMPTY_VALUE_INDICATOR);
		
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

	public void addHeader(String name, String value) {
	    System.out.println(">>> for user '" + username + "' adding hdr '" + name + "' with value '" + value + "'");
		this.headers.remove(name);
		this.headers.put(name, value);
	}

	public void injectUserHeaders(HeaderBuffer headersBfr) {
		for(Entry<String, String> e : headers.entrySet()) {
			headersBfr.append(new Header(e.getKey(), e.getValue()));
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

	public String getProperty(String name) {
		return headers.get(name);
	}

//	public void setProperty(String name, String value) {
//		headers.put(name, value);
//	}
}
