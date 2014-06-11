package org.lds.sso.appwrap;

/**
 * Endpoint interface that any endpoint for handling a request must implement. There are currently three implementors
 * of this interface: {@link org.lds.sso.appwrap.AppEndPoint} , {@link org.lds.sso.appwrap.LocalFileEndPoint}, and
 * {@link org.lds.sso.appwrap.UnenforcedEndPoint}. {@link org.lds.sso.appwrap.LocalFileEndPoint} is for serving file
 * content directly from the wamulator. The other two are for traffic to be proxied to a back end host/port with the
 * former only allowing proxying if the user meets specified conditions while the latter proxies unconditionally.
 *
 * @author boydmr
 *
 */
public interface EndPoint extends Comparable<EndPoint> {
	public int compareTo(EndPoint o);
	public String getId();
	public String getContextRoot();
	public String getQueryString();
    public Integer getDeclarationOrder();
    public void setDeclarationOrder(int index);
    public String getOriginalName();
}
