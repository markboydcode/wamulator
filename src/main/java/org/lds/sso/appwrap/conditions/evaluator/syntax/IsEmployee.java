package org.lds.sso.appwrap.conditions.evaluator.syntax;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;

public class IsEmployee extends SyntaxBase {

	@Override
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
		boolean debug = ctx.shouldLogResult(this);
		String dn = ctx.user.getProperty(UserHeaderNames.DN);
		if (dn == null) {
			if (debug) {
				ctx.logResult(this, false, "dn not in session");
			}
			return false;
		}
		boolean isEmp = dn.toLowerCase().contains("ou=int");

		if (debug) {
			ctx.logResult(this, isEmp, (isEmp ? "user has ou=int in their dn"
					: "user does not have ou=int in their dn"));
		}
		return isEmp;
	}
}
