package org.lds.sso.appwrap.model;

public class AuthNScheme {

	private String scheme;
	private String name;
	
	
	public AuthNScheme(String scheme, String name) {
		this.scheme = scheme;
		this.name = name;
	}
	
	public String getScheme() {
		return scheme;
	}
	
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
