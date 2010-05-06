package org.lds.sso.appwrap.conditions.evaluator;

import java.util.Map;

/**
 * Interface implemented by evaluators whose xml syntax supports nested xml elements
 * and thence evaluators that can contain other evaluators.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public interface IEvaluatorContainer extends IEvaluator {
	
	/**
	 * Adds a nested evaluator to this container.
	 * @param e
	 */
	public void addEvaluator(IEvaluator e) throws EvaluationException;
	
	/**
	 * Called after all nested elements have been processed allowing this 
	 * container to indicate if it is valid or not. Throws an exception if
	 * the container can not function like an empty <AND/> syntax element for
	 * example would throw such an exception with a message indicating the 
	 * problem.
	 * 
	 * @throws EvaluationException
	 */
	public void validate() throws EvaluationException;
}
