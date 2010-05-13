package org.lds.sso.appwrap.conditions.evaluator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lds.sso.appwrap.conditions.evaluator.syntax.*;
import org.xml.sax.*;

import javax.xml.parsers.SAXParserFactory;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class LogicalSyntaxEvaluationEngine {
	protected static final Logger cLog = Logger.getLogger(LogicalSyntaxEvaluationEngine.class);
	
	/**
	 * The map of syntax implementation classes.
	 */
	protected static final Map<String, Class> DICTIONARY = getSyntax();
	
	protected static long UNUSED_EVALUATOR_MAX_LIFE_MILLIS = 30000;
	
	protected Thread garbageCollector = null;
	
	/**
	 * Map of evaluators by their configuration Strings. If the same configuration
	 * is used by two different policies there will be no difference in the 
	 * graph of evaluators and evaluators must be thread safe so they can be shared
	 * for evaluating policies.
	 */
	protected final Map<String, EvaluatorUsageHolder> evaluators = new HashMap<String, EvaluatorUsageHolder>();
	
	public LogicalSyntaxEvaluationEngine() {
		Runnable collector = new Runnable() {
			public void run() {
				long samplingDelay = UNUSED_EVALUATOR_MAX_LIFE_MILLIS/2;
				while (true) {
					try {
						StringWriter sw = null;
						PrintWriter pw = null;
						if (cLog.isDebugEnabled()){
							sw = new StringWriter();
							pw = new PrintWriter(sw);
							cLog.debug("SCANNER sleeping for " + samplingDelay + " milliseconds.");
						}
						Thread.sleep(samplingDelay);
						if (pw != null) {
							pw.println();
							pw.println("SCANNING evaluators...");
						}
						synchronized (evaluators) {
							for (Iterator<Map.Entry<String, EvaluatorUsageHolder>> itr = evaluators.entrySet().iterator(); itr
									.hasNext();) {
								Map.Entry<String, EvaluatorUsageHolder> entry = itr.next();
								EvaluatorUsageHolder holder = entry.getValue();
								long current = System.currentTimeMillis();
								long diff = current - holder.millisTouchedTime;

								if (diff > UNUSED_EVALUATOR_MAX_LIFE_MILLIS) {
									if (pw != null) {
										pw.println("  evaluator unused for " + diff 
											+ " milliseconds. REMOVING "
											+ holder.evaluator.getSyntax().substring(0,20)
											+ "...");
									}
									itr.remove();
								}
								else {
									if (pw != null) {
										pw.println("  evaluator unused for " + diff 
											+ " milliseconds. LEAVING "
											+ holder.evaluator.getSyntax().substring(0,20)
											+ "...");
									}
								}
							}
						}
						if (pw != null) {
							pw.flush();
							cLog.debug(sw.toString());
						}
					}
					catch (Exception e) {
						if (e instanceof InterruptedException) {
							cLog.error(Thread.currentThread().getName()
									+ " interrupted. Exiting.", e);
							return;
						}
						cLog.error(Thread.currentThread().getName()
								+ " incurred problem while scanning.", e);
					}
				}
			}
		};
		garbageCollector = new Thread(collector);
		garbageCollector.setDaemon(true);
		garbageCollector.setName(this.getClass().getName() + ".unused-evaluators-cleaner");
		startGarbageCollecting();
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
	 * @param syntax the xml content of the custom rule
	 * @return the IEvaluator object that handles the passed-in syntax
	 * @throws EvaluationException if no evaluator is found for the syntax or if there is an error in the synatx
	 */
	public IEvaluator getEvaluator(String syntax) throws EvaluationException {
		syntax = syntax.trim();
		EvaluatorUsageHolder holder = null;
		
		synchronized(evaluators) {
			holder = evaluators.get(syntax);
		}
		
		if (holder != null) {
			holder.millisTouchedTime = System.currentTimeMillis();
		}
		else {
			XMLReader rdr;
			try {
				rdr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			}
			catch (Exception e) {
				throw new EvaluationException("Unable to create parser for syntax '" + syntax 
						+ "'.", e);
			}
			SyntaxContentHandler hndlr = new SyntaxContentHandler(syntax); 
			rdr.setContentHandler(hndlr);
			InputSource src = new InputSource(new StringReader(syntax));
			try {
				rdr.parse(src);
			}
			catch (Exception e) {
				throw new EvaluationException("Unable to parse syntax '" + syntax 
						+ "'.", e);
			}
			
			if (hndlr.root != null) {
				holder = new EvaluatorUsageHolder(hndlr.root);
				synchronized(evaluators) {
					evaluators.put(syntax, holder);
				}
			}
			else {
				throw new EvaluationException("Parsing syntax '" + syntax 
						+ "' produced no evaluators. Enter valid syntax.");
			}
		}
		return holder.evaluator;
	}
	
	/**
	 * Utility method for registering compile time dependencies on syntax 
	 * implementation classes in a map.
	 * 
	 * @return	the map of handlers
	 */
	private static Map<String, Class> getSyntax() {
		Map<String, Class> map = new HashMap<String, Class>();

		add(map, HasLdsAccountId.class);
		add(map, IsEmployee.class);
		add(map, IsMember.class);
		add(map, MemberOfUnit.class);
		add(map, HasPosition.class);
		add(map, HasAssignment.class);
		add(map, Assignment.class);
		add(map, Position.class);
		add(map, Unit.class);
		add(map, AND.class);
		add(map, OR.class);
		add(map, NOT.class);
		add(map, LdsAccount.class);
		add(map, CtxMatches.class);

		return map;
	}
	
	/**
	 * Convenience method making addition of supported classes simpler.
	 * @param m	map to add the class to
	 * @param c	class to add
	 */
	private static void add(Map<String, Class> m, Class c) {
		m.put(c.getSimpleName(), c);
	}
	
	/**
	 * Holds and instance of IEvaluator along with the millis time of when the
	 * evaluator was last selected from cache allowing for purging unused
	 * evaluator object graphs when policies are changed.
	 * 
	 * @author Mark Boyd
	 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
	 *
	 */
	public static class EvaluatorUsageHolder {
		public IEvaluator evaluator = null;
		public volatile long millisTouchedTime = 0;
		
		public EvaluatorUsageHolder(IEvaluator e) {
			this.evaluator = e;
			this.millisTouchedTime = System.currentTimeMillis();
		}
	}
	
	/**
	 * Holds an instance of IEvaluator along with the xml:base attribute value
	 * had on the element that defined the evaluator IF it had such an attribute
	 * at all. This is to allow us to cache the evaluator by its xml:base
	 * value so that it can be used by other policy syntax that also includes 
	 * the same graph representing the same xml:base chunk of xml nested within
	 * the policy.
	 * 
	 * @author Mark Boyd
	 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
	 *
	 */
	public static class MidParseEvaluatorHolder {
		public IEvaluator evaluator = null;
		public String xmlBase = null;
		
		public MidParseEvaluatorHolder(IEvaluator e, String base) {
			this.evaluator = e;
			this.xmlBase = base;
		}
		
		public boolean hasXmlBase() {
			return xmlBase != null;
		}
	}
	
	/**
	 * Handler for receiving SAX parsing events while processing syntax string.
	 * 
	 * @author Mark Boyd
	 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
	 *
	 */
	public class SyntaxContentHandler implements ContentHandler {
		
		private List<MidParseEvaluatorHolder> containers = new ArrayList<MidParseEvaluatorHolder>();
		private String contentBeingParsed = null;
		private IEvaluator root = null;

		/**
		 * Keeps track of whether the current event is occurring somewhere within
		 * an element that contained an xml:base attribute AND was already cached
		 * and therefore does not require constructing of the evaluator graph.
		 */
		private boolean withinCachedXmlBaseElement;
		
		/**
		 * Keeps track of how deeply within and xml base element an event is 
		 * occurring so that we know when we have arrived back at that element
		 * closing tag and can set withinCachedXmlBaseElement to false.
		 */
		private int depthWithinXmlBaseElement = 0;
		
		public SyntaxContentHandler(String content) {
			contentBeingParsed = content;
		}

		public void startElement(String uri, String ln, String name, Attributes atts) throws SAXException {
			if (withinCachedXmlBaseElement) {
				// ignore events, just stream them on by until we get out of the
				// cached element.
				depthWithinXmlBaseElement++;
				return;
			}
			
			Class cls = DICTIONARY.get(name);
			if (cls == null) {
				String msg = "Unsupported Syntax '" + name + "' in custom syntax evaluation.";
				cLog.setLevel(Level.DEBUG);
				cLog.error(msg);
				cLog.error(contentBeingParsed);
				throw new SAXException(msg);
			}
			try {
				// instantiate the class registered to implement the element
				// and configure it with the element's attributes
				String xmlBase = null;
				IEvaluator ev = null;
				
				// we use a sorted map for ensuring debug output is formated
				// correstly if element debugging is enabled.
				Map<String,String> aMap = new TreeMap<String,String>();
				
				for (int i=0; i<atts.getLength(); i++) {
					String nm = atts.getQName(i);
					String val = atts.getValue(i);
					aMap.put(nm, val);
					
					// check for xml base and see if already cached
					if (nm.equals("xml:base")) {
						EvaluatorUsageHolder holder = evaluators.get(val);
						if (holder != null) {
							// found it already cached, no need to instantiate
							// nested graph.
							withinCachedXmlBaseElement = true;
							ev = holder.evaluator;
						}
						else { // not yet cached, grab xmlBase to cache after parsing
							xmlBase = val;
						}
					}
				}
				
				// instantiate the evaluator corresponding to the element if it
				// wasn't found in cache.
				if (ev == null) {
					ev = (IEvaluator) cls.newInstance();
					ev.init(contentBeingParsed, aMap); 
				}

				// is it a nested element being read, inject into parent evaluator
				// making sure parent is a container

				if (containers.size() > 0) { 
					MidParseEvaluatorHolder holder = containers.get(containers.size()-1);
					IEvaluator parent = holder.evaluator;
					
					if (parent instanceof IEvaluatorContainer) {
						IEvaluatorContainer c = (IEvaluatorContainer) parent;
						c.addEvaluator(ev);
					}
					else {
						throw new SAXException("Unsupported Syntax. '" 
								+ parent.getClass().getSimpleName() + "' in '" 
								+ contentBeingParsed 
								+ "' does not support nested elements such as '"
								+ name + "'.");
					}
				}
				// only cache if we are to build nested tree and validate afterward
				if (!withinCachedXmlBaseElement) {
					MidParseEvaluatorHolder holder = new MidParseEvaluatorHolder(ev, xmlBase);
					containers.add(holder);
				}
				
				if (root == null) {
					root = ev;
				}
			}
			catch (Exception e) {
				throw new SAXException(e);
			}
		}

		public void endElement(String uri, String localName, String name) throws SAXException {
			// see if we have arrived back at the element with xml base found in cache.
			if (withinCachedXmlBaseElement) {
				if (depthWithinXmlBaseElement > 0) {
					// still within xmlbase element, decrement depth and return
					depthWithinXmlBaseElement--;
				}
				else {
					// so we reached depth of zero. we are back on the element that
					// has the xml base attribute
					withinCachedXmlBaseElement = false;
				}
				// yes return here since we didn't cache in container list and
				// don't need to validate
				return;
			}
			
			// pull out of the hierarchy of container elements
			IEvaluator ev = null;
			MidParseEvaluatorHolder holder = containers.remove(containers.size()-1);
			
			// does it have xml:base? cache it by that value.
			if (holder != null) {
				ev = holder.evaluator;
				if (holder.hasXmlBase()) {
					EvaluatorUsageHolder euHolder = new EvaluatorUsageHolder(ev);
					evaluators.put(holder.xmlBase, euHolder);
				}
			}
			
			// is it a container? validate its syntax.
			if (ev != null && ev instanceof IEvaluatorContainer) {
				try {
					((IEvaluatorContainer)ev).validate();
				}
				catch (EvaluationException e) {
					throw new SAXException("Cannot validate container '" +
							ev.getClass().getSimpleName() + "'.", e);
				}
			}
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
		}

		public void endDocument() throws SAXException {
		}

		public void endPrefixMapping(String prefix) throws SAXException {
		}

		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		}

		public void processingInstruction(String target, String data) throws SAXException {
		}

		public void setDocumentLocator(Locator locator) {
		}

		public void skippedEntity(String name) throws SAXException {
		}

		public void startDocument() throws SAXException {
		}

		public void startPrefixMapping(String prefix, String uri) throws SAXException {
		}
	}
}
