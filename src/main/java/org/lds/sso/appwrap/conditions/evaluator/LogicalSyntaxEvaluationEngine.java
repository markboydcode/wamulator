package org.lds.sso.appwrap.conditions.evaluator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.lds.sso.appwrap.conditions.evaluator.syntax.AND;
import org.lds.sso.appwrap.conditions.evaluator.syntax.Attribute;
import org.lds.sso.appwrap.conditions.evaluator.syntax.NOT;
import org.lds.sso.appwrap.conditions.evaluator.syntax.OR;
import org.lds.sso.appwrap.io.LogUtils;

/**
 * Engine for loading syntax and constructing processing object graph
 * representing such syntax and implementing its characteristics. Syntax is
 * defined by classes representing instances of either {@link IEvaluator} or
 * {@link IEvaluatorContainer}. The supported syntax resides in a static Map.
 * Each instance of the engine has its own Map of constructed evaluator object
 * graphs and a garbage collector that scans this map occasionally cleaning out
 * unused evaluators. An instance can be acquired using the 
 * getSyntaxEvalutionInstance() static method that subclasses this class and
 * overrides {@link #startGarbageCollector()} to prevent garbage collecting.
 * This is done to get evaluator graphs and hence to verify evaluator syntax
 * where that engine instance will be discarded with all referenced objects and
 * hence no need for a long lived garbage collector thread to be launched.
 * 
 * @author BoydMR
 * 
 */
public class LogicalSyntaxEvaluationEngine {
    public static final Logger cLog = Logger.getLogger(LogicalSyntaxEvaluationEngine.class.getName());

    /**
     * The time that an evaluator is allowed to live since last being used
     * before being reclaimed and releasing its memory.
     */
    protected static long UNUSED_EVALUATOR_MAX_LIFE_MILLIS = 30000;

    /**
     * Thread that periodically scans evaluator object graphs held in the
     * instance of the engine cleaning up unused evaluators.
     */
    protected Thread garbageCollector = null;

    /**
     * Map of evaluators by their configuration Strings. If the same
     * configuration is used by two different policies there will be no
     * difference in the graph of evaluators and evaluators must be thread safe
     * so they can be shared for evaluating policies.
     */
    protected final Map<String, EvaluatorUsageHolder> evaluators = new HashMap<String, EvaluatorUsageHolder>();
    
    private volatile boolean stopCollector = false;
    
    private volatile boolean collectorStopped = true;
    
