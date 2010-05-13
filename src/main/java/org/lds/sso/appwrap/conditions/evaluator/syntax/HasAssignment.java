package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluatorContainer;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;

/**
 * Evaluator implementing three equivalent syntaxes like so:
 * 
 * <HasAssignment position='4' unit='444'/>
 * 
 * OR
 * 
 * <HasAssignment>
 *  <Assignment position='4' unit='444'/>
 * </HasAssignment>
 * 
 * OR
 * 
 * <HasAssignment position='4' unit='444'>
 *  <Assignment position='4' unit='444'/>
 * </HasAssignment>
 * 
 * 
 * An unlimited set of nested Assignment elements can be supported. The method,
 * isConditionSatisfied returns true if the user has any of the configured
 * positions. There is no priority given to an assignment specified as
 * attributes of this element or as nested Assignment elements. If two or more 
 * assignments are the same then only one is kept for evaluation. The Assignment
 * element is implemented by the Assignment evaluator type. 
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class HasAssignment extends SyntaxBase implements IEvaluatorContainer {
	public static final String[] NO_POSITIONS = new String[] {};
	protected List<AssignmentValues> cfgVals = new ArrayList<AssignmentValues>();
    
	@Override
	public void init(String syntax, Map<String, String> cfg) throws EvaluationException {
		super.init(syntax, cfg);
		String pos = cfg.get("position");
		String unit = cfg.get("unit");
		
		if (pos != null && ! pos.equals("") &&
				unit != null && ! unit.equals("")) {
			addCfgVal(pos, unit);
		}
	}

	/**
	 * Called at policy evaluation time to see if the user represented by the 
	 * passed-in context meets the conditions of this evaluator.
	 */
	@Override
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
		boolean debug = ctx.shouldLogResult(this);
		String positions = ctx.user.getProperty(UserHeaderNames.POSITIONS);

		if (positions == null) {
			if (debug) {
				ctx.logResult(this, false, "assignments not in session");
			}
			return false;
		}
		if (positions == UserHeaderNames.EMPTY_VALUE_INDICATOR) {
			if (debug) {
				ctx.logResult(this, false, "user does not have any assignments");
			}
			return false;
		}
		
			for (AssignmentValues cfgVal : cfgVals) {
				if (cfgVal.pattern.matcher(positions).matches()) {
					if (debug) {
						ctx.logResult(this, true, "user has assignment "
								+ cfgVal.rawPos + " in unit " + cfgVal.rawUnit);
					}
					return true;
				}
			}
		if (debug) {
			if (cfgVals.size() == 1) {
				ctx.logResult(this, false, "user does not have the assignment");
			}
			else {
				ctx.logResult(this, false, "user has none of the assignments");
			}
		}
		return false;
	}

	/**
	 * Supports multiple assignment evaluatoin using nested assignment pseudo-
	 * evaluators that aren't uesd as evaluators but are used solely to inject
	 * additional values into this class.
	 */
	public void addEvaluator(IEvaluator e) throws EvaluationException {
		if (! (e instanceof Assignment)) {
			throw new EvaluationException("Illegal child '" + e.getClass().getSimpleName()
					+ "' of '" + this.getClass().getSimpleName() + "' in '"
					+ this.syntax + "'. " + this.getClass().getSimpleName()
					+ " only accepts nested children of type " 
					+ Assignment.class.getSimpleName() + ".");
		}
		Assignment assig = (Assignment) e;
		addCfgVal(assig.getPosition(), assig.getUnit());
	}
	
	/**
	 * Add value making sure that we don't already check for that combination.
	 * 
	 * @param position
	 * @param unit
	 */
	public void addCfgVal(String position, String unit) {
		AssignmentValues cfgVal = new AssignmentValues(position, unit);

		if (! cfgVals.contains(cfgVal)) {
			cfgVals.add(cfgVal);
		}
	}
	
	/**
	 * Class used to hold configured assignments looked for in user's sessions
	 * when evaluating policies. Overrides equals and hashcode to use the 
	 * implementation available for id and have a hashcode of 0 if id is null. 
	 * 
	 * @author Mark Boyd
	 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
	 *
	 */
	protected static class AssignmentValues {
		public Pattern pattern;
        public String position = null;
		public String unitContains = null;
		public String rawUnit = null;
		public String rawPos = null;
		public String id = null;
	 	
		public AssignmentValues(String position, String unit) {
			this.id = position + UserHeaderNames.UNITS_DELIMITER + unit;
			this.rawPos = position;
			this.rawUnit = unit;

			/*
			 * Create a pattern that will match against a potentially multivalued
			 * position header like:
			 * 
			 * p4/7u111/5u222/1u333/:p23/7u444/5u555/1u666/1016u777/
			 * 
			 * This should match on a bishop in unit 333 but no in unit 777.
			 * 
			 */
			this.pattern = Pattern.compile(".*p" + rawPos + "/[^:]*u" + rawUnit + "/.*");
		}

		@Override
		public boolean equals(Object obj) {
			boolean rightClass = obj instanceof AssignmentValues;
			if (!rightClass) {
				return false;
			}
			AssignmentValues obja = (AssignmentValues) obj;
			String oid = obja.id;
			
			return (id == null && oid == null) 
				|| (id != null && oid != null && id.equals(oid));
		}

		@Override
		public int hashCode() {
			return (id == null ? 0 : id.hashCode());
		}
	}

	/**
	 * Validates that this evaluator has what it needs to function.
	 */
	public void validate() throws EvaluationException {
		if (this.cfgVals.size() == 0) {
			throw new EvaluationException("Element '" 
				+ this.getClass().getSimpleName() + "' in '" + this.syntax 
				+ "' must have attribute postion and unit or have one or " 
				+ "more nested Assignment elements with position and unit");
		}
	}
}
