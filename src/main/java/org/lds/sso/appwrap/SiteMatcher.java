package org.lds.sso.appwrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.LogicalSyntaxEvaluationEngine;
import org.lds.sso.appwrap.io.LogUtils;

public class SiteMatcher {
	private static final Logger cLog = Logger.getLogger(SiteMatcher.class.getName());
	private String host;
	private int port;
	protected Set<OrderedUri> urls = new TreeSet<OrderedUri>();
	private Set<EndPoint> mappedEndPoints = new TreeSet<EndPoint>();

	private String scheme;
	private LogicalSyntaxEvaluationEngine cEngine;
	protected Map<String, String> cSynMap;
	protected Map<AllowedUri, String> conditionsMap = new HashMap<AllowedUri, String>();
    
    protected int endPoints = 0;
    protected int policies = 0;

	/**
	 * Create a matcher that allows multiple URLs to be configured within it.
	 * 
	 * @param host
	 * @param port
	 */
	public SiteMatcher(String scheme, String host, int port, TrafficManager trafficMgr) {
		this.setConditionEnv(trafficMgr.cEngine, trafficMgr.cSyntaxMap);
		this.scheme = scheme;
		this.host = host;
		this.port = port;
	}
	
	public OrderedUri getUriMatcher(String scheme, String host, int port, String path, String query) {
        for(OrderedUri uri : urls) {
            if (uri.matches(scheme, host, port, path, query)) {
                return uri;
            }
        }
        return null;
	}

	public boolean isAllowed(String scheme, String host, int port, String action, String path, String query, User user) {
		if (this.port == port && this.host.equals(host)) {
		    OrderedUri uri = getUriMatcher(scheme, host, port, path, query);
		    if (uri != null) {
                if (uri.getClass() == UnenforcedUri.class) {
                    return true;
                }
                // instance of AllowedUri
                AllowedUri au = (AllowedUri) uri;
                if (au.allowed(action)) {
            
                    String condId = conditionsMap.get(au);
                    if (condId == null) { // no condition needs to be met
                        return true;
                    }
                    else { // must further meet conditions for access
                        String syntax = cSynMap.get(condId);
                        IEvaluator evaluator = null;
                        try {
                            evaluator = cEngine.getEvaluator(condId, syntax);
                        }
                        catch (EvaluationException e) {
                            LogUtils.severe(cLog, "Disallowing access to {0} since unable to obtain evaluator for condition alias {1} with syntax {2}.", e, au, condId, syntax); 
                            return false;
                        }
                        EvaluationContext ctx = new EvaluationContext(user, new HashMap<String,String>());
                        try {
                            return evaluator.isConditionSatisfied(ctx);
                        }
                        catch (EvaluationException e) {
                            LogUtils.severe(cLog, "Error occurred for {0} for user {1} denying access.", e, au, user.getUsername());
                        }
                        return false;
                    }
                }
                return true;
            }
		}
		return false;
	}

	/**
	 * Finds a registered mapping to an endpoint or null if none is
	 * found matching the passed in canonical url.
	 * @param canUrl
	 * @return
	 */
	public EndPoint getEndpointForCanonicalUrl(String canUrl) {
		for(EndPoint ep : mappedEndPoints) {
			// TODO: will have to delegate into endpoint to evaluate when we
			// add support for backward references and env macros for fileendpoints.
			if (canUrl.startsWith(ep.getCanonicalContextRoot())) {
				return ep;
			}
		}
		return null;
	}

	/**
	 * Returns true if this site matches on the host, port, and uri.
	 */
	public boolean matches(String host, int port) {
		if (!this.host.equals(host) || this.port != port) {
			return false;
		}
		return true; 
	}
	
	public void addMapping(EndPoint point) {
		point.setDeclarationOrder(++endPoints);
		mappedEndPoints.add(point);
	}
	