    public void stopGarbageCollecting() {
    	stopCollector = true;
    	int total = 0;
    	while(!collectorStopped && total <= 5000) {
    		total +=100;
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				/* Do Nothing */
			}
    	}
    }

    /**
     * Starts the garbage collector. Unit tests can subclass and override this
     * method to prevent garbage collecting.
     */
    protected void startGarbageCollector() {
        Runnable collector = new Runnable() {
            public void run() {
            	collectorStopped = false;
                long samplingDelay = UNUSED_EVALUATOR_MAX_LIFE_MILLIS / 2;
                StringWriter sw = null;
                PrintWriter pw = null;
                boolean pwLineWrapped = false;
                while (!stopCollector) {
                    try {
                        if (cLog.isLoggable(Level.FINE)) {
                            if (pw != null && sw != null) {
                                if (!pwLineWrapped) {
                                    // means no evaluator log entries were written
                                    pw.println(" sleeping for " + samplingDelay
                                            + " milliseconds.");
                                }
                                else {
                                    pw.println("SCANNER sleeping for " + samplingDelay
                                            + " milliseconds.");
                                }
                                pw.flush();
                                LogUtils.fine(cLog, sw.toString());
                            }
                            // start new ones
                            sw = new StringWriter();
                            pw = new PrintWriter(sw);
                        }
                        int sleep = 0;
                        while(sleep < samplingDelay && !stopCollector) {
                        	sleep+=100;
                        	Thread.sleep(100);
                        }
                        if (pw != null) {
                            pw.println();
                            pw.print("SCANNING evaluators...");
                        }
                        synchronized (evaluators) {
                            pwLineWrapped = false;
                            for (Iterator<Map.Entry<String, EvaluatorUsageHolder>> itr = evaluators
                                    .entrySet().iterator(); itr.hasNext();) {
                                Map.Entry<String, EvaluatorUsageHolder> entry = itr
                                        .next();
                                EvaluatorUsageHolder holder = entry.getValue();
                                long current = System.currentTimeMillis();
                                long diff = current - holder.millisTouchedTime;

                                if (diff > UNUSED_EVALUATOR_MAX_LIFE_MILLIS) {
                                    if (pw != null) {
                                        if (!pwLineWrapped) {
                                            pw.println();
                                            pwLineWrapped = true;
                                        }
                                        pw.println("  unused for "
                                                + diff
                                                + " ms. REMOVING "
                                                + holder.name);
                                    }
                                    itr.remove();
                                } else {
                                    if (pw != null) {
                                        if (!pwLineWrapped) {
                                            pw.println();
                                            pwLineWrapped = true;
                                        }
                                        pw.println("  evaluator unused for "
                                                + diff
                                                + " ms. LEAVING "
                                                + holder.name);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            LogUtils.severe(cLog, "{0} interrupted. Exiting.", e, Thread.currentThread().getName());
                            return;
                        }
                        LogUtils.severe(cLog, "{0} incurred problem while scanning.", e, Thread.currentThread().getName());
                    }
                }
                collectorStopped = true;
            }
        };
        garbageCollector = new Thread(collector);
        garbageCollector.setDaemon(true);
        garbageCollector.setName(this.getClass().getName()
                + ".unused-evaluators-cleaner");
        startGarbageCollecting();
    }

    /**
     * Private constructor for singleton instantiation.
     */
    public LogicalSyntaxEvaluationEngine() {
        startGarbageCollector();
    }
    
    /**
     * Creates a discardable instance of the engine that won't start an
     * evalution garbage collector. For that use the constructor. 
     * @return
     */
    public static final LogicalSyntaxEvaluationEngine getSyntaxEvalutionInstance() {
        return new LogicalSyntaxEvaluationEngine()
        {
            @Override
            public void startGarbageCollecting() {
                // leave empty to prevent garbage collector from starting since
                // we are only instantiating to test the syntax of the evaluator
                // and this object should get garbage collected and discarded
            }
        };
    }

    /**
     * Allows subclasses to change constructor behavior including launching of
     * collector thread if needed if this ever gets subclassed.
     */
    public void startGarbageCollecting() {
        garbageCollector.start();
    }

    /**
     * Gets an IEvaluator for the given configuration String possibly returning
     * it from cache or parsing it if it has not yet been created.
     * 
     * @param syntax
     *            the xml content of the custom rule
     * @return the IEvaluator object that handles the passed-in syntax
     * @throws EvaluationException
     *             if no evaluator is found for the syntax or if there is an
     *             error in the synatx
     */
    public IEvaluator getEvaluator(String alias, String syntax) throws EvaluationException {
    	return getEvaluator(alias, syntax, false);
    }
    
    public IEvaluator getEvaluator(String alias, String syntax, boolean debug) throws EvaluationException {
        syntax = syntax.trim();
        EvaluatorUsageHolder holder = null;

        synchronized (evaluators) {
            holder = evaluators.get(syntax);
        }

        if (holder != null) {
            holder.millisTouchedTime = System.currentTimeMillis();
        } else {
        	ExprNode filter = null;
            try {
				filter = FilterParser.parse(syntax);
				
			} catch (ParseException e) {
				throw new EvaluationException("Unable to parse syntax '" + syntax + "'.", e);
			}

            holder = new EvaluatorUsageHolder(alias, filterToEvaluator(filter, debug));
            synchronized (evaluators) {
                evaluators.put(syntax, holder);
            }
        }
        return holder.evaluator;
    }
    
    /**
     * Converts a filter to an evaluator.
     * @param filter ExprNode filter representing an ldap query
     * @return evaluator IEvaluator
     */
    @SuppressWarnings("rawtypes")
	public IEvaluator filterToEvaluator(ExprNode filter, boolean debug) {
    	IEvaluator ie = null;
    	Map<String, String> aMap = new TreeMap<String, String>();
    	
    	if (filter.isLeaf()) {
            String headerName = ((LeafNode) filter).getAttribute();
            aMap.put("name", headerName);
            String operation = "EQUALS";
            String value = "";
            if (filter.getClass() == SubstringNode.class) {
            	SubstringNode ssNode = (SubstringNode) filter;
            	if (ssNode.getAny() != null && ssNode.getAny().size() > 0) {
            		value = "*" + ssNode.getAny().get(0) + "*";
            	} else if (ssNode.getInitial() != null) {
            		value = ssNode.getInitial() + "*";
            	} else if (ssNode.getFinal() != null) {
            		value = "*" + ssNode.getFinal();
            	}
            } else if (filter.getClass() == EqualityNode.class) {
            	Value bv = ((EqualityNode) filter).getValue();
            	value = bv.toString();
            } else if (filter.getClass() == PresenceNode.class) {
            	operation = "EXISTS";
            } 
            aMap.put("operation", operation);
            aMap.put("value", value);
            if (debug) {
            	aMap.put("debug", "true");
            }
            
            try {
				ie = (IEvaluator) Attribute.class.newInstance();
				ie.init(filter.toString(), aMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
    	} else if (filter.getClass() == OrNode.class) {
    		try {
    			ie = (IEvaluator) OR.class.newInstance();
    			if (debug) {
                	aMap.put("debug", "true");
                }
    			ie.init(filter.toString(), aMap);
    			for (ExprNode child : ((OrNode) filter).getChildren()) {
    				((OR) ie).addEvaluator(filterToEvaluator(child, debug));
    			}
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	} else if (filter.getClass() == AndNode.class) {
    		try {
    			ie = (IEvaluator) AND.class.newInstance();
    			if (debug) {
                	aMap.put("debug", "true");
                }
    			ie.init(filter.toString(), aMap);
    			for (ExprNode child : ((AndNode) filter).getChildren()) {
    				((AND) ie).addEvaluator(filterToEvaluator(child, debug));
    			} 
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	} else if (filter.getClass() == NotNode.class) {
    		try {
    			ie = (IEvaluator) NOT.class.newInstance();
    			if (debug) {
                	aMap.put("debug", "true");
                }
    			ie.init(filter.toString(), aMap);
    			for (ExprNode child : ((NotNode) filter).getChildren()) {
    				((NOT) ie).addEvaluator(filterToEvaluator(child, debug));
    			} 
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        }
    	
    	return ie;
    }

    /**
     * Holds and instance of IEvaluator along with the millis time of when the
     * evaluator was last selected from cache allowing for purging unused
     * evaluator object graphs when policies are changed.
     * 
     * @author Mark Boyd
     * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day
     *             Saints
     * 
     */
    public static class EvaluatorUsageHolder {
        public IEvaluator evaluator = null;
        public volatile long millisTouchedTime = 0;
        private String name;

        public EvaluatorUsageHolder(String alias, IEvaluator e) {
            this.name = alias;
            this.evaluator = e;
            this.millisTouchedTime = System.currentTimeMillis();
        }
    }
}
