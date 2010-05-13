package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluatorContainer;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;

/**
 * Implements support for evaluating if users are members of a unit or if they 
 * have an assignment in that unit or a contained unit.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class MemberOfUnit extends SyntaxBase implements IEvaluatorContainer {
    protected List<Values> cfgUnits = new ArrayList<Values>();
    
	@Override
	public void init(String syntax, Map<String, String> cfg) throws EvaluationException {
		super.init(syntax, cfg);
		String id = cfg.get("id");
		
		if (id != null && ! id.equals("")) {
			addUnit(id);
		}
	}
    
	protected void addUnit(String unitId) {
		Values vals = new Values(unitId);
		if (!cfgUnits.contains(vals)) {
			cfgUnits.add(vals);
		}

	}

	@Override
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
		boolean debug = ctx.shouldLogResult(this);
		String units = super.getSessionValue(UserHeaderNames.UNITS, 
				ctx.user);
		if (units == null) {
			if (debug) {
				ctx.logResult(this, false, "units not in session");
			}
			return false;
		}
		if (units == UserHeaderNames.EMPTY_VALUE_INDICATOR) {
			if (debug) {
				ctx.logResult(this, false, "user is not a member of any unit");
			}
			return false;
		}
		
		/*
		 * Units pattern is /#u###/#u###/#u###/ with variable length numeric
		 * characters where #### is a numeric id for the unit and the change is
		 * as long as needed to convey the hierarchy.
		 */
		for(Values cfgUnit : cfgUnits) {
			if (units.contains(cfgUnit.unitPattern)) {
				if (debug) {
					ctx.logResult(this, true, "user is member of unit " + cfgUnit.rawUnit);
				}
				return true;
			}
		}
		/*
		 * If we get to this point the user is not a member of the unit. But,
		 * the user's assignments must also be consulted for access to the unit
		 * to accommodate assignments in units other than the ones in which a
		 * user lives. Student ward or inner city missions are examples.
		 * 
		 * To determine if a
		 * user has an assignment in a unit we look for contains u###/
		 * using the unitInPattern variable of the Values object.
		 */
		String unitsOfAssignment = null;
		unitsOfAssignment = ctx.user.getProperty(UserHeaderNames.POSITIONS);
		
		if (unitsOfAssignment != null && !unitsOfAssignment.equals(UserHeaderNames.EMPTY_VALUE_INDICATOR)) {
			for (Values cfgUnit : cfgUnits) {
				if (unitsOfAssignment.contains(cfgUnit.unitPattern)) {
					if (debug) {
						ctx.logResult(this, true, "user is not a member of unit " + cfgUnit.rawUnit
								+ " but has assignment in unit.");
					}
					return true;
				}
			}
		}

		if (debug) {
			if (cfgUnits.size() == 1) {
				ctx.logResult(this, false, "user is not a member of nor has assignment in " 
						+ cfgUnits.get(0).rawUnit);
			}
			else {
				ctx.logResult(this, false, "user is not a member of nor has assignments in any configured units");
			}
		}
		return false;
	}
	
	/**
	 * Holds patterns searched for in the user's units session value to 
	 * determine if they are a member of that unit whether it be their immediate
	 * unit or a containing unit. Overrides equals and hashcode to leverage 
	 * that of the unitAtEnd variable and return 0 for hashcode if unitAtEnd is
	 * null.
	 * 
	 * @author Mark Boyd
	 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
	 *
	 */
	public static class Values {
	    public String unitPattern = null;
		public String rawUnit = null;

	    public Values(String unitId) {
			// units pattern is '/' + # + 'u' + #### + "/" always in headers 
	        // where the first number if the unit type id and the second is the
	        // unit number.
	    	rawUnit = unitId;
			unitPattern = UserHeaderNames.UNIT_PREFIX + unitId 
				+ UserHeaderNames.UNITS_DELIMITER;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Values)) {
				return false;
			}
			Values objv = (Values) obj;
			String ouae = objv.unitPattern;
			return (unitPattern == null && objv.unitPattern == null) 
				|| (unitPattern != null && unitPattern.equals(objv.unitPattern));
		}

		@Override
		public int hashCode() {
			if (unitPattern == null) {
				return 0;
			}
			return unitPattern.hashCode();
		}
		
	}

	/**
	 * Supports multiple unit evaluation using nested unit pseudo-
	 * evaluators that aren't uesd as evaluators but are used solely to inject
	 * additional values into this class.
	 */
	public void addEvaluator(IEvaluator e) throws EvaluationException {
		if (! (e instanceof Unit)) {
			throw new EvaluationException("Illegal child '" + e.getClass().getSimpleName()
					+ "' of '" + this.getClass().getSimpleName() + "' in '"
					+ this.syntax + "'. " + this.getClass().getSimpleName()
					+ " only accepts nested children of type " 
					+ Unit.class.getSimpleName() + ".");
		}
		Unit unit = (Unit) e;
		addUnit(unit.getId());
	}

	/**
	 * Validates that this evaluator has what it needs to function.
	 */
	public void validate() throws EvaluationException {
		if (this.cfgUnits.size() == 0) {
			throw new EvaluationException("Element '" 
				+ this.getClass().getSimpleName() + "' in '" + this.syntax 
				+ "' must have attribute id or have one or " 
				+ "more nested " + Unit.class.getSimpleName() 
				+ " elements with an id.");
		}
	}
}
