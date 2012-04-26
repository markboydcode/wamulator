package org.lds.sso.appwrap;

import java.util.logging.Logger;

import org.lds.sso.appwrap.AppEndPoint.InboundScheme;
import org.lds.sso.appwrap.AppEndPoint.Scheme;

public class OrderedUri implements Comparable<OrderedUri>{
	private static Logger logger = Logger.getLogger(OrderedUri.class.getName());

    private Integer declarationOrder = 0;
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
        
        if (path.startsWith("*")) {
            pathPrefix = "";
            usePathPrefixMatching = true;
        }
        else if(path.endsWith("*")) {
            pathPrefix = path.substring(0, path.length()-1);
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
    
    public boolean matches(Scheme scheme, String host, int port, String path, String query) {
		logger.fine("@@ Testing cpath=" + pathMatch + ", scheme=" + this.scheme + ", host=" + this.host +
					", port=" + this.port + ", query=" + queryMatch + ", queryIsRequired=" + queryIsRequired +
					", queryPrefix=" + queryPrefix + ", useQueryPrefixMatching=" + useQueryPrefixMatching);
		logger.fine("@@ For a match with requested path=" + path + ", scheme=" + scheme + ", host=" + host +
					", port=" + port + ", query=" + query);
		boolean match = _matches(scheme, host, port, path, query);
		String message = match ? "SUCCESS: The cpath: " + pathMatch + " matches the requested path: " + path :
						 		 "FAILED: The cpath: " + pathMatch + " doesn't match the requested path: " + path;
		logger.fine(message);
		return match;
    }

	public boolean _matches(Scheme scheme, String host, int port, String path, String query) {
		if ((this.host != null && ! this.host.equals(host))) {
			return false;
		}
		// return false if configured scheme is not BOTH and incoming scheme 
		// differs from configured or they are the same and the port from the
		// host header does not match
		if (this.scheme != InboundScheme.BOTH 
				&& ! this.scheme.moniker.equals(scheme.moniker) 
					|| (this.scheme.moniker.equals(scheme.moniker) && this.port != port)) {
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
			if (path.toLowerCase().startsWith(pathPrefix.toLowerCase())) {
				return true;
			}
		}
		else {
			if (path.equalsIgnoreCase(pathMatch)) {
				return true;
			}
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
