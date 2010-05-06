package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluatorContainer;
import org.lds.sso.appwrap.conditions.evaluator.LegacyPropsInjector;

public class HasLdsAccountId extends SyntaxBase implements IEvaluatorContainer {
	private List<String> cfgIds = new ArrayList<String>();
	private boolean cfgAny = false;

	public void init(String syntax, Map<String,String> cfg) throws EvaluationException {
		super.init(syntax, cfg);
		String id = cfg.get("id");
		
		if (id != null && ! id.equals("")) {
			addCfgId(id);
		}
	}
	
	public void addCfgId(String id) {
		if (id.equals("*")) {
			cfgAny = true;
		}
		else if (! cfgIds.contains(id)) {
			cfgIds.add(id);
		}
	}

	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
		boolean debug = ctx.shouldLogResult(this);
		String id = super.getSessionValue(LegacyPropsInjector.CP_LDS_ACCOUNT_ID_PROPERTY, 
				ctx.user);
		if (id == null) {
			if (debug) {
				ctx.logResult(this, false, "user id not in session");
			}
			return false; 
		}
		if (LegacyPropsInjector.EMPTY_VALUE_INDICATOR.equals(id)) {
			if (debug) {
				ctx.logResult(this, false, "user id = empty");
			}
			return false;
		}
		
		// so at this point the user has an ldsaccountid so if the 'any' condition
		// is specified we are done
		if (cfgAny) {
			if (debug) {
				ctx.logResult(this, true, "user has an id");
			}
			return true;
		}
		else { // see if any specified IDs match that of the user
			for(String cfgId : cfgIds) {
				if (cfgId.equals(id)) {
					if (debug) {
						ctx.logResult(this, true, "user has the id " + cfgId);
					}
					return true;
				}
			}
		}
		if (debug) {
			if (cfgIds.size() == 1) {
				ctx.logResult(this, false, "user does not have the id");
			}
			else {
				ctx.logResult(this, false, "user does not have any of the ids");
			}
		}
		return false;
	}

	/**
	 * Supports multiple ldsAccountId evaluation using nested ldsAccount pseudo-
	 * evaluators that aren't uesd as evaluators but are used solely to inject
	 * additional values into this class.
	 */
	public void addEvaluator(IEvaluator e) throws EvaluationException {
		if (! (e instanceof LdsAccount)) {
			throw new EvaluationException("Illegal child '" + e.getClass().getSimpleName()
					+ "' of '" + this.getClass().getSimpleName() + "' in '"
					+ this.syntax + "'. " + this.getClass().getSimpleName()
					+ " only accepts nested children of type " 
					+ LdsAccount.class.getSimpleName() + ".");
		}
		LdsAccount acct = (LdsAccount) e;
		addCfgId(acct.getId());
	}

	/**
	 * Validates that this evaluator has what it needs to function.
	 */
	public void validate() throws EvaluationException {
		if (!this.cfgAny && this.cfgIds.size() == 0) {
			throw new EvaluationException("Element '" 
				+ this.getClass().getSimpleName() + "' in '" + this.syntax 
				+ "' must have attribute id or have one or " 
				+ "more nested " + LdsAccount.class.getSimpleName() 
				+ " elements with an id.");
		}
	}
}
