package org.lds.sso.appwrap;

import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.lds.sso.appwrap.proxy.RequestHandler;

public class User {

	protected String password = null;
	protected String username = null;
	private Map<String, String> headers = new TreeMap<String, String>();
	
	public User(String username, String pwd) {
		this.password = pwd;
		this.username = username;
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
}
