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

	protected Set<AllowedUri> allowedUrls = new TreeSet<AllowedUri>();
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
	public void addAllowedUri(AllowedUri uri) {
		this.allowedUrls.add(uri);
	}
	
	public void removePermissions() {
		allowedUrls.clear();
	}

	/**
	 * Determines if a user is allowed the action on the resource indicated by
	 * host, port, and path.
	 * 
	 * @param host
	 * @param port
	 * @param action
	 * @param uri
	 * @return
	 */
	public boolean isPermitted(String host, int port, String action, String uri) {
		for(AllowedUri au : allowedUrls) {
			if (au.matches(host, port, uri) && au.allowed(action)) {
				return true;
			}
		}
		return false;
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
	
	public Set<NvPair> getPermissions() {
		Set<NvPair> prms = null;
		
		if (allowedUrls.size() > 0) {
			prms = new TreeSet<NvPair>();
			for(AllowedUri au : allowedUrls) {
				for (String action : au.actions) {
					prms.add(new NvPair(au.uriMatch, action));
				}
			}
		}
		return prms;
	}
}
