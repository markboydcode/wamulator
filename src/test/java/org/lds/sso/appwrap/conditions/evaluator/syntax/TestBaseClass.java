package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.LogicalSyntaxEvaluationEngine;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for syntax unit tests allowing for centralization of log tweaks
 * and disabling of syntax garbageCollector thread.
 * 
 * @author BoydMR
 *
 */
public class TestBaseClass {
    protected StringWriterHandler swa = null;
    protected Logger cLog = null;
    protected Level old = null;
    protected LogicalSyntaxEvaluationEngine eng = null;

    @BeforeClass
    public void setUpLogger() {
        cLog = Logger.getLogger(EvaluationContext.DEBUG_LOG_NAME);
        old = cLog.getLevel();
        cLog.setLevel(Level.FINE);
        swa = new StringWriterHandler();
        cLog.addHandler(swa);

        eng = new LogicalSyntaxEvaluationEngine() {
            @Override
            protected void startGarbageCollector() {
                // disables garbage collection so that it doesn't foul the logs
            }
        };
    }
    
    @AfterClass
    public void restoreLogger() {
        cLog.removeHandler(swa);
        cLog.setLevel(old);
    }
    
    @BeforeMethod
    public void resetLogBuffer() {
        swa.clearBuffer();
    }
}
