package org.lds.sso.appwrap.conditions.evaluator.syntax;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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
    protected StringWriterAppender swa = null;
    protected Logger cLog = null;
    protected Level old = null;
    protected LogicalSyntaxEvaluationEngine eng = null;

    @BeforeClass
    public void setUpLogger() {
        cLog = Logger.getLogger(EvaluationContext.DEBUG_LOG_NAME);
        old = cLog.getLevel();
        cLog.setLevel(Level.DEBUG);
        swa = new StringWriterAppender();
        cLog.addAppender(swa);

        eng = new LogicalSyntaxEvaluationEngine() {
            @Override
            protected void startGarbageCollector() {
                // disables garbase collection so that it doesn't foul the logs
            }
        };
    }
    
    @AfterClass
    public void restoreLogger() {
        cLog.removeAppender(swa);
        cLog.setLevel(old);
    }
    
    @BeforeMethod
    public void resetLogBuffer() {
        swa.clearBuffer();
    }
}
