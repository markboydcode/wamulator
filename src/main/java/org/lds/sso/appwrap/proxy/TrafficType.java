package org.lds.sso.appwrap.proxy;

/**
 * Represents the types of traffic passing through the proxy. Site traffic
 * is for any registered virtual site as identified by the request's host and 
 * port located in the request's host header. Related traffic is any traffic that
 * is not for the host and port of any registered site but is used by the site 
 * and hence should show in traffic logs. Ignored is any traffic completely 
 * unrelated to the the registered sites and their applications. Unclassified traffic
 * is traffic that is not related to sites nor has been classified yet as being
 * related or ignored.
 * 
 * Types are: {@link #SITE}, {@link #RELATED}, {@link #IGNORED}, and {@link #UNCLASSIFIED}.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public enum TrafficType {
	/**
	 * Indicates that the full host header of a request matches a configured site
	 * and hence the traffic will be routed to that site. All site traffic is
	 * fully logged.
	 */
	SITE,
	/**
	 * Indicates that the full host header of a request has been marked as an
	 * external site
	 * whose traffic is related to one of the internal configured sites although it is
	 * not that site's traffic. For example, the bishop app registered as a site but 
	 * delegating to the external cdol application. Related traffic is logged
	 * for use in troubleshooting integration issues between applications.
	 */
	RELATED,
	/**
	 * Traffic to the full host header has been marked as ignored and is not 
	 * shown in logs or in the console.
	 */
	IGNORED,
	/**
	 * Traffic not matching a site nor matching full host header values marked
	 * as related or ignored. Any traffic that is unclassified results in a 404
	 * Not Found response.
	 */
	UNCLASSIFIED
}
