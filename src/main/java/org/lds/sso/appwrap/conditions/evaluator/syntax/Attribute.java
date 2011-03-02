package org.lds.sso.appwrap.conditions.evaluator.syntax;

import org.lds.sso.appwrap.NvPair;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: gmdayley
 * Date: 2/25/11
 * Time: 2:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class Attribute extends SyntaxBase {
    private static final String EXTENSIBLE_CASE_EXACT_SUBSTRING_MATCH = "caseExactSubstringMatch";
    private static final String EXTENSIBLE_CASE_EXACT_MATCH = "caseExactMatch";
    private static final String CONFIG_NAME = "name";
    private static final String CONFIG_OPERATION = "operation";
    private static final String CONFIG_VALUE = "value";

    private Attribute.Operation operation;
    private String attributeName;
    private String operationName;
    private String attributeValue;
    private boolean caseExactSubstringMatch;
    private boolean caseExactMatch;

    @Override
    public void init(String syntax, Map<String, String> cfg) throws EvaluationException {
        super.init(syntax, cfg);
        this.attributeName = super.getRequired(CONFIG_NAME, cfg);//parseAttributeName(cfg.get(CONFIG_NAME));
        this.operationName = super.getRequired(CONFIG_OPERATION, cfg);
        this.operation = Operation.getOperation(cfg.get(CONFIG_OPERATION));
        if(operation == Attribute.Operation.EQUALS){
            this.attributeValue = super.getRequired(CONFIG_VALUE, cfg);
        }
    }

   /* private String parseAttributeName(String name) {
        if(name.contains(":") && name.endsWith(":")){//Check for Extensible Matching
            String extensible = name.substring(name.indexOf(":"), name.lastIndexOf(":"));
            if(EXTENSIBLE_CASE_EXACT_MATCH.equals(extensible)){
                caseExactMatch = true;
            }
            else if(EXTENSIBLE_CASE_EXACT_SUBSTRING_MATCH.equals(extensible)){
                caseExactSubstringMatch = true;
            }
            return name.substring(name.indexOf(":"));
        }
        return name;
    }*/

    @Override
    public boolean isConditionSatisfied(EvaluationContext evaluationCtx) throws EvaluationException {
        User user = evaluationCtx.user;

        boolean satisfied;
        switch (operation) {
            case EQUALS:
                NvPair[] attribute = user.getAttribute(attributeName);
                satisfied = wildCardMatch(attribute[0].getValue(), attributeValue);
                if(satisfied){
                    debug(evaluationCtx, satisfied, String.format("user has attribute that matches value, actual: %s", attribute[0].getValue()));
                }
                else{
                    debug(evaluationCtx, satisfied, String.format("user does not have attribute that matches value, actual: %s", attribute[0].getValue()));
                }

                break;
            case EXISTS:
                satisfied = user.hasAttribute(attributeName);
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

/*    public static boolean wildCardMatch2(String text, String pattern) {
//        int idx = pattern.indexOf("(?<!\\\\)\\*");
        String[] cards = pattern.split("\\*", 2);
        //Find card

        int idx = text.indexOf(cards[0]);
        if (idx == -1) {return false;}
        text = text.substring(idx + cards[0].length());


        if (cards.length == 1) { // No wildcard
            return cards[0].equals("") || text.equals("");
        }
        else {
            return wildCardMatch2(text, cards[1]);
        }
    }*/

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
