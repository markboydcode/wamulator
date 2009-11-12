/**
 * 
 */
package org.lds.sso.appwrap;

/**
 * Class for holding name and value pairs and exposing them with javabean's
 * getters and setters.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class NvPair implements Comparable<NvPair>{
	public NvPair(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	private String name;
	private String value;
	
	public String getName() {
		return name;
	}
	public String getValue() {
		return value;
	}
	public String toString() {
		return name + ": " + value;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public int compareTo(NvPair nvp) {
		return this.name.compareTo(nvp.name);
	}
}