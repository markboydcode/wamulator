package org.lds.sso.appwrap.proxy;

/**
 * Represents the types of traffic passing through the proxy. Site traffic
 * is for any registered virtual site as identified by the request's host and 
 * port located in the request's host header. Not_site is any traffic that
 * is not for the host and port of any registered site. Unclassified traffic
 * is traffic that is not been classified as yet.
 * 
 * Types are: {@link #SITE}, {@link #NOT_SITE}, and {@link #UNCLASSIFIED}.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public enum TrafficType {
	/**
	 * Indicates that the full host header of a request matches a configured site
	 * and hence the traffic will be routed to that site.
	 */
	SITE('-'),
	/**
	 * Indicates that the full host header of a request does not match any 
	 * configured by-site declaration. For such requests to be allowed through
	 * the config directive's allow-non-sso-traffic attribute must be set to
	 * true.
	 */
	NOT_SITE('!'),
	/**
	 * Traffic not matching a site nor matching full host header values marked
	 * as related or ignored. Any traffic that is unclassified results in a 404
	 * Not Found response.
	 */
	UNCLASSIFIED('?');
	
	private char typeCharForLogEntries;

    private TrafficType(char typeCharForLogEntries) {
	    this.typeCharForLogEntries = typeCharForLogEntries;
	}
    
    public char getTypeCharForLogEntries() {
        return this.typeCharForLogEntries;
    }
}
