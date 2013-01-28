package org.lds.sso.appwrap.conditions.evaluator;

import java.util.logging.Level;

import org.lds.sso.appwrap.conditions.evaluator.syntax.SyntaxBase;
import org.testng.Assert;
import org.testng.annotations.Test;

public class EvaluationContextTest {
/*
TODO: 
1) unit test of hasLdsAccountId w/debugging
2) start adding logging to other elements and add unit tests for each of them
3) add unit tests for multi-level nesting
4) add unit tests multi-level evaluators with debug specified in middle
5) test multiple debugs don't cause problems, only outermost is honored. 
 */

    @Test
    public void testShouldLogByDebugResult() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.FINE);
        try {
            LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
            eng.garbageCollector.interrupt();

            IEvaluator ev = eng.getEvaluator("test-evaluator", "(employee=*)", true);
            SyntaxBase base = (SyntaxBase) ev;

            EvaluationContext ctx = new EvaluationContext();

            Assert.assertTrue(ctx.shouldLogResult(base), "should log result.");
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
    }

    @Test
    public void testShouldNotLogResult() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.FINE);
        try {
            LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
            eng.garbageCollector.interrupt();

            IEvaluator ev = eng.getEvaluator("test-evaluator", "(employee=*)");
            SyntaxBase base = (SyntaxBase) ev;

            EvaluationContext ctx = new EvaluationContext();

            Assert.assertFalse(ctx.shouldLogResult(base),
                    "should NOT log result.");
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
    }
}
