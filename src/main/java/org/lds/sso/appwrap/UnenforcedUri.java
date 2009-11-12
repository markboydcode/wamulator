/**
 * 
 */
package org.lds.sso.appwrap;

public class UnenforcedUri implements Comparable<UnenforcedUri>{
	protected boolean usePrefixMatching = false;
	protected String uriPrefix = null;
	protected String uriMatch = null;
	protected String host;
	protected int port;
	protected String id;
	
	public UnenforcedUri(String host, int port, String path) {
		this.host = host;
		this.port = port;
		uriMatch = path;
		id = host + ":" + port + path;
		
		if (path.endsWith("*")) {
			uriPrefix = path.substring(0, path.length()-2);
			usePrefixMatching = true;
		}
	}
	
	public boolean matches(String host, int port, String path) {
		if (! host.equals(this.host) || this.port != port)
		{
			return false;
		}
		if (usePrefixMatching) {
			if (path.startsWith(uriPrefix)) {
				return true;
			}
		}
		else {
			if (path.equals(uriMatch)) {
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