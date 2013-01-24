package org.lds.sso.appwrap.conditions.evaluator;

import java.util.logging.Level;

import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.conditions.evaluator.syntax.AND;
import org.lds.sso.appwrap.conditions.evaluator.syntax.NOT;
import org.lds.sso.appwrap.conditions.evaluator.syntax.OR;
import org.lds.sso.appwrap.identity.User;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LogicalSyntaxEvaluationEngineTest {

    //@Test
    public void testXmlBaseCaching() throws EvaluationException {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.FINE);
        try {
            String xmlBase = "/ml-pap/group/labs-general.xml";
            String policy = "(&(ldsposv2=p4/*)(|(unit=506605)(unit=506303)(ldsmrn=*)))";
            LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
            eng.getEvaluator("test-evaluator", policy);
            Assert.assertEquals(eng.evaluators.size(), 2,
                    "should be two evaluators the root and the xml:base ref.");
            Assert.assertNotNull(eng.evaluators.get(xmlBase),
                    "xml:base node should be cached separately.");
            Assert.assertTrue(eng.evaluators.get(xmlBase).evaluator
                            .getClass() == OR.class,
                            "xml:base node should be instance of OR class");
            String policy2 = "(&(ldsposv2=p1/*)(|(unit=506605)(unit=506303)(ldsmrn=*)))";
            eng.getEvaluator("test-evaluator", policy2);
            Assert.assertEquals(eng.evaluators.size(), 3,
                            "should be three evaluators, two roots and the xml:base ref used by both.");
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}

	@Test
	public void testUnusedEvaluatorGarbageCollector() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.FINE);
        try {
			long oldVal = LogicalSyntaxEvaluationEngine.UNUSED_EVALUATOR_MAX_LIFE_MILLIS;
			
			LogicalSyntaxEvaluationEngine.UNUSED_EVALUATOR_MAX_LIFE_MILLIS = 6000;
			long start = System.currentTimeMillis();
			System.out.println("--main instantiating engine and hence scanner at " 
					+ (System.currentTimeMillis() - start));
			LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
			// scan sleep time elapsed ==> 0
			System.out.println("--main sleeping for 1500 at " 
					+ (System.currentTimeMillis() - start));
			
			Thread.sleep(1500);
			System.out.println("--scanner should be sleeping now....");
			System.out.println("--main creating two evaluators at " 
					+ (System.currentTimeMillis() - start));
			eng.getEvaluator("test-evaluator", "(acctid=12345)");
			eng.getEvaluator("test-evaluator", "(&(accid=12345)(mrn=*)(employee=A))");
			int size = eng.evaluators.size();
			System.out.println("--main testing for 2 evaluators at " 
					+ (System.currentTimeMillis() - start));
			Assert.assertTrue(size == 2, "should be 2 evaluators but was " + size);
			System.out.println("--main sleeping for 3000 at " 
					+ (System.currentTimeMillis() - start));
	
			Thread.sleep(3000);
			System.out.println("--main creating one evaluator at " 
					+ (System.currentTimeMillis() - start));
			eng.getEvaluator("test-evaluator", "(accid=??????)");
			System.out.println("--main testing for 3 evaluators at " 
					+ (System.currentTimeMillis() - start));
			size = eng.evaluators.size();
			Assert.assertTrue(size == 3, "should be 3 evaluators right now but was " + size);
			System.out.println("--main sleeping for 5500 at " 
					+ (System.currentTimeMillis() - start));
	
			Thread.sleep(5500);
			System.out.println("--main testing for 1 evaluators at " 
					+ (System.currentTimeMillis() - start));
			size = eng.evaluators.size();
			Assert.assertTrue(size == 1, "should be one evaluators right now but size was " + size);
			System.out.println("--main sleeping for 3000 at " 
					+ (System.currentTimeMillis() - start));
	
			Thread.sleep(3000);
			System.out.println("--main testing for 0 evaluators at " 
					+ (System.currentTimeMillis() - start));
			size = eng.evaluators.size();
			Assert.assertTrue(size == 0, "all evaluators should have been reclaimed but size was " + size);
			eng.garbageCollector.interrupt();
			LogicalSyntaxEvaluationEngine.UNUSED_EVALUATOR_MAX_LIFE_MILLIS = oldVal;
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}
	
	@Test
	public void testNOT() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
			LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
			IEvaluator ev = eng.getEvaluator("test-evaluator", "(!(apps=12345))");
			Assert.assertTrue(ev instanceof NOT, "Wrong class instantiated.");
			
			EvaluationContext ctx = new EvaluationContext();
			ctx.user = new User("nm","pwd");
			ctx.user.addAttributeValues("apps", new String[] {"12345"});
			
			Assert.assertFalse(ev.isConditionSatisfied(ctx), "should be false since has apps 12345");
			
			ctx.user = EasyMock.createMock(User.class);
			ctx.user = new User("nm","pwd");
			ctx.user.addAttributeValues("apps", new String[] {"-----"});
			
			Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be true since doesn't have apps 12345");
	
			eng.garbageCollector.interrupt();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}

	@Test
	public void testAND() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
			LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
			IEvaluator ev = eng.getEvaluator("test-evaluator", "(&(accid=12345)(mrn=*)(employee=A))");
			Assert.assertTrue(ev instanceof AND, "Wrong class instantiated.");
			
			EvaluationContext ctx = new EvaluationContext();
			ctx.user = new User("nm","pwd");
			ctx.user.addAttributeValues("accid", new String[] {"12345"});
			ctx.user.addAttributeValues("employee", new String[] {"A"});
			ctx.user.addAttributeValues("mrn", new String[] {"12345"});
			
			Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be employee, member, and have ldsAccountId 12345");
	
			ctx.user = EasyMock.createMock(User.class);
			ctx.user = new User("nm","pwd");
			ctx.user.addAttributeValues("employee", new String[] {"T"});
			
			Assert.assertFalse(ev.isConditionSatisfied(ctx), "should fail since not an employee");
	
			eng.garbageCollector.interrupt();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}

	@Test (expectedExceptions= {EvaluationException.class})
	public void testORValidateThrowsExcp() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
        	OR or = new OR();
        	or.validate();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}
	
	@Test (expectedExceptions= {EvaluationException.class})
	public void testANDValidateThrowsExcp() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
        	AND and = new AND();
        	and.validate();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}
	
	@Test (expectedExceptions= {EvaluationException.class})
	public void testNOTValidateThrowsExcp() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
        	NOT not = new NOT();
        	not.validate();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}
	
	@Test
	public void testOR() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
			LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
			IEvaluator ev = eng.getEvaluator("test-evaluator", "(|(accid=12345)(mrn=*)(employee=A))");
			Assert.assertTrue(ev instanceof OR, "Wrong class instantiated.");
			
			EvaluationContext ctx = new EvaluationContext();
			ctx.user = new User("nm","pwd");
			ctx.user.addAttributeValues("employee", new String[]{"A"});
			ctx.user.addAttributeValues("mrn", new String[]{"something"});
			
			Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be true for employee");
	
			ctx.user = new User("nm","pwd");
			ctx.user.addAttributeValues("employee", new String[]{"E"});
			ctx.user.addAttributeValues("mrn", new String[]{"something"});
			
			Assert.assertTrue(ev.isConditionSatisfied(ctx), "should true for member");
	
			ctx.user = new User("nm","pwd");
			ctx.user.addAttributeValues("employee", new String[]{"E"});
			ctx.user.addAttributeValues("accid", new String[]{"12345"});
			
			Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be true for ldsapplication 12345");
	
			ctx.user = new User("nm","pwd");
			ctx.user.addAttributeValues("employee", new String[]{"E"});
			ctx.user.addAttributeValues("accid", new String[]{"2222"});
			
			Assert.assertFalse(ev.isConditionSatisfied(ctx), "should fail since not an employee, member, or has the correct ldsapplication");
	
			eng.garbageCollector.interrupt();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}
}
