package org.lds.sso.appwrap.conditions.evaluator;

import java.util.Map;

/**
 * Interface implemented by evaluators to participate in condition evaluation.
 * Implementations provide the semantics of the language supported. Evaluators
 * MUST be thread safe meaning they MUST support multi-threaded operation.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public interface IEvaluator {

	/**
	 * Attribute name that can be placed on any top level element the causes its
	 * evaluator instance and any contained evaluators to log their decision outcome.
	 */
	public static final String DEBUG = "debug";

	/**
	 * Attribute name that can be placed on any top level element the causes its
	 * evaluator instance and any contained evaluators to log their decision outcome
	 * if the username of the user matches the value of this attribute.
	 */
	public static final String DEBUG_USER = "debug-user";

	/**
	 * Method called at condition evaluation time to ascertain if a given user
	 * fulfills the requirements of the condition.
	 * 
	 * @param ctx
	 * @return
	 * @throws EvaluationException
	 */
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException;
	
	/**
	 * Method called immediately after instantiation of an evaluator instance
	 * but before any calls to isConditionSatisfied. Values passed are attributes
	 * of the xml element representing this evaluator and implementing the xml 
	 * based configuration language.
	 * 
	 * @param cfg
	 * @throws EvaluationException
	 */
	public void init(String syntax, Map<String,String> cfg) throws EvaluationException;
	
	/**
	 * Returns the syntax String passed to the init method.
	 * 
	 * @return
	 */
	public String getSyntax();
}
