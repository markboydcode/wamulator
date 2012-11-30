package org.lds.sso.appwrap.conditions.evaluator.syntax;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluatorContainer;

public class AllowDeny extends SyntaxBase implements IEvaluatorContainer {

	protected IEvaluator allowEvaluator = null;
	protected IEvaluator denyEvaluator = null;
	protected boolean allowTakesPrecedence = true;
	
	public AllowDeny(IEvaluator allow, IEvaluator deny, boolean allowTakesPrecedence) {
		this.allowEvaluator = allow;
		this.denyEvaluator = deny;
		this.allowTakesPrecedence = allowTakesPrecedence;
	}
	
	@Override
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
		boolean debug = ctx.shouldLogResult(this);
		if (debug) {
			ctx.beginLoggingSatisfiedChildEvaluators();
		}
		boolean isAllowed = false;
		
		if (denyEvaluator == null) {
			if (allowEvaluator.isConditionSatisfied(ctx)) {
				isAllowed = true;
			}
		} else if (allowEvaluator != null) {
			if (allowTakesPrecedence) {
				if (allowEvaluator.isConditionSatisfied(ctx)) {
					isAllowed = true;
				}
			} else {
				if (!denyEvaluator.isConditionSatisfied(ctx) &&
					allowEvaluator.isConditionSatisfied(ctx)) {
					isAllowed = true;
				}
			}
		}

		if (debug) {
			ctx.endLoggingOfChildren();
			ctx.logResult(this, isAllowed);
		}
		return isAllowed;
	}
	
	public void validate() throws EvaluationException {
		if (this.allowEvaluator == null && this.denyEvaluator == null) {
			throw new EvaluationException("Element '" 
				+ this.getClass().getSimpleName() + "' in '" + this.syntax 
				+ "' must have an allow or a deny Evaluator.");
		}
	}

	@Override
	public void addEvaluator(IEvaluator e) throws EvaluationException {
		// TODO Auto-generated method stub
		
	}
}
