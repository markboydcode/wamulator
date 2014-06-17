package org.lds.sso.appwrap;

import org.lds.sso.appwrap.AppEndPoint.InboundScheme;
import org.lds.sso.appwrap.AppEndPoint.Scheme;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.LogicalSyntaxEvaluationEngine;
import org.lds.sso.appwrap.conditions.evaluator.syntax.AllowDeny;
import org.lds.sso.appwrap.identity.User;
import org.lds.sso.appwrap.io.LogUtils;

import java.util.*;
import java.util.logging.Logger;

/**
 * This element corresponds to by-site xml declarations in the XML config file. Its contents correspond to the
 * contents of by-site's nested XML constructs. I
 */
public class SiteMatcher {
	private static final Logger cLog = Logger.getLogger(SiteMatcher.class.getName());
	private String host;
	private int port;
	protected Set<OrderedUri> urls = new TreeSet<OrderedUri>();
	private Set<EndPoint> mappedEndPoints = new TreeSet<EndPoint>();

	private InboundScheme scheme;
	private LogicalSyntaxEvaluationEngine cEngine;
	protected Map<String, String> cSynMap;
	protected Map<OrderedUri, String> conditionsMap = new HashMap<OrderedUri, String>();
	protected Map<String, Boolean> allowTakesPrecedenceMap = new HashMap<String, Boolean>();
    
    protected int endPoints = 0;
    protected int policies = 0;
	private EndPoint lastEndPointAdded = null;

	/**
	 * Create a matcher that allows multiple URL endpoints to be configured within it potentially with conditions
     * specified that a user must match before traffic will be allowed to that endpoint.
	 * 
	 * @param host
	 * @param port
	 */
	public SiteMatcher(InboundScheme scheme, String host, int port, TrafficManager trafficMgr) {
		this.setConditionEnv(trafficMgr.cEngine, trafficMgr.cSyntaxMap);
		this.scheme = scheme;
		this.host = host;
		this.port = port;
	}
	
	public OrderedUri getUriMatcher(Scheme scheme, String host, int port, String path, String query) {
        for(OrderedUri uri : urls) {
            if (uri.matches(scheme, host, port, path, query)) {
                return uri;
            }
        }
        return null;
	}

