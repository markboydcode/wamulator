/**
 * 
 */
package org.lds.sso.appwrap;

public class UnenforcedUri implements Comparable<UnenforcedUri>{
	protected boolean queryIsRequired = false;
	protected boolean usePathPrefixMatching = false;
	protected String pathPrefix = null;
	protected String pathMatch = null;
	protected boolean useQueryPrefixMatching = false;
	protected String queryPrefix = null;
	protected String queryMatch = null;
	protected String host;
	protected int port;
	protected String id;
	private String scheme;
	
	public UnenforcedUri(String scheme, String host, int port, String path, String query) {
		this.host = host;
		this.port = port;
		this.scheme = scheme;
		
		pathMatch = path;
		id = scheme + "://" + host + ":" + port + path + (query == null ? "" : "?" + query);
		
		if (path.startsWith("*")) {
			pathPrefix = "";
			usePathPrefixMatching = true;
		}
		else if(path.endsWith("*")) {
			pathPrefix = path.substring(0, path.length()-2);
			usePathPrefixMatching = true;
		}
		
		if (query != null) {
			queryIsRequired = true;
			queryMatch = query;
			
			if (query.startsWith("*")) {
				queryPrefix = "";
				useQueryPrefixMatching = true;
			}
			else if (query.endsWith("*")) {
				queryPrefix = query.substring(0, query.length()-2);
				useQueryPrefixMatching = true;
			}
		}
	}
	
	public boolean matches(String scheme, String host, int port, String path, String query) {
		if (! this.scheme.equals(scheme) || ! this.host.equals(host) || this.port != port) {
			return false;
		}
		
		if (queryIsRequired) {
			if (query == null) {
				return false;
			}
			if (useQueryPrefixMatching) {
				if (! query.startsWith(queryPrefix)) {
					return false;
				}
			}
			else {
				if (! query.equals(queryMatch)) {
					return false;
				}
			}
		}
		else if (query != null) {
			return false; 
		}
		
		if (usePathPrefixMatching) {
			if (path.startsWith(pathPrefix)) {
				return true;
			}
		}
		else {
			if (path.equals(pathMatch)) {
				return true;
			}
		}
		return false;
	}

	public String toString() {
		return id;
	}

	public int compareTo(UnenforcedUri o) {
		return id.compareTo(o.id);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof UnenforcedUri)) {
			return false;
		}
		UnenforcedUri uu = (UnenforcedUri) obj;
		return id.equals(uu.id);
	}


	@Override
	public int hashCode() {
		return id.hashCode();
	}
}