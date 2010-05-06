package org.lds.sso.appwrap.conditions.evaluator;

public class EvaluationException extends Exception {

    public EvaluationException(String string) {
        super(string);
    }

    public EvaluationException(String string, Exception e) {
        super(string, e);
    }

}
