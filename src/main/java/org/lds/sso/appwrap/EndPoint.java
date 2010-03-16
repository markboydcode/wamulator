package org.lds.sso.appwrap;

/**
 * Endpoint interface that any endpoint for handling a request must implement.
 * 
 * @author boydmr
 *
 */
public interface EndPoint extends Comparable<EndPoint> {
	public int compareTo(EndPoint o);
	public String getId();
	public String getCanonicalContextRoot();
}
