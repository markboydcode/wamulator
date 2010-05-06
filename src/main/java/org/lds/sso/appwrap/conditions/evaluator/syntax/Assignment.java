package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.Map;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;

/**
 * Implements ability to declare nested assignment elements that a parent 
 * element can pull in and use for evaluation of its condition.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class Assignment extends SyntaxBase {
	
	private String position;
	private String unit;

	@Override
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
		throw new EvaluationException("Should never be called. Evaluator used " +
				"only to inject values into parent.");
	}

	@Override
	public void init(String syntax, Map<String, String> cfg) throws EvaluationException {
		super.init(syntax, cfg);
		position = super.getRequired("position", cfg);
		unit = super.getRequired("unit", cfg);
	}
	
	public String getPosition() {
		return position;
	}
	
	public String getUnit() {
		return unit;
	}
}
