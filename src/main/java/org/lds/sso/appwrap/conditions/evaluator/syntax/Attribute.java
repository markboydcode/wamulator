package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.Map;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.identity.User;

/**
 * Created by IntelliJ IDEA.
 * User: gmdayley
 * Date: 2/25/11
 * Time: 2:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class Attribute extends SyntaxBase {
    private static final String CONFIG_NAME = "name";
    private static final String CONFIG_OPERATION = "operation";
    private static final String CONFIG_VALUE = "value";

    private Attribute.Operation operation;
    private String attributeName;
    private String operationName;
    private String attributeValue;

    @Override
    public void init(String syntax, Map<String, String> cfg) throws EvaluationException {
        super.init(syntax, cfg);
        this.attributeName = super.getRequired(CONFIG_NAME, cfg);
        this.operationName = super.getRequired(CONFIG_OPERATION, cfg);
        this.operation = Operation.getOperation(cfg.get(CONFIG_OPERATION));
        if(operation == Attribute.Operation.EQUALS){
            this.attributeValue = super.getRequired(CONFIG_VALUE, cfg);
        }
    }

    @Override
    public boolean isConditionSatisfied(EvaluationContext evaluationCtx) throws EvaluationException {
        User user = evaluationCtx.user;        

        boolean satisfied = false;
        switch (operation) {
            case EQUALS:
                String[] vals = user.getAttribute(attributeName.toLowerCase());
                if (vals != null) {
                	if (vals.length == 1 && vals[0].contains(":")) {
                    	vals = vals[0].split(":");
                    }
                    for (String val : vals) {
                        if (wildCardMatch(val, attributeValue)) {
                            satisfied = true;
                            break;
                        }
                    }
                }
                if (satisfied) {
                    debug(evaluationCtx, satisfied, String.format("user has attribute that matches value, actual: %s", vals[0]));
                } else {
                    if (vals != null && vals.length != 0) {
                        debug(evaluationCtx, satisfied, String.format("user does not have attribute that matches value, actual: %s", vals[0]));
                    } else {
                        debug(evaluationCtx, satisfied, "user does not have attribute");
                    }
                }

                break;
            case EXISTS:
                satisfied = user.hasAttribute(attributeName.toLowerCase());
                debug(evaluationCtx, satisfied, String.format("Evaluating if user has attribute[%s] %b", attributeName, satisfied));
                break;
            case NOT_SUPPPORTED:
                throw new EvaluationException("Operation not supported: " + operationName);
            default:
                return false;
        }

        return satisfied;
    }

    public static boolean wildCardMatch(String text, String pattern){
    	// if no wildcard then test for exact match
    	if (!pattern.contains("*")) {
    		return text.equals(pattern);
    	}
    	
        boolean startWithWildcard = pattern.startsWith("*");
        boolean endsWithWildcard = pattern.endsWith("*");
        String [] tokens = pattern.split("(?<!\\\\)\\*"); //Find all * that don't have a \ before them

        //Escape all * in text string to match escaped * in pattern
        text = text.replaceAll("\\*", "\\\\*");
        //TODO: case sensitivity


        for (int i = 0, tokensLength = tokens.length; i < tokensLength; i++) {
            String card = tokens[i];

            //todo: possibly move these outside the loop for performance...
            if(i == 0){//First time
                if(!startWithWildcard){
                    if(!text.startsWith(card)){
                        return false;
                    }
                }
            }
            else if(i == tokensLength-1){//Last time
                if(!endsWithWildcard){
                    if(!text.endsWith(card)){
                        return false;
                    }
                }
            }

            int idx = text.indexOf(card);
            if (idx == -1) { return false;}
            text = text.substring(idx + card.length());
        }

        return true;
    }

    public void debug(EvaluationContext ctx, boolean outcome, String message){
        if(ctx.shouldLogResult(this)){
            ctx.logResult(this, outcome, message);
        }
    }

    public enum Operation {
        EXISTS,
        EQUALS,
        NOT_SUPPPORTED;

        public static Operation getOperation(String operation) {
            if ("exists".equalsIgnoreCase(operation)) {
                return EXISTS;
            } else if ("equals".equalsIgnoreCase(operation)) {
                return EQUALS;
            } else {
                return NOT_SUPPPORTED;
            }
        }
    }


}
