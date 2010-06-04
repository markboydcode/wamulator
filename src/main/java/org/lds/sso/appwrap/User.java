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
	
	protected String password = null;
	protected String username = null;
	private Map<String, String> headers = new TreeMap<String, String>(UserHeaderNames.defaultHeaders);
	Principal principal = null;

	public User(String username, String pwd) {
		this.password = pwd;
		this.username = username;
        this.headers.put(UserHeaderNames.CN, username);
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
		this.headers.put(UserHeaderNames.CN, username);
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
}