	public boolean isAllowed(Scheme scheme, String host, int port, String action, String path, String query, User user) {
		if (this.port == port && this.host.equals(host)) {
		    OrderedUri uri = getUriMatcher(scheme, host, port, path, query);
		    if (uri != null) {
                if (uri.getClass() == UnenforcedUri.class) {
                    return true;
                }
                if (uri.getClass() == DeniedUri.class) {
                    return false;
                }
                // instance of AllowedUri
                AllowedUri au = (AllowedUri) uri;
                if (au.allowed(action)) {
            
                    String condId = conditionsMap.get(au);
                    if (condId == null) { // no condition needs to be met
                        return true;
                    } else { // must further meet conditions for access
                    	String allowCondId = condId + "-allow";
                    	String denyCondId = condId + "-deny";
                        String allowSyntax = cSynMap.get(allowCondId);
                        String denySyntax = cSynMap.get(denyCondId);
                        IEvaluator allowEvaluator = null;
                        IEvaluator denyEvaluator = null;
                        try {
                            if (allowSyntax != null) {
                            	allowEvaluator = cEngine.getEvaluator(allowCondId, allowSyntax);
                            }
                            if (denySyntax != null) {
                            	denyEvaluator = cEngine.getEvaluator(denyCondId, denySyntax);
                            }
                        }
                        catch (EvaluationException e) {
                            LogUtils.severe(cLog, "Disallowing access to {0} since unable to obtain evaluator for condition alias {1} with allow syntax {2} and deny syntax {3}.", e, au, condId, allowSyntax, denySyntax); 
                            return false;
                        }
                        EvaluationContext ctx = new EvaluationContext(user, new HashMap<String,String>());
                        AllowDeny adEvaluator = new AllowDeny(allowEvaluator, denyEvaluator, doesAllowTakePrecedence(au.pathMatch));
                        try {
                            return adEvaluator.isConditionSatisfied(ctx);
                        }
                        catch (EvaluationException e) {
                            LogUtils.severe(cLog, "Error occurred for {0} for user {1} denying access.", e, au, user.getUsername());
                        }
                        return false;
                    }
                }
                return false;
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
		EndPoint result = null;
		
		int queryIndex = canUrl.indexOf("?");
		String canQueryString = null;
		if (queryIndex > 0) {
			canQueryString = canUrl.substring(queryIndex + 1);
			canUrl = canUrl.substring(0, queryIndex);
		}

		for(EndPoint ep : mappedEndPoints) {
			cLog.fine("@@ getEndpointForCanonicalUrl: comparing: " + ep.getContextRoot() + " and: " + canUrl);
			
			String canonCtxRoot = ep.getContextRoot();
			if (canonCtxRoot != null) {
				if (UriMatcher.matches(canUrl, canonCtxRoot)) {
					result = ep;
					break;
				}
			}
			
			String queryString = ep.getQueryString();
			if (queryString != null && canQueryString != null) {
				if (UriMatcher.matches(canQueryString, queryString)) {
					result = ep;
					break;
				}
			}
		}
		if (result == null) {
			cLog.info("FAILURE: we didn't find a tpath endpoint that matches.");
		} else {
			cLog.info("SUCCESS: selected matcher was --> " + result.getOriginalName());
		}
		
		return result;
	}

	/**
	 * Returns true if this object has the same configured InboundScheme, host, 
	 * and port.
	 * 
	 * @param scheme
	 * @param host
	 * @param port
	 * @return
	 */
	public boolean isSame(InboundScheme scheme, String host, int port) {
		return this.scheme == scheme && this.host.equalsIgnoreCase(host) && this.port == port;
	}

	/**
	 * Returns true if this site matches on the scheme, host, port of a uri.
	 * @param scheme 
	 */
	public boolean matches(Scheme scheme, String host, int port) {
		if (this.scheme == InboundScheme.BOTH && this.host.equals(host)) {
			cLog.fine("SUCCESS: The site matches, use this site: " + this);
			return true; 
		}
		if (this.scheme != InboundScheme.BOTH && this.scheme.moniker.equals(scheme.moniker) && this.host.equals(host) && this.port == port) {
			cLog.fine("SUCCESS: The site matches, use this site: " + this);
			return true; 
		}
		cLog.fine("FAILED: The site doesn't match: " + this);
		return false;
	}

    /**
     * Adds an instance of EndPoint to the set of mapped endpoints in the matcher.
     *
     * @param point
     */
	public void addMapping(EndPoint point) {
		point.setDeclarationOrder(++endPoints);
		mappedEndPoints.add(point);
		lastEndPointAdded = point;
	}
	
	public EndPoint getLastMappingAdded() {
		return lastEndPointAdded;
	}
	
    /**
     * Called whenever proxy port changes. This allows for late binding of the
     * SiteMatcher ports when using "auto" port binding for the proxy port and
     * the by-site port attribute specifies use of the proxy-port alias.
     *  
     * @param proxyPort
     */
	public void proxyPortChanged(int proxyPort) {
	    if (this.port == 0) {
	        this.port = proxyPort;

	        // if matcher had zero port then all unenforced and allowed urls
	        // will have inherited it and must be updated as well
	        List<OrderedUri> updates = new ArrayList<OrderedUri>();
	        
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
	            if (aep.getEndpointHttpPort() == 0) {
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
     * Determines if the passed-in url is an unenforcedUrl or matches a defined
     * one.
     * @param query 
     * 
     * @param scheme
	 * @param host
	 * @param port
	 * @param path
	 * @param query
     * @return
     */
    public boolean isUnenforced(Scheme scheme, String host, int port, String path, String query) {
        OrderedUri url = getManagerOfUri(scheme, host, port, path, query);
        if (url != null) {
            if (url.getClass() == UnenforcedUri.class) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the passed-in url is an enforced Url.
     * 
     * @param query 
     * 
     * @return
     */
    public boolean isEnforced(Scheme scheme, String host, int port, String path, String query) {
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
     * @return
     */
    public OrderedUri getManagerOfUri(Scheme scheme, String host, int port, String path, String query) {
        for (OrderedUri url : urls) {
            if (url.matches(scheme, host, port, path, query)) {
                return url;
            }
        }
        return null;
    }

	public void addAllowedUri(AllowedUri au, String condId, String allow) {
	    au.setDeclarationOrder(++policies);
		this.urls.add(au);
		
		if (allow == null) {
			allow = "(cn=*)";
		}
 
		if (condId != null) {
            cSynMap.put(condId + "-allow", allow.toString());
            conditionsMap.put(au, condId);
        }
	}
	
	public void addDeniedUri(DeniedUri du, String condId, String deny) {
		du.setDeclarationOrder(++policies);
		this.urls.add(du);
 
		if (condId != null && deny != null) {
            cSynMap.put(condId + "-deny", deny.toString());
            conditionsMap.put(du, condId);
        }
	}
	
	public void addAllowTakesPrecedence(String uri, boolean allowTakesPrecedence) {
		allowTakesPrecedenceMap.put(uri, new Boolean(allowTakesPrecedence));
	}
	
	public boolean doesAllowTakePrecedence(String uri) {
		return allowTakesPrecedenceMap.get(uri);
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public InboundScheme getScheme() {
		return scheme;
	}

	public void setConditionEnv(LogicalSyntaxEvaluationEngine engine, Map<String,String>syntaxMap) {
		cEngine = engine;
		cSynMap = syntaxMap;
	}

	public void addFileMapping(String originalName, String cctx, String file, String type) {
		EndPoint ep = new LocalFileEndPoint(originalName, cctx, file, type);
        ep.setDeclarationOrder(++endPoints);
		mappedEndPoints.add(ep);
	}
    
    public void addUnenforcedMapping(String originalName, String cctx, String thost, int tport) {
        EndPoint ep = new UnenforcedEndPoint(originalName, cctx, thost, tport);
        ep.setDeclarationOrder(++endPoints);
        mappedEndPoints.add(ep);
    }
	
	public String toString() {
	    return this.scheme + "://" + this.host + ":" + this.port;
	}
}
