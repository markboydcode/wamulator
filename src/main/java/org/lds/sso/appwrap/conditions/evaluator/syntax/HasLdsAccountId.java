package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.ConvertToNoLdsAccountId;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluatorContainer;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;

/**
 * Evaluator that is no longer supported.
 * 
 * @author BoydMR
 *
 */
public class HasLdsAccountId extends SyntaxBase implements IEvaluatorContainer {
    public static final Logger cLog = Logger.getLogger(HasLdsAccountId.class);
    
	private List<String> cfgIds = new ArrayList<String>();
	private boolean cfgAny = false;

	public void init(String syntax, Map<String,String> cfg) throws EvaluationException {
	    throw new IllegalArgumentException("HasLdsAccountId is no longer supported. " 
	            + "Instead of running " + Service.class.getName() 
	            + ", execute " + ConvertToNoLdsAccountId.class.getName() 
	            + " with the same classapth and config file for steps " 
	            + "to convert your files."
	            );
	}
	public boolean isConditionSatisfied(EvaluationContext ctx) throws EvaluationException {
	    throw new UnsupportedOperationException("HasLdsAccountId is not supported.");
	}

	/**
	 * Supports multiple ldsAccountId evaluation using nested ldsAccount pseudo-
	 * evaluators that aren't uesd as evaluators but are used solely to inject
	 * additional values into this class.
	 */
	public void addEvaluator(IEvaluator e) throws EvaluationException {
        throw new UnsupportedOperationException("HasLdsAccountId is not supported.");
	}

	/**
	 * Validates that this evaluator has what it needs to function.
	 */
	public void validate() throws EvaluationException {
        throw new UnsupportedOperationException("HasLdsAccountId is not supported.");
	}
}
