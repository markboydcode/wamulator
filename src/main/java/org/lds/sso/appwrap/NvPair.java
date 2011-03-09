/**
 * 
 */
package org.lds.sso.appwrap;

/**
 * Class for holding name and value pairs and exposing them with javabean's
 * getters and setters. Natural ordering for this class is sorting by name first
 * and if equal then by value. A pair with a null value for the name will 
 * sort before a pair with a non-null name. Within a null name value the same
 * sorting will then apply to values. Null values sort prior to non-null
 * values. Two instances with identical name and value pairs
 * will themselves be equal and have the same hashcode.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class NvPair implements Comparable<NvPair>{
	public NvPair(String name, String value) {
		this.name = name;
		this.value = value;
		updateId();
	}
	
	private void updateId() {
	    /*
	     * 2* below makes nvpair(null,"a").equals(nvpair("a",null)) false
	     */
        id = (2*(name == null ? "".hashCode() : name.hashCode()))
            + (value == null ? "".hashCode() : value.hashCode());
    }

    private String name;
	private String value;
	private int id;
	
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
		updateId();
	}
	public void setValue(String value) {
		this.value = value;
        updateId();
	}
	
	/**
	 * Compares pairs by name first, value second. In both comparisons a null 
	 * value is considered lexicographically less than any other non-null string.
	 */
	public int compareTo(NvPair nvp) {
	    int answer = 0;
	    
	    if (name == null) {
	        if (nvp.name == null) {
	            answer = 0;
	        }
	        else {
	            answer = -1;
	        }
	    }
	    else {
	        if (nvp.name == null) {
	            answer = +1;
	        }
	        else {
	            answer = this.name.compareTo(nvp.name);    
	        }
	    }
	    
	    if (answer == 0) {
	        if (value == null) {
	            if (nvp.value == null) {
	                answer = 0;
	            }
	            else {
	                answer = -1;
	            }
	        }
	        else {
	            if (nvp.value == null) {
	                answer = +1;
	            }
	            else {
	                answer = this.value.compareTo(nvp.value);    
	            }
	        }
	    }
		return answer;
	}
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof NvPair)) {
            return false;
        }
        NvPair n = (NvPair) obj;
        return this.hashCode() == n.hashCode();
    }
    @Override
    public int hashCode() {
        return id;
    }
}