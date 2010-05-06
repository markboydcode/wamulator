package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.Map;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;

/**
 * Pseudo evaluator used as child elements of HasLdsAccountId solely to inject 
 * additional IDs for evaluation.
 *  
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class LdsAccount extends SyntaxBase {

	
	private String id;

	@Override
	public void init(String syntax, Map<String, String> cfg) throws EvaluationException {
		super.init(syntax, cfg);
		this.id = super.getRequired("id", cfg);
	}

	@Override
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
		throw new EvaluationException("Should never be called. Evaluator used " +
				"only to inject values into parent.");
	}
	
	public String getId() {
		return id;
	}
}
