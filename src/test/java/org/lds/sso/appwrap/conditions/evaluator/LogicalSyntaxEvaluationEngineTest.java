package org.lds.sso.appwrap.conditions.evaluator;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.NvPair;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.syntax.AND;
import org.lds.sso.appwrap.conditions.evaluator.syntax.HasAssignment;
import org.lds.sso.appwrap.conditions.evaluator.syntax.HasLdsAccountId;
import org.lds.sso.appwrap.conditions.evaluator.syntax.HasPosition;
import org.lds.sso.appwrap.conditions.evaluator.syntax.IsEmployee;
import org.lds.sso.appwrap.conditions.evaluator.syntax.IsMember;
import org.lds.sso.appwrap.conditions.evaluator.syntax.MemberOfUnit;
import org.lds.sso.appwrap.conditions.evaluator.syntax.NOT;
import org.lds.sso.appwrap.conditions.evaluator.syntax.OR;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LogicalSyntaxEvaluationEngineTest {

    @Test
    public void testXmlBaseCaching() throws EvaluationException {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.DEBUG);
        try {
            String xmlBase = "/ml-pap/group/labs-general.xml";
            String policy = "" 
                + "<AND>" 
                + "  <OR name='labs general access\' xml:base='" + xmlBase + "'>" 
                + "    <IsMember/>"
                + "    <MemberOfUnit>"
                + "      <Unit id='506303'/>" 
                + "      <Unit id='506605'/>"
                + "    </MemberOfUnit>" 
                + "  </OR>" 
                + "  <HasPosition>"
                + "    <Position id='4' type='bishop'/>"
                + "  </HasPosition>" 
                + "</AND>";
            LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
            eng.getEvaluator(policy);
            Assert.assertEquals(eng.evaluators.size(), 2,
                    "should be two evaluators the root and the xml:base ref.");
            Assert.assertNotNull(eng.evaluators.get(xmlBase),
                    "xml:base node should be cached separately.");
            Assert.assertTrue(eng.evaluators.get(xmlBase).evaluator
                            .getClass() == OR.class,
                            "xml:base node should be instance of OR class");
            String policy2 = 
                      "<AND>" 
                    + "  <OR name='labs general access\' "
                    + "      xml:base='" + xmlBase + "'>" 
                    + "    <IsMember/>"
                    + "    <MemberOfUnit>"
                    + "      <Unit id='506303'/>" 
                    + "      <Unit id='506605'/>"
                    + "    </MemberOfUnit>" 
                    + "  </OR>" 
                    + "  <HasPosition>"
                    + "    <Position id='1' type='Stake President'/>"
                    + "  </HasPosition>" 
                    + "</AND>";
            eng.getEvaluator(policy2);
            Assert.assertEquals(eng.evaluators.size(), 3,
                            "should be three evaluators, two roots and the xml:base ref used by both.");
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}

	@Test
	public void testUnusedEvaluatorGarbageCollector() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.DEBUG);
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
		eng.getEvaluator("<HasLdsApplication value='12345' username='ngienglishbishop'/>");
		eng.getEvaluator(
				"<AND>" +
				" <IsEmployee/>" +
				" <IsMember/>" +
				" <HasLdsApplication value='12345' username='ngienglishbishop'/>" +
				"</AND>");
		int size = eng.evaluators.size();
		System.out.println("--main testing for 2 evaluators at " 
				+ (System.currentTimeMillis() - start));
		Assert.assertTrue(size == 2, "should be 2 evaluators but was " + size);
		System.out.println("--main sleeping for 3000 at " 
				+ (System.currentTimeMillis() - start));

		Thread.sleep(3000);
		System.out.println("--main creating one evaluator at " 
				+ (System.currentTimeMillis() - start));
		eng.getEvaluator("<HasLdsApplication value='??????' username='ngienglishbishop'/>");
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
		IEvaluator ev = eng.getEvaluator("<NOT><HasLdsApplication value='12345' username='ngienglishbishop'/></NOT>");
		Assert.assertTrue(ev instanceof NOT, "Wrong class instantiated.");
		
		EvaluationContext ctx = new EvaluationContext();
		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getAttributes()).andReturn(new NvPair[] {new NvPair(User.LDSAPPS_ATT, "12345")});
		EasyMock.replay(ctx.user);
		
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should be false since has lds application 12345");
		
		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getAttributes()).andReturn(new NvPair[] {new NvPair(User.LDSAPPS_ATT, "-----")});
		EasyMock.replay(ctx.user);
		
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be true since doesn't have lds application 12345");
		EasyMock.verify(ctx.user);
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
		IEvaluator ev = eng.getEvaluator(
				"<AND>" +
				" <IsEmployee/>" +
				" <IsMember/>" +
				" <HasLdsApplication value='12345' username='ngienglishbishop'/>" +
				"</AND>");
		Assert.assertTrue(ev instanceof AND, "Wrong class instantiated.");
		
		EvaluationContext ctx = new EvaluationContext();
		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.DN)).andReturn("-- ou=int --");
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.LDS_MRN)).andReturn("12345");
		EasyMock.expect(ctx.user.getAttributes()).andReturn(new NvPair[] {new NvPair(User.LDSAPPS_ATT, "12345")});
		EasyMock.replay(ctx.user);
		
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be employee, member, and have ldsAccountId 12345");
		EasyMock.verify(ctx.user);

		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.DN)).andReturn("-- ou=ext --");
		EasyMock.replay(ctx.user);
		
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should fail since not an employee");
		EasyMock.verify(ctx.user);
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
		IEvaluator ev = eng.getEvaluator(
				"<OR>" +
				" <IsEmployee/>" +
				" <IsMember/>" +
				" <HasLdsApplication value='12345' username='ngienglishbishop'/>" +
				"</OR>");
		Assert.assertTrue(ev instanceof OR, "Wrong class instantiated.");
		
		EvaluationContext ctx = new EvaluationContext();
		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.DN)).andReturn("-- ou=int --");
		EasyMock.replay(ctx.user);
		
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be true for employee");
		EasyMock.verify(ctx.user); 

		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.DN)).andReturn("-- ou=ext --");
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.LDS_MRN)).andReturn("12345");
		EasyMock.replay(ctx.user);
		
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should true for member");
		EasyMock.verify(ctx.user);

		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.DN)).andReturn("-- ou=ext --");
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.LDS_MRN)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
		EasyMock.expect(ctx.user.getAttributes()).andReturn(new NvPair[] {new NvPair(User.LDSAPPS_ATT, "12345")});
		EasyMock.replay(ctx.user);
		
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be true for ldsapplication 12345");
		EasyMock.verify(ctx.user);

		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.DN)).andReturn("-- ou=ext --");
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.LDS_MRN)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
		EasyMock.expect(ctx.user.getAttributes()).andReturn(new NvPair[] {new NvPair(User.LDSAPPS_ATT, "2222")});
		EasyMock.replay(ctx.user);
		
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should fail since not an employee, member, or has the correct ldsapplication");
		EasyMock.verify(ctx.user);
		eng.garbageCollector.interrupt();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}


	@Test
	public void testIsEmployee() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
		LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
		IEvaluator ev = eng.getEvaluator("<IsEmployee/>");
		Assert.assertTrue(ev instanceof IsEmployee, "Wrong class instantiated.");
		
		EvaluationContext ctx = new EvaluationContext();
		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.DN)).andReturn("-- ou=int --");
		EasyMock.replay(ctx.user);
		
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "is an employee");
		EasyMock.verify(ctx.user);
		
		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.DN)).andReturn("-- ou=ext --");
		EasyMock.replay(ctx.user);
		
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "is not an employee");
		EasyMock.verify(ctx.user);
		eng.garbageCollector.interrupt();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}

	@Test
	public void testIsMember() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
		LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
		IEvaluator ev = eng.getEvaluator("<IsMember/>");
		Assert.assertTrue(ev instanceof IsMember, "Wrong class instantiated.");
		
		EvaluationContext ctx = new EvaluationContext();
		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.LDS_MRN)).andReturn("12345");
		EasyMock.replay(ctx.user);
		
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "is a member");
		EasyMock.verify(ctx.user);
		
		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.LDS_MRN)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
		EasyMock.replay(ctx.user);
		
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "is not a member");
		EasyMock.verify(ctx.user);
		eng.garbageCollector.interrupt();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}

	@Test
	public void testMemberOfUnit() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
		LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
		IEvaluator ev = eng.getEvaluator("<MemberOfUnit id='111'/>");
		Assert.assertTrue(ev instanceof MemberOfUnit, "Wrong class instantiated.");
		
		EvaluationContext ctx = new EvaluationContext();
		ctx.user = EasyMock.createMock(User.class);
        EasyMock.expect(ctx.user.getProperty(UserHeaderNames.UNITS)).andReturn("/7u33/5u111/1u555/");
		EasyMock.replay(ctx.user);
		
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be member of 111");
		EasyMock.verify(ctx.user);
		
		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.UNITS)).andReturn("/7u33/5u111/");
		EasyMock.replay(ctx.user);
		
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be member of 111");
		EasyMock.verify(ctx.user);

		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.UNITS)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
		EasyMock.replay(ctx.user);
		
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not be a member of 111");
		EasyMock.verify(ctx.user);
		eng.garbageCollector.interrupt();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}
	
    @Test
    public void testMemberOfUnitNested() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
            LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
            IEvaluator ev = eng.getEvaluator("<MemberOfUnit id='333'><Unit id='455'/><Unit id='500'/><Unit id='200'/></MemberOfUnit>");
            Assert.assertTrue(ev instanceof MemberOfUnit, "Wrong class instantiated.");
            
            EvaluationContext ctx = new EvaluationContext();
            ctx.user = EasyMock.createMock(User.class);
            EasyMock.expect(ctx.user.getProperty(UserHeaderNames.UNITS)).andReturn("/7u455/5u111/1u555/");
            EasyMock.replay(ctx.user);
            
            Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be member of 455");
            EasyMock.verify(ctx.user);
            
            ctx.user = EasyMock.createMock(User.class);
            EasyMock.expect(ctx.user.getProperty(UserHeaderNames.UNITS)).andReturn("/7u222/5u33/1u333/");
            EasyMock.replay(ctx.user);
            
            Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be member of 333");
            EasyMock.verify(ctx.user);
    
            ctx.user = EasyMock.createMock(User.class);
            EasyMock.expect(ctx.user.getProperty(UserHeaderNames.UNITS)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
            EasyMock.replay(ctx.user);
            
            Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not be a member of 111, 455, 200, or 500");
            EasyMock.verify(ctx.user);
            eng.garbageCollector.interrupt();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
    }
    
    @Test
    public void testMemberOfAssignmentUnit() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
        LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
        IEvaluator ev = eng.getEvaluator("<MemberOfUnit id='200'></MemberOfUnit>");
        Assert.assertTrue(ev instanceof MemberOfUnit, "Wrong class instantiated.");
        
        EvaluationContext ctx = new EvaluationContext();
        ctx.user = EasyMock.createMock(User.class);
        EasyMock.expect(ctx.user.getProperty(UserHeaderNames.UNITS)).andReturn("/7u455/5u111/1u555/");
        EasyMock.expect(ctx.user.getProperty(UserHeaderNames.POSITIONS)).andReturn("p4/7u200/5u111/1u555/:p4/7u500/5u600/1u700/");
        EasyMock.replay(ctx.user);
        
        Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be member of 200");
        EasyMock.verify(ctx.user);
        
        ctx.user = EasyMock.createMock(User.class);
        EasyMock.expect(ctx.user.getProperty(UserHeaderNames.UNITS)).andReturn("/7u455/5u111/1u555/");
        EasyMock.expect(ctx.user.getProperty(UserHeaderNames.POSITIONS)).andReturn("p4/7u222/5u200/1u555/:p4/7u500/5u600/1u700/");
        EasyMock.replay(ctx.user);
        
        Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be member of 200");
        EasyMock.verify(ctx.user);

        ctx.user = EasyMock.createMock(User.class);
        EasyMock.expect(ctx.user.getProperty(UserHeaderNames.UNITS)).andReturn("/7u455/5u111/1u555/");
        EasyMock.expect(ctx.user.getProperty(UserHeaderNames.POSITIONS)).andReturn("p4/7u222/5u333/1u200/:p4/7u500/5u600/1u700/");
        EasyMock.replay(ctx.user);
        
        Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be member of 200");
        EasyMock.verify(ctx.user);

        ctx.user = EasyMock.createMock(User.class);
        EasyMock.expect(ctx.user.getProperty(UserHeaderNames.UNITS)).andReturn("/7u455/5u111/1u555/");
        EasyMock.expect(ctx.user.getProperty(UserHeaderNames.POSITIONS)).andReturn("p4/7u222/5u333/1u555/:p4/7u200/5u600/1u700/");
        EasyMock.replay(ctx.user);
        
        Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be member of 200");
        EasyMock.verify(ctx.user);

        ctx.user = EasyMock.createMock(User.class);
        EasyMock.expect(ctx.user.getProperty(UserHeaderNames.UNITS)).andReturn("/7u455/5u111/1u555/");
        EasyMock.expect(ctx.user.getProperty(UserHeaderNames.POSITIONS)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
        EasyMock.replay(ctx.user);
        
        Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not be a member of 111, 455, 200, or 500");
        EasyMock.verify(ctx.user);
        eng.garbageCollector.interrupt();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
    }
    
	@Test 
	public void testMemberOfUnitInitAllowsEmptyAtts() throws Exception {
		try {
	        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
	        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
	        try {
		MemberOfUnit m = new MemberOfUnit();
		m.init("", new HashMap());
	        } finally {
	            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
	        }
		}
		catch(Exception e) {
			Assert.fail("init() shouldn't throw an exception", e);
		}
	}
	
	@Test (expectedExceptions= {EvaluationException.class})
	public void testMemberOfUnitValidateThrowsExcp() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
		MemberOfUnit m = new MemberOfUnit();
		m.init("", new HashMap());
		m.validate();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}
	
	@Test 
	public void testHasPositionInitAllowsEmptyAtts() throws Exception {
		try {
	        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
	        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
	        try {
		HasPosition m = new HasPosition();
		m.init("", new HashMap());
	        } finally {
	            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
	        }
		}
		catch(Exception e) {
			Assert.fail("init() shouldn't throw an exception", e);
		}
	}
	
	@Test (expectedExceptions= {EvaluationException.class})
	public void testHasPositionValidateThrowsExcp() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
		HasPosition m = new HasPosition();
		m.init("", new HashMap());
		m.validate();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}
	
	@Test
	public void testHasPosition() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
		LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
		IEvaluator ev = eng.getEvaluator("<HasPosition id='4'/>");
		Assert.assertTrue(ev instanceof HasPosition, "Wrong class instantiated.");
		
		EvaluationContext ctx = new EvaluationContext();
		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.POSITIONS)).andReturn("P4/u766/u5111/u1555/");
		EasyMock.replay(ctx.user);
		
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have position 4");
		EasyMock.verify(ctx.user);
		
		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.POSITIONS)).andReturn("p1/u5111/u1555/");
		EasyMock.replay(ctx.user);
		
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have position 4");
		EasyMock.verify(ctx.user);

		ctx.user = EasyMock.createMock(User.class);
		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.POSITIONS)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
		EasyMock.replay(ctx.user);
		
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have position 4");
		EasyMock.verify(ctx.user);
		eng.garbageCollector.interrupt();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}

	@Test
	public void testMultipleHasPosition() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
    		LogicalSyntaxEvaluationEngine eng = new LogicalSyntaxEvaluationEngine();
    		IEvaluator ev = eng.getEvaluator("<HasPosition id='4'><Position id='57'/></HasPosition>");
    		Assert.assertTrue(ev instanceof HasPosition, "Wrong class instantiated.");
    		
    		EvaluationContext ctx = new EvaluationContext();
    		ctx.user = EasyMock.createMock(User.class);
    		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.POSITIONS)).andReturn("p4/7u66/5u111/1u555/");
    		EasyMock.replay(ctx.user);
    		
    		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have position 4");
    		EasyMock.verify(ctx.user);
    		
    		ctx.user = EasyMock.createMock(User.class);
    		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.POSITIONS)).andReturn("P57/7U66/5U111/1U555"); // test case-insensitivity
    		EasyMock.replay(ctx.user);
    		
    		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have position 57");
    		EasyMock.verify(ctx.user);
    		
    		ctx.user = EasyMock.createMock(User.class);
    		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.POSITIONS)).andReturn("P1:66:111:555");
    		EasyMock.replay(ctx.user);
    		
    		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have position 4 or 57");
    		EasyMock.verify(ctx.user);
    
    		ctx.user = EasyMock.createMock(User.class);
    		EasyMock.expect(ctx.user.getProperty(UserHeaderNames.POSITIONS)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
    		EasyMock.replay(ctx.user);
    		
    		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have position 4 or 57");
    		EasyMock.verify(ctx.user);
    		eng.garbageCollector.interrupt();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}

	@Test 
	public void testHasAssignmentInitAllowsEmptyAtts() throws Exception {
		try {
	        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
	        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
	        try {
        		HasAssignment m = new HasAssignment();
        		m.init("", new HashMap());
	        } finally {
	            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
	        }
		}
		catch(Exception e) {
			Assert.fail("init() shouldn't throw an exception", e);
		}
	}
	
	@Test (expectedExceptions= {EvaluationException.class})
	public void testHasAssignmentValidateThrowsExcp() throws Exception {
        Level old = LogicalSyntaxEvaluationEngine.cLog.getLevel();
        LogicalSyntaxEvaluationEngine.cLog.setLevel(Level.OFF);
        try {
    		HasAssignment m = new HasAssignment();
    		m.init("", new HashMap());
    		m.validate();
        } finally {
            LogicalSyntaxEvaluationEngine.cLog.setLevel(old);
        }
	}

}
