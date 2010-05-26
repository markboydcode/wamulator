package org.lds.sso.appwrap.conditions.evaluator.syntax;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;

public class IsMember extends SyntaxBase {

	@Override
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
		boolean debug = ctx.shouldLogResult(this);
		String mrn = ctx.user.getProperty(UserHeaderNames.LDS_MRN);
		
		if (mrn == null) {
			if (debug) {
				ctx.logResult(this, false, "mrn not in session");
			}
			return false;
		}
		boolean isMem = ! mrn.equals(UserHeaderNames.EMPTY_VALUE_INDICATOR);

		if (debug) {
			ctx.logResult(this, isMem, (isMem ? "user has an mrn"
					: "user does not have an mrn"));
		}
		return isMem;
	}
}
