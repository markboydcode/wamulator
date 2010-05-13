package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluatorContainer;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;

public class HasPosition extends SyntaxBase implements IEvaluatorContainer {
	private List<String> cfgPositions = new ArrayList<String>();

	/**
	 * Initialize with attributes, if any, on the element represented by this
	 * class.
	 */
	@Override
	public void init(String syntax, Map<String, String> cfg) throws EvaluationException {
		super.init(syntax, cfg);
		String id = cfg.get("id");
		
		if (id != null && ! id.equals("")) {
			addCfgPos(id);
		}
	}
	
	/**
	 * Adds a position to the set of positons to be tested for in user's sessions.
	 * @param pos
	 */
	private void addCfgPos(String pos) {
		String cfgPos = UserHeaderNames.POSITION_HEADER_PREFIX + pos
		+ UserHeaderNames.UNITS_DELIMITER;
		if (! cfgPositions.contains(cfgPos)) {
			cfgPositions.add(cfgPos);
		}
	}
    
	@Override
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
		boolean debug = ctx.shouldLogResult(this);
		String positions = super.getSessionValue(UserHeaderNames.POSITIONS, 
				ctx.user);
		if (positions == null) {
			if (debug) {
				ctx.logResult(this, false, "positions not in session");
			}
			return false;
		}
		
		for(String cfgPos : cfgPositions ) {
			if (positions.contains(cfgPos)) {
				if (debug) {
					ctx.logResult(this, true, "user has position " + cfgPos.substring(1, cfgPos.length()-1));
				}
				return true;
			}
		}
		if (debug) {
			if (cfgPositions.size() == 1) {
				ctx.logResult(this, false, "user does not have position");
			}
			else {
				ctx.logResult(this, false, "user has none of the positions");
			}
		}
		return false;
	}

	/**
	 * Supports multiple position evaluation using nested position pseudo-
	 * evaluators that aren't uesd as evaluators but are used solely to inject
	 * additional values into this class.
	 */
	public void addEvaluator(IEvaluator e) throws EvaluationException {
		if (! (e instanceof Position)) {
			throw new EvaluationException("Illegal child '" + e.getClass().getSimpleName()
					+ "' of '" + this.getClass().getSimpleName() + "' in '"
					+ this.syntax + "'. " + this.getClass().getSimpleName()
					+ " only accepts nested children of type " 
					+ Position.class.getSimpleName() + ".");
		}
		Position pos = (Position) e;
		addCfgPos(pos.getId());
	}

	/**
	 * Validates that this evaluator has what it needs to function.
	 */
	public void validate() throws EvaluationException {
		if (this.cfgPositions.size() == 0) {
			throw new EvaluationException("Element '" 
				+ this.getClass().getSimpleName() + "' in '" + this.syntax 
				+ "' must have attribute id or have one or " 
				+ "more nested " + Position.class.getSimpleName() 
				+ " elements with an id.");
		}
	}
}
