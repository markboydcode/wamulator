/**
 * 
 */
package org.lds.sso.appwrap;

/**
 * Stores a permitted URI or URI pattern is a terminating wildcard is included
 * and includes the allowed action on that URI or URIs that match the pattern.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class PermittedUri implements Comparable<PermittedUri>{
	public boolean usePrefixMatching = false;
	public String uriPrefix = null;
	public String uriMatch = null;
	public String action = null;
	public String id = null;
	
	public PermittedUri(String action, String uri) {
		this.action = action;
		this.uriMatch = uri;
		id = action + ":" + uriMatch;
		
		if (uri.endsWith("*")) {
			uriPrefix = uri.substring(0, uri.length()-2);
			usePrefixMatching = true;
		}
	}
	
	public String toString() {
		return id;
	}
	
	public int compareTo(PermittedUri o) {
		return id.compareTo(o.id);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PermittedUri)) {
			return false;
		}
		PermittedUri p = (PermittedUri) obj;
		// TODO Auto-generated method stub
		return id.equals(p.id);
	}


	@Override
	public int hashCode() {
		return id.hashCode();
	}
}