package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.PrintWriter;
import java.security.Principal;
import java.util.Map;

import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;

public abstract class SyntaxBase
    implements IEvaluator
{

    protected String syntax = null;
	private boolean debug = false;
	private boolean debugUser = false;
	private String username = null;
	private Map<String, String> elementAtts;

    public SyntaxBase()
    {
        syntax = null;
    }

    public String getSyntax()
    {
        return syntax;
    }

    /**
     * Stores syntax and evaluates if debugging is turned on for the evaluator.
     */
    public void init(String syntax, Map<String, String> cfg)
        throws EvaluationException
    {
        this.syntax = syntax;
        this.elementAtts = cfg;
        if (cfg.get(DEBUG) != null) {
        	debug = true;
        }
        if (cfg.get(DEBUG_USER) != null) {
        	debug = false;
        	debugUser = true;
        	username = cfg.get(DEBUG_USER);
        }
    }

    /**
     * Utility method for logging start tag responsible for instantiation
     * of this evaluator.
     * @param log
     */
    public void logElementStartTag(PrintWriter log, boolean hasChildren) {
    	log.print("<");
    	log.print(getClass().getSimpleName());
    	for (Map.Entry<String, String> e : elementAtts.entrySet()) {
    		log.print(" ");
    		log.print(e.getKey());
    		log.print("='");
    		log.print(e.getValue());
    		log.print("'");
    	}
    	if (!hasChildren) {
    		log.print("/>"); // not println so we can add stuff after.
    	}
    	else
    	{
        	log.print(">"); // not println so we can add stuff after.
    	}
    }
    
    /**
	 * Utility method for logging end tag of the element responsible for
	 * instantiation of this evaluator.
	 * @param log
	 */
    public void logElementEndTag(PrintWriter log) {
    	log.print("</");
    	log.print(getClass().getSimpleName());
    	log.print(">");
    }

    /**
     * Returns true if the syntax representative of this evaluator had an 
     * attribute of debug.
     * 
     * @return
     */
    public boolean isDebugging() {
    	return debug;
    }
    
    /**
     * Returns true if the syntax representative of this evaluator had an 
     * attribute of debugUser.
     * 
     * @return
     */
    public boolean isDebuggingUser() {
    	return debugUser;
    }
    
    /**
     * Returns the value of the debugUser attribute of the syntax representative
     * of this evaluator. The value is meant to be the id used by the user to
     * authenticate and allows debugging to be turned on for a single user.
     * 
     * @return
     */
    public String getDebugUserName() {
    	return username;
    }
    
    /**
     * Convenience method for obtaining the value of a required attribute on 
     * the syntax of this evaluator or throwing a consistent message if such 
     * an attribute was not found.
     * 
     * @param name
     * @param cfg
     * @return
     * @throws EvaluationException
     */
    public String getRequired(String name, Map cfg)
        throws EvaluationException
    {
        String val = (String)cfg.get(name);
        if(val == null)
            throw new EvaluationException(
            		"Attribute '" + name + "' must be defined for <"
            		+ getClass().getSimpleName() + "> in " + syntax);
        else
            return val;
    }

    public abstract boolean isConditionSatisfied(EvaluationContext evaluationcontext)
        throws EvaluationException;
}