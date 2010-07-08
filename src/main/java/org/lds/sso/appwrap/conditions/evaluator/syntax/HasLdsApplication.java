package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lds.sso.appwrap.NvPair;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluatorContainer;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;

public class HasLdsApplication extends SyntaxBase implements IEvaluatorContainer {
    private String cfgValue;

	public void init(String syntax, Map<String,String> cfg) throws EvaluationException {
		super.init(syntax, cfg);
		String val = cfg.get("value");
		
		if (val != null && ! val.equals("")) {
			cfgValue = val;
		}
	}
	
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
		boolean debug = ctx.shouldLogResult(this);
		NvPair[] atts = ctx.user.getAttributes();
		if (atts.length == 0) {
			if (debug) {
				ctx.logResult(this, false, "user has no ldsApplications");
			}
			return false; 
		}
		
        for (NvPair att : atts) {
            if (User.LDSAPPS_ATT.equals(att.getName()) && cfgValue.equals(att.getValue())) {
                if (debug) {
                    ctx.logResult(this, true, "user has value " + cfgValue);
                }
                return true;
            }
        }
		if (debug) {
		    ctx.logResult(this, false, "user does not have the value");
		}
		return false;
	}

	/**
	 * Supports multiple ldsAccountId evaluation using nested ldsAccount pseudo-
	 * evaluators that aren't uesd as evaluators but are used solely to inject
	 * additional values into this class.
	 */
	public void addEvaluator(IEvaluator e) throws EvaluationException {
	    throw new EvaluationException("Illegal child '" + e.getClass().getSimpleName()
	            + "' of '" + this.getClass().getSimpleName() + "' in '"
	            + this.syntax + "'. " + this.getClass().getSimpleName()
	            + " does not accept nested children.");
	}

	/**
	 * Validates that this evaluator has what it needs to function.
	 */
	public void validate() throws EvaluationException {
		if (this.cfgValue == null) {
			throw new EvaluationException("Element '" 
				+ this.getClass().getSimpleName() + "' in '" + this.syntax 
				+ "' must have a 'value' attribute.");
		}
	}
}
