package org.lds.sso.appwrap.conditions.evaluator.syntax;

import org.lds.sso.appwrap.conditions.evaluator.IEvaluatorContainer;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.apache.tools.ant.util.StringUtils;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

/**
 * User: NEWEYCR
 * Date: May 11, 2010
 * Time: 12:16:22 PM
 */
public class CtxMatches extends SyntaxBase implements IEvaluatorContainer {
	public static final String ATTR_HEADER		= "header";
	public static final String ATTR_REGEX		= "regex";
	// supported children tags
	public static final String CLASS_ASSIGNMENT	= "Assignment";
	public static final String CLASS_POSITION	= "Position";
	public static final String CLASS_UNIT		= "Unit";
	public static final String[] SUPPORTED_CHILDREN = {CLASS_ASSIGNMENT, CLASS_POSITION, CLASS_UNIT };

	private String regex;
	private String header;
	private List<String> aliases = new ArrayList<String>();
	private List<IEvaluator> evaluators = new ArrayList<IEvaluator>();

	@Override
	public void init(String syntax, Map<String, String> cfg) throws EvaluationException {
		super.init(syntax, cfg);
		regex = cfg.get(ATTR_REGEX);
		header = cfg.get(ATTR_HEADER);
		if (null != regex) {
			regex = parseAliases();
		}
	}

	protected String parseAliases() {
		StringBuilder buffer = new StringBuilder();
		int idxBegin, idxEnd = 0;
		do {
			idxBegin = regex.indexOf("{$", idxEnd);
			if (idxBegin >= 0) {
				buffer.append(regex.substring(idxEnd, idxBegin));
				buffer.append("{").append(aliases.size()).append("}");
				idxEnd = regex.indexOf("$}", idxBegin);
				if (idxEnd > 0) {
					aliases.add(regex.substring(idxBegin + 2, idxEnd));
					idxEnd += 2;
				}
			}
		} while (idxBegin >= 0 && idxEnd >= 0);
		// copy over anything left over after the last alias
		if (idxEnd >= 0) {
			buffer.append(regex.substring(idxEnd));
		}
		return buffer.toString();
	}

	public boolean isConditionSatisfied(EvaluationContext evaluationcontext) throws EvaluationException {
		boolean debug = evaluationcontext.shouldLogResult(this);
		// for each child, build the regex and evaluate it
		for (IEvaluator evaluator : evaluators) {
			try {
				List<String> replacements = new ArrayList<String>();
				for (String alias : aliases) {
					if (alias.startsWith("ctx.")) {
						String property = alias.substring(4);
						replacements.add((String)evaluationcontext.env.get(property));
					}
					else {
						String property = alias.substring(alias.indexOf('.') + 1);
						Method m = evaluator.getClass().getMethod("get" + property.substring(0, 1).toUpperCase() + property.substring(1));
						replacements.add((String)m.invoke(evaluator));
					}
				}
				Pattern pattern = Pattern.compile(MessageFormat.format(regex, replacements.toArray()));
				Matcher matcher = pattern.matcher(evaluationcontext.user.getProperty(header));
				if (matcher.find()) {
					// this is handled as an OR
					if (debug) {
						evaluationcontext.logResult(this, true);
					}
					return true;
				}
			}
			catch (NoSuchMethodException e) {
				if (debug) {
					evaluationcontext.logResult(this, 
												false, 
												"This child evaluator ("+evaluator.getSyntax()+") doesn't contain the property specified in the alias property");
				}
			}
			catch (InvocationTargetException e) {
				if (debug) {
					evaluationcontext.logResult(this, 
												false, 
												"This child evaluator ("+evaluator.getSyntax()+") doesn't contain the property specified in the alias property");
				}
			}
			catch (IllegalAccessException e) {
				if (debug) {
					evaluationcontext.logResult(this, 
												false, 
												"This child evaluator ("+evaluator.getSyntax()+") doesn't contain the property specified in the alias property");
				}
			}
		}
		if (debug) {
			evaluationcontext.logResult(this, false);
		}
		return false;
	}

	public void addEvaluator(IEvaluator e) throws EvaluationException {
		if (childSupported(e)) {
			evaluators.add(e);
		}
	}

	private boolean childSupported(IEvaluator e) {
		String name = e.getClass().getSimpleName();
		for (String child : SUPPORTED_CHILDREN) {
			if (child.equals(name)) {
				return true;
			}
		}
		return false;
	}

	public void validate() throws EvaluationException {
		if (isEmpty(header)) {
			throw new EvaluationException(this.syntax + " must have the 'header' attribute specified.");
		}
		if (isEmpty(regex)) {
			throw new EvaluationException(this.syntax + " must have the 'regex' attribute specified.");
		}
		if (0 == evaluators.size()) {
			throw new EvaluationException((this.syntax + " must contain at least one child tag."));
		}
	}
	
	protected boolean isEmpty(String str) {
		return null == str || 0 == str.length();
	}
}
