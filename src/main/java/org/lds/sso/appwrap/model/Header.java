package org.lds.sso.appwrap.model;

public class Header {

	private String type;
	private String name;
	private String value;
	
	
	public Header(String name, String type, String value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}
	
	public Header(String type, String value) {
		this.type = type;
		this.value = value;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
}
