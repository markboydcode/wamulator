package org.lds.sso.appwrap;

import org.lds.sso.appwrap.AppEndPoint.InboundScheme;
import org.lds.sso.appwrap.AppEndPoint.Scheme;

import java.util.logging.Logger;

/**
 * Represents URIs that sort according to the document order in the XML configuration.
 */
public class OrderedUri implements Comparable<OrderedUri>{
	private static Logger logger = Logger.getLogger(OrderedUri.class.getName());

    private Integer declarationOrder = 0;
    protected String pathMatch = null;
    protected String queryMatch = null;
    protected String host;
    protected int port;
    protected String id;
    private InboundScheme scheme = InboundScheme.HTTP;
    private String cpathDeclaration;
    
    public OrderedUri(InboundScheme scheme, String host, int port, String path, String query, String cpathDeclaration) {
        this.cpathDeclaration = cpathDeclaration;
        this.host = host;
        this.port = port;
        if (scheme == null) {
        	scheme = InboundScheme.HTTP; // default
        }
        this.scheme = scheme;
        
        pathMatch = path;
        queryMatch = query;

        updateId();
    }
    
    /**
     * Returns the original cpath attribute value used when declaring the 
     * management of this URI pattern.
     *  
     * @return
     */
    public String getCpathDeclaration() {
        return this.cpathDeclaration;
    }
    
    /**
     * Updates the id of this object which changes its hashcode, equals, and
     * toString behaviors.
     */
    protected void updateId() {
        id = scheme + "://" + host + ":" + port + pathMatch + (queryMatch == null ? "" : "?" + queryMatch);
    }
    
    /**
     * Allows port to be updated only if originally set to zero which only occurs
     * when "auto" is specified for the proxy port allowing any port to be used
     * for the proxy. Returns true if the port for this object changed as a 
     * result of this call; false otherwise.
     * 
     * @param port
     */
    public boolean proxyPortChanged(int port) {
        if (this.port == 0) {
            this.port = port;
            updateId();
            return true;
        }
        return false;
    }
    
    public boolean matches(String action, Scheme scheme, String host, int port, String path, String query) {
		logger.fine("@@ Testing cpath=" + pathMatch + ", scheme=" + this.scheme + ", host=" + this.host +
					", port=" + this.port + ", query=" + queryMatch);
		logger.fine("@@ For a match with requested path=" + path + ", scheme=" + scheme + ", host=" + host +
					", port=" + port + ", query=" + query);
		boolean match = _matches(action, scheme, host, port, path, query);
		String message = match ? "SUCCESS: The cpath: " + pathMatch + " matches the requested path: " + path :
						 		 "FAILED: The cpath: " + pathMatch + " doesn't match the requested path: " + path;
		logger.fine(message);
		return match;
    }

	public boolean _matches(String action, Scheme scheme, String host, int port, String path, String query) {
        if (this instanceof AllowedUri) { // also works for unenforcedUri since it is a subclass
            if (!((AllowedUri)this).allowed(action)) { // if method in allowed then we have a match on method, else not
                return false;
            }
        }
        if (this instanceof DeniedUri) {
            if (((DeniedUri)this).denied(action)) { // if method denied then we have a match on method, else false
                return false;
            }
        }
		if ((this.host != null && ! this.host.equals(host))) {
			return false;
		}
		
		// return false if configured scheme is not BOTH and incoming scheme 
		// differs from configured or they are the same and the port from the
		// host header does not match
		if (this.scheme != InboundScheme.BOTH && ! this.scheme.moniker.equals(scheme.moniker) || 
				(this.scheme.moniker.equals(scheme.moniker) && this.port != port)) {
			return false;
		}

		boolean queryMatches = false;
		if (query != null && queryMatch != null && UriMatcher.matches(query, queryMatch)) {
			queryMatches = true;
		}

		boolean pathMatches = false;
		if (path != null && pathMatch != null && UriMatcher.matches(path, pathMatch)) {
			pathMatches = true;
		}
		
		if (queryMatch != null && pathMatch != null) {
			return queryMatches && pathMatches;
		} else if (pathMatch != null) {
			return pathMatches;
		} else if (queryMatch != null) {
			return queryMatches;
		}
		
		return false;
	}

    public String toString() {
        return id;
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

    
    public void setDeclarationOrder(int order) {
        this.declarationOrder = new Integer(order);
    }

    public int compareTo(OrderedUri m) {
        return declarationOrder.compareTo(m.declarationOrder);
    }
}
