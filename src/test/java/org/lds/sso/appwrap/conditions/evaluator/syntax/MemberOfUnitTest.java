package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.lds.sso.appwrap.conditions.evaluator.syntax.MemberOfUnit.Values;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MemberOfUnitTest extends TestBaseClass {
	@Test
	public void verifyEmptyUnitNoCallToAddCfgVal() throws EvaluationException {
		MemberOfUnit mou = new MemberOfUnit();
		mou.init("syntax", new HashMap());
		Assert.assertEquals(mou.cfgUnits.size(), 0);
	}
	
	@Test
	public void testMemberOfUnitDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("<MemberOfUnit id='555' debug-user='ngienglishbishop'/>");
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.UNITS)).andReturn("/7u1/5u111/1u555/");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be a member of 555");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "T  <MemberOfUnit debug-user='ngienglishbishop' id='555'/>  user is member of unit 555");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	
	@Test
	public void testMemberOfUnitByAssigDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("<MemberOfUnit id='555' debug-user='ngienglishbishop'/>");
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.UNITS)).andReturn("/7u1/5u111/1u888/");
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn("p4/7u1/5u555/1u888/");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be a member of 555 by virtue of having assignment in 555");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "T  <MemberOfUnit debug-user='ngienglishbishop' id='555'/>  user is not a member of unit 555 but has assignment in unit.");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testMemberOfUnitNotDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("<MemberOfUnit id='555' debug-user='ngienglishbishop'/>");
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.UNITS)).andReturn("/7u20/5u111/1u500/");
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not be a member of 555");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <MemberOfUnit debug-user='ngienglishbishop' id='555'/>  user is not a member of nor has assignment in 555");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasNoUnitDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("<MemberOfUnit id='555' debug-user='ngienglishbishop'/>");
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.UNITS)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not be a member of 555");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <MemberOfUnit debug-user='ngienglishbishop' id='555'/>  user is not a member of any unit");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testMemberOfUnitNotAnyDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<MemberOfUnit id='555' debug-user='ngienglishbishop'>" +
				" <Unit id='555'/>" +
				" <Unit id='500'/>" +
				"</MemberOfUnit>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.UNITS)).andReturn("/7u40/5u222/1u333/");
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not be a member of any of the units");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <MemberOfUnit debug-user='ngienglishbishop' id='555'/>  user is not a member of nor has assignments in any configured units");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	
	@Test
	public void testMemberOfUnitEmptySessionValDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<MemberOfUnit debug-user='ngienglishbishop'>" +
				" <Unit id='650'/>" +
				" <Unit id='550'/>" +
				" <Unit id='500'/>" +
				"</MemberOfUnit>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.UNITS)).andReturn(null);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not be a member of any of the units");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <MemberOfUnit debug-user='ngienglishbishop'/>  units not in session");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testBadValuesDontGetAddedToCfg() throws EvaluationException {
		MemberOfUnit mou = new MemberOfUnit();
		mou.init("syntax", new HashMap<String,String>());
		Assert.assertEquals(mou.cfgUnits.size(), 0);
		
		Map<String,String> vals = new HashMap<String,String>();
		vals.put("id", "");
		Assert.assertEquals(mou.cfgUnits.size(), 0);
		
		vals = new HashMap<String,String>();
		vals.put("id", null);
		mou.init("syntax", vals);
		Assert.assertEquals(mou.cfgUnits.size(), 0);
	}
	
	@Test(expectedExceptions = {EvaluationException.class})	
	public void testInvalidEvaluatorNotAllowed() throws EvaluationException {
		MemberOfUnit ha = new MemberOfUnit();
		ha.addEvaluator(new OR());
	}
	
	@Test
	public void testAddCfgVal() {
		MemberOfUnit mou = new MemberOfUnit();
		mou.addUnit("5");
		Values av = mou.cfgUnits.get(0);
		Assert.assertTrue(av != null);
		
		mou.addUnit("5"); // should not result in another insertion.
		Values av2 = mou.cfgUnits.get(0);
		Assert.assertSame(av2, av);
	}

	@Test
	public void testValues() {
		Values v = new Values("1");
		Assert.assertEquals(v.hashCode(), (UserHeaderNames.UNIT_PREFIX + "1" 
	            + UserHeaderNames.UNITS_DELIMITER).hashCode());
		
		v.unitPattern = null;
		
		Assert.assertEquals(v.hashCode(), 0);
		Assert.assertFalse(v.equals(null));
		Assert.assertFalse(v.equals(new Object()));
		
		Values null1 = new Values("");
		null1.unitPattern = null;
		Values null2 = new Values("");
		null2.unitPattern = null;
		Assert.assertTrue(null1.equals(null2));
		
		Values real = new Values("1");
		Assert.assertFalse(null1.equals(real));
		Values real2 = new Values("1");
		
		Assert.assertTrue(real.equals(real2));
	}
}
