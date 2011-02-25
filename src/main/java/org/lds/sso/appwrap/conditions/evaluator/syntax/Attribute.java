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
    private Attribute.Operation operation;
    private String attributeName;
    private String operationName;
    private String attributeValue;

    @Override
    public void init(String syntax, Map<String, String> cfg) throws EvaluationException {
        super.init(syntax, cfg);
        this.attributeName = cfg.get("name");
        this.operationName = cfg.get("operation");
        this.attributeValue = cfg.get("value");
        this.operation = Operation.getOperation(cfg.get("operation"));
    }

    @Override
    public boolean isConditionSatisfied(EvaluationContext evaluationcontext) throws EvaluationException {
        User user = evaluationcontext.user;

        switch (operation){
            case EQUALS:
                return user.hasAttributeValue(attributeName, attributeValue);
            case EXISTS:
//                return user.hasAttribute(attributeName);
                return false;
            case NOT_SUPPPORTED:
                throw new EvaluationException("Operation not supported: " + operationName);
            default:
                return false;
        }
    }

    public enum Operation {
        EXISTS,
        EQUALS,
        NOT_SUPPPORTED;

        public static Operation getOperation(String operation){
            if("exists".equalsIgnoreCase(operation)){
                return EXISTS;
            }
            else if("equals".equalsIgnoreCase(operation)){
                return EQUALS;
            }
            else{
                return NOT_SUPPPORTED;
            }
        }
    }
}
