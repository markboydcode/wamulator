package org.lds.sso.appwrap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Represents an internet site that is implemented as a reverse proxy 
 * aggregating content and features from one to many other applications with
 * perhaps differing implementation technologies. A virtual site has a DNS name
 * and optional port. The default port is port 80. It also has from one to many
 * application endpoints representing the applications to which its traffic is
 * routed to blend that application's featureset into that of the virtual site.
 * An application endpoint has a canonical context and an application context
 * which can be the same as the canonical. The application endpoint canonical 
 * context represents the
 * single top level path combination for which all URL requests for sub-paths of
 * that top level path are handled by this application. For example, suppost 
 * that a ward directory application is part of the internet site lds.org and
 * is located at /member/direcotry/... Numerous sub-paths are part of this
 * application such as /member/directory/unit/786/list, and 
 * /member/directory/unit/786/family/24, and so on. In this example the 
 * canonical context is /member/directory and the virtual site is lds.org.
 * 
 * Furthermore, suppose that this directory is implemented with a java 
 * application located at host456 on port 8080 with a java context of 
 * /directory-v4. The application context would then be /directory-v4 with an
 * application host of host456 and port 8080. All requests made of the virtual
 * site for /member/directory... would be transformed to /directory-v4... 
 * before passing the requests on to host 456 port 8080.
 *  
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class VirtualSite {
	private String hostAndPort;
	private int port = 0;
	private Map<String, AppEndPoint> apps = new TreeMap<String, AppEndPoint>();
	
	
	public VirtualSite(String hostAndPort) {
		this.hostAndPort = hostAndPort;
	}
	
	public String getHostAndPort() {
		return hostAndPort;
	}
	
	public void setHostAndPort(String hostAndPort) {
		this.hostAndPort = hostAndPort;
	}

	/**
	 * Creates or replaces an application endpoint registration enabling the 
	 * reverse proxy to route requests for URIs starting with the canonicalCtx 
	 * to the indicated port with the canonicalCtx replaced with the appCtx
	 * value and either a header or query parameter of cctx added containing the canonicalCtx
	 * value so that the back end application can tell how to properly redirect
	 * in the canonical URL space.
	 * 
	 * @param canonicalCtx
	 * @param appCtx
	 * @param appPort
	 */
	public void setApplication(String canonicalCtx, String appCtx, int appPort) {
		// removed since replacing with new config approach.
		/*
		AppEndPoint ep = new AppEndPoint(canonicalCtx, appCtx, appPort);
		apps.put(canonicalCtx, ep);
		*/
	}

	/**
	 * Removes an application registration preventing the reverse proxy from 
	 * routing any incoming requests matching that canonical context to the 
	 * application's port in the future.
	 * 
	 * @param canonicalCtx
	 */
	public void removeApplication(String canonicalCtx) {
		apps.remove(canonicalCtx);
	}
	
	/**
	 * Returns the collection of registered AppEndPoints.
	 * 
	 * @return
	 */
	public Collection<AppEndPoint> getApplications() {
		return apps.values();
	}

	
	/**
	 * Scans the set of registered applications for a canonical context that
	 * matches the start of the passed-in canonical URL to identify an 
	 * application port to which to route the request.
	 * 
	 * @return
	 */
	public AppEndPoint getAppEndpointForCanonicalUrl(String canUrl) {
		for(AppEndPoint ep : apps.values()) {
			if (canUrl.startsWith(ep.getCanonicalContextRoot())) {
				return ep;
			}
		}
		return null;
	}
}
