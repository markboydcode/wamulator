package org.lds.sso.appwrap;

/**
 * Created by IntelliJ IDEA.
 * User: FoxleyTD
 * Date: 9/13/13
 * Time: 4:54 PM
 */
public class UnenforcedEndPoint implements EndPoint {
    private String originalName;
    
    private String contextRoot;
    private String thost;
    private int tport;

    private String id;
    private String queryString;

    /**
     * A number that indicates the order of declaration in the configuration file
     * in xml document order within a by-site element for use in comparable 
     * operations allowing endpoint with hierarchically nested canonical urls
     * to coexist but allow for the nested one to be declared first and match
     * without being lost by the endpoint with the containing URL.
     */
    private Integer declarationOrder = null;


    public UnenforcedEndPoint(String originalName, String contextRoot, String thost, int tport) {
        this.contextRoot = contextRoot;
        this.thost = thost;
        this.tport = tport;
        this.originalName = originalName;
    }
    
    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public String getThost() {
        return thost;
    }

    public void setThost(String thost) {
        this.thost = thost;
    }

    public int getTport() {
        return tport;
    }

    public void setTport(int tport) {
        this.tport = tport;
    }

    public Integer getDeclarationOrder() {
        return declarationOrder;
    }

    public void setDeclarationOrder(int declarationOrder) {
        this.declarationOrder = declarationOrder;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public int compareTo(EndPoint o) {
        return declarationOrder.compareTo(o.getDeclarationOrder());
    }
    
    public String getOriginalName() {
        return "[cctx-unenforced] cctx=" + originalName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UnenforcedEndPoint)) {
            return false;
        }
        UnenforcedEndPoint ep = (UnenforcedEndPoint) obj;
        return id.equals(ep.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
