package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.ArrayList;
import java.util.List;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluatorContainer;

/**
 * Evaluates a single contained {@link IEvaluator} returning true if the
 * contained {@link IEvaluator} returns false for
 * {@link IEvaluator#isConditionSatisfied(EvaluationContext)}. Returns false if
 * the contained {@link IEvaluator} returns true thus implementing a logical NOT
 * construct.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class NOT extends SyntaxBase implements IEvaluatorContainer{

	protected IEvaluator evaluator = null;
	
	@Override
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
		boolean debug = ctx.shouldLogResult(this);
		boolean isAllowed = false;
		
		if (debug) {
			ctx.beginLoggingEitherChildEvaluatorOutcome();
			isAllowed = ! evaluator.isConditionSatisfied(ctx);
			ctx.endLoggingOfChildren();
			ctx.logResult(this, isAllowed);
		}
		else {
			isAllowed = ! evaluator.isConditionSatisfied(ctx);
		}
		return isAllowed;
	}

	public void addEvaluator(IEvaluator e) throws EvaluationException {
		if (evaluator != null) {
			throw new EvaluationException("Element '" 
					+ this.getClass().getSimpleName() + "' in '" + this.syntax 
					+ "' only accepts a single nested element.");
		}
		evaluator = e;
	}

	public void validate() throws EvaluationException {
		if (evaluator == null) {
			throw new EvaluationException("Element '" 
				+ this.getClass().getSimpleName() + "' in '" + this.syntax 
				+ "' must have a nested element.");
		}
	}

}
