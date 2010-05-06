package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.ArrayList;
import java.util.List;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluatorContainer;

/**
 * Evaluates contained {@link IEvaluator}s and returns false if it encounters
 * one that answers false for {@link IEvaluator#isConditionSatisfied(EvaluationContext)}.
 * If all return true then this evaluator evaluates to true thus implementing
 * a logical AND construct.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class AND extends SyntaxBase implements IEvaluatorContainer{

	protected List<IEvaluator> evaluators = new ArrayList<IEvaluator>();
	
	@Override
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
		boolean debug = ctx.shouldLogResult(this);
		if (debug) {
			ctx.beginLoggingUnsatisfiedChildEvaluators(); // only care who broke it
		}
		boolean isAllowed = true;
		
		for(IEvaluator evaluator : evaluators) {
			if (! evaluator.isConditionSatisfied(ctx)) {
				isAllowed = false;
				break;
			}
		}
		if (debug) {
			ctx.endLoggingOfChildren();
			ctx.logResult(this, isAllowed);
		}
		return isAllowed;
	}

	public void addEvaluator(IEvaluator e) throws EvaluationException {
		evaluators.add(e);
	}

	public void validate() throws EvaluationException {
		if (this.evaluators.size() == 0) {
			throw new EvaluationException("Element '" 
				+ this.getClass().getSimpleName() + "' in '" + this.syntax 
				+ "' must have one or more nested elements.");
		}
	}

}