    /**
     * Called whenever proxy port changes. This allows for late binding of the
     * SiteMatcher ports when using "auto" port binding for the proxy port and
     * the by-site port attribute specifies use of the proxy-port alias.
     *  
     * @param consolePort
     */
	public void proxyPortChanged(int proxyPort) {
	    if (this.port == 0) {
	        this.port = proxyPort;

	        // if matcher had zero port then all unenforced and allowed urls
	        // will have inherited it and must be updated as well
	        List<OrderedUri> updates = new ArrayList<OrderedUri>();
	        Map<String, AllowedUri> cUpdates = new HashMap<String, AllowedUri>();
	        
	        for (Iterator<OrderedUri> itr=this.urls.iterator(); itr.hasNext();) {
	            OrderedUri uri = itr.next();
	            
                // remove from conditions map before changing since it
                // affects our hashcode and we'll lose the linkage to the
                // condition syntax if that uri had a condition
	            String cond = conditionsMap.remove(uri);
                if (uri.proxyPortChanged(proxyPort)) {
                    itr.remove();
                    updates.add(uri);
                }
                if (cond != null) {
                    // cast here is fine since only allowed will have conditions
                    conditionsMap.put((AllowedUri)uri, cond);
                }
	        }
	        if (updates.size() >0) {
	            urls.addAll(updates);
	        }
	    }
	}
	
	/**
	 * Called whenever console port changes. This allows for late binding of the
	 * AppEndPoint ports when using "auto" port binding for the console port and
	 * the cctx-mapping tport attribute specifies use of the console-port.
	 *  
	 * @param consolePort
	 */
	public void consolePortChanged(int consolePort) {
	    List<EndPoint> updates = new ArrayList<EndPoint>();
	    
	    for (Iterator<EndPoint> itr=mappedEndPoints.iterator(); itr.hasNext();) {
	        EndPoint ep = itr.next();
	        
	        if (ep instanceof AppEndPoint) {
	            AppEndPoint aep = (AppEndPoint) ep;
	            if (aep.getEndpointPort() == 0) {
	                itr.remove();
	                aep.setEndpointPort(consolePort);
	                updates.add(aep);
	            }
	        }
	    }
	    if (updates.size() >0) {
	        mappedEndPoints.addAll(updates);
	    }
	}

	public void addUnenforcedUri(UnenforcedUri uu) {
        uu.setDeclarationOrder(++policies);
		urls.add(uu);
	}
	
    /**
     * Determines if the passed-in url is an unenforeceUrl or matches a defined
     * one.
     * @param query 
     * 
     * @param uri
     * @return
     */
    public boolean isUnenforced(String scheme, String host, int port, String path, String query) {
        OrderedUri url = getManagerOfUri(scheme, host, port, path, query);
        if (url != null) {
            if (url.getClass() == UnenforcedUri.class) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the passed-in url is an enforece Url.
     * 
     * @param query 
     * 
     * @return
     */
    public boolean isEnforced(String scheme, String host, int port, String path, String query) {
        OrderedUri url = getManagerOfUri(scheme, host, port, path, query);
        if (url != null) {
            if (url.getClass() == AllowedUri.class) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a subclass of OrderedUri that manages the specified URL or null
     * if no registered URIs match this URL.
     * 
     * @param scheme
     * @param host
     * @param port
     * @param path
     * @param query
     * @param cls
     * @return
     */
    public OrderedUri getManagerOfUri(String scheme, String host, int port, String path, String query) {
        for (OrderedUri url : urls) {
            if (url.matches(scheme, host, port, path, query)) {
                return url;
            }
        }
        return null;
    }

	public void addAllowedUri(AllowedUri au, String condId, String condSyntax) {
	    au.setDeclarationOrder(++policies);
		this.urls.add(au);
 
		if (condId != null && condSyntax != null) {
            cSynMap.put(condId, condSyntax);
            conditionsMap.put(au, condId);
        }
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getScheme() {
		return scheme;
	}

	public void setConditionEnv(LogicalSyntaxEvaluationEngine engine, Map<String,String>syntaxMap) {
		cEngine = engine;
		cSynMap = syntaxMap;
	}

	public void addFileMapping(String cctx, String file, String type) {
		EndPoint ep = new LocalFileEndPoint(cctx, file, type);
        ep.setDeclarationOrder(++endPoints);
		mappedEndPoints.add(ep);
	}
	
	public String toString() {
	    return this.scheme + "://" + this.host + ":" + this.port;
	}
}
