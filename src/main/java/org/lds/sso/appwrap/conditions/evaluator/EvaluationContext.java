package org.lds.sso.appwrap.conditions.evaluator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lds.sso.appwrap.conditions.evaluator.syntax.SyntaxBase;
import org.lds.sso.appwrap.identity.User;
import org.lds.sso.appwrap.io.LogUtils;

/**
 * Context passed to evaluators during evaluation of their condition for a given
 * user.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class EvaluationContext {
	public static final String DEBUG_LOG_NAME = LogicalSyntaxEvaluationEngine.class.getName() + ".syntax-log";

	public EvaluationContext() {
		
	}
	
	public EvaluationContext(User user, Map env) {
		this.user = user;
        this.env = env;
		if (env == null) {
		    this.env = new HashMap<String,String>();
		}
		else {
	        this.env = env;
		}
	}
	
	public User user = null;
	public Map env = null;

	/**
	 * Indicates if an evaluator should log debug info.
	 */
	public boolean logging = false;
	private LogHolder logUtils = null;
	private List<LogHolder> parentLogUtils = null;
	private SyntaxBase debugTrigger;
	private Level debugOriginalLevel;
	
	/**
	 * Used by evaluators in their isConditionSatisfied method to determine if 
	 * the evaluator should log its outcome along with a descriptive message by
	 * calling {@link #logResult(SyntaxBase, boolean, String)}.
	 * 
	 * @param evaluator
	 * @return
	 */
	public boolean shouldLogResult(SyntaxBase ev) {
		// if already logging then don't look to current evaluator's config
		if (this.logUtils  != null) {
			return true;
		}
		// see if current evaluator specifies debug
		if (ev.isDebugging()) {
			setUpLogging(ev);
			return true;
		}
        // see if current evaluator specifies debug user
		String debugNm = ev.getDebugUserName(); 
        if (debugNm != null && debugNm.equalsIgnoreCase(user.getUsername())) {
            setUpLogging(ev);
            return true;
        }
        return false;
	}
	
	/**
	 * Sets up the tools for logging the current element representing the 
	 * current evaluator.
	 * @param ev
	 */
	private void setUpLogging(SyntaxBase ev) {
		logUtils = new LogHolder("");
		parentLogUtils = new ArrayList<LogHolder>();
		Logger ossoLog = Logger.getLogger(DEBUG_LOG_NAME);
		debugOriginalLevel = ossoLog.getLevel();
		ossoLog.setLevel(Level.FINE);
		this.debugTrigger = ev; // so we know when to turn it back off
	}
	
	public void beginLoggingUnsatisfiedChildEvaluators() {
		parentLogUtils.add(logUtils);
		logUtils = new LogHolder(logUtils.indent);
		logUtils.logNotSatisfied();
	}
	
	public void beginLoggingSatisfiedChildEvaluators() {
		parentLogUtils.add(logUtils);
		logUtils = new LogHolder(logUtils.indent);
		logUtils.logSatisfied();
	}
	
	public void beginLoggingEitherChildEvaluatorOutcome() {
		parentLogUtils.add(logUtils);
		logUtils = new LogHolder(logUtils.indent);
		logUtils.logBothOutcomes();
	}
	
	public void endLoggingOfChildren() {
		logUtils.log.flush();
		String childrenOutcome = logUtils.buffer.toString();
		logUtils = parentLogUtils.remove(parentLogUtils.size()-1);
		logUtils.nestedOutcomes = childrenOutcome;
	}
	
	/**
	 * Used by evaluators to log the result of their evaulation prior to returning 
	 * from {@link IEvaluator#isConditionSatisfied(EvaluationContext) but without
	 * a message to append to the end of the start element tag line representing this
	 * evaluator.
	 * 
	 * Example: Logical combinators AND, OR, and NOT don't need a message while
	 * attribute evaluators generally would. The NOT evaluator would not pass a
	 * message while the HasPosition evaluator would resulting in this debug
	 * output.
	 * 
	 * <code>
	 * f  <NOT debug-user='ngienglishbishop'>  
	 * t    <HasPosition username='ngienglishbishop' id='4'/>  user has position P4:
	 *    </NOT>
     * </code>
	 * 
	 * @param evaluator
	 * @param b
	 * @param string
	 */
	public void logResult(SyntaxBase evaluator, boolean outcome) {
		logResult(evaluator, outcome, (String) null);
	}
	
	/**
	 * Used by evaluators to log the result of their evaulation prior to returning 
	 * from {@link IEvaluator#isConditionSatisfied(EvaluationContext) including
	 * a message to append to the end of the start element tag line representing this
	 * evaluator.
	 * 
	 * Example: Logical combinators AND, OR, and NOT don't need a message while
	 * attribute evaluators generally would. The HasPosition evaluator would 
	 * pass a message while the NOT evaluator would not resulting in this debug
	 * output.
	 * 
	 * <code>
	 * f  <NOT debug-user='ngienglishbishop'>  
	 * t    <HasPosition username='ngienglishbishop' id='4'/>  user has position P4:
	 *    </NOT>
     * </code>
     * 
	 * @param evaluator
	 * @param b
	 * @param string
	 */
	public void logResult(SyntaxBase evaluator, boolean outcome, String message) {
		if (logUtils.isLoggingBoth() ||
				(logUtils.isLoggingSatisfied() && outcome == true) ||
				(logUtils.isLoggingSatisfied() == false && outcome == false) ||
				debugTrigger == evaluator) {
			logUtils.log.print((outcome ? "T" : "F"));
			logUtils.log.print(logUtils.indent);
			evaluator.logElementStartTag(logUtils.log, logUtils.nestedOutcomes != null);

			if (message != null && ! message.equals("")) {
				logUtils.log.print("  ");
				logUtils.log.print(message);
			}
			logUtils.log.println();
			
			if (logUtils.nestedOutcomes != null) {
				logUtils.log.print(logUtils.nestedOutcomes);
				// print line with end element tag
				logUtils.log.print(" ");
				logUtils.log.print(logUtils.indent);
				evaluator.logElementEndTag(logUtils.log);
				logUtils.log.println();
			}
		}
		
		// now see if we are back at the evaluator that triggered start of logging
		if (debugTrigger == evaluator) {
			// we are, dump log to file and stop logging
			Logger ossoLog = Logger.getLogger(DEBUG_LOG_NAME);
			logUtils.log.println("----- env -----");
			for( Iterator itr = env.entrySet().iterator(); itr.hasNext();) {
				Map.Entry e = (Entry) itr.next();
				logUtils.log.print(e.getKey());
				logUtils.log.print(" = ");
				logUtils.log.println(e.getValue().toString());
			}
			logUtils.log.println("---------------");
			logUtils.log.flush();
			LogUtils.fine(ossoLog, logUtils.buffer.toString());
			ossoLog.setLevel(debugOriginalLevel);
			logUtils = null;
			debugTrigger = null;
			parentLogUtils = null;
		}
	}
	
	/**
	 * Holds articles needed for logging outcomes.
	 * 
	 * @author Mark Boyd
	 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
	 *
	 */
	private static class LogHolder {
		StringWriter buffer = null;
		PrintWriter log = null;
		String indent = null;
		boolean shouldLogSatisfied = true;
		boolean shouldLogBoth = true;
		private String nestedOutcomes = null;
		
		LogHolder(String indentation){
			indent = indentation + "  ";
			buffer = new StringWriter();
			log = new PrintWriter(buffer);
		}
		
		public boolean isLoggingBoth() {
			return shouldLogBoth;
		}

		/**
		 * Indicates that only conditions returning false from 
		 * {@link IEvaluator#isConditionSatisfied(EvaluationContext)} should 
		 * be included in debug output. 
		 */
		void logNotSatisfied() {
			shouldLogSatisfied = false;
			shouldLogBoth = false;
		}
		
		/**
		 * Indicates that only conditions returning true from 
		 * {@link IEvaluator#isConditionSatisfied(EvaluationContext)} should 
		 * be included in debug output. 
		 */
		void logSatisfied() {
			shouldLogSatisfied = true;
			shouldLogBoth = false;
		}
		
		/**
		 * Indicates that both positive and negative outcomes should be logged. Example,
		 * An AND evaluator with a nested NOT around evaluator E wants to show
		 * if the NOT returned false and show E's affirmative outcome. 
		 * An OR evaluator with a nested NOT around evaluator E wants to show
		 * if the NOT returned true and show E's negative outcome. So when NOT
		 * is logging it wants both affirmative and negative responses of its
		 * nested element logged.
		 * @return
		 */
		void logBothOutcomes() {
			shouldLogBoth = true;
		}
		
		void dontLogBothOutcomes() {
			shouldLogBoth = false;
		}
		
		/**
		 * Indicates if positive or negative outcomes should be logged. Example,
		 * an AND evaluator doesn't care who was true for debug purposes. The
		 * killer is the nested evaluator that returned false. So an AND
		 * container will tell logging of its children to only include the one
		 * that returned false. An OR container would do the opposite. The
		 * top-most evaluator will, by default, log the outcome of
		 * {@link IEvaluator#isConditionSatisfied(EvaluationContext)} regardless
		 * of its value. The default is to log conditions returning true.
		 * @return
		 */
		boolean isLoggingSatisfied() {
			return shouldLogBoth || shouldLogSatisfied;
		}
	}
}
