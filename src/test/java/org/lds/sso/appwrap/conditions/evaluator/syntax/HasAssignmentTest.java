package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.lds.sso.appwrap.conditions.evaluator.syntax.HasAssignment.AssignmentValues;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HasAssignmentTest extends TestBaseClass {
	
	@Test
	public void verifyEmptyPositionOrUnitNoCallToAddCfgVal() throws EvaluationException {
		HasAssignment h = new HasAssignment();
		h.init("syntax", new HashMap());
		Assert.assertEquals(h.cfgVals.size(), 0);
	}
	
	@Test
	public void testHasAssignmentDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("<HasAssignment position='1' unit='555' debug-user='ngienglishbishop'/>");
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn("p1/5u111/1u555/");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have position 1 in unit 555");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "T  <HasAssignment debug-user='ngienglishbishop' position='1' unit='555'/>  user has assignment 1 in unit 555");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasAssignmentNotDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("<HasAssignment position='1' unit='555' debug-user='ngienglishbishop'/>");
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn("p20/5u111/1u555");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have assignment");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <HasAssignment debug-user='ngienglishbishop' position='1' unit='555'/>  user does not have the assignment");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasNoAssignmentDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("<HasAssignment position='1' unit='555' debug-user='ngienglishbishop'/>");
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have assignment");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <HasAssignment debug-user='ngienglishbishop' position='1' unit='555'/>  user does not have any assignments");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasAssignmentNotAnyDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasAssignment position='1' unit='555' " +
				" debug-user='ngienglishbishop'>" +
				" <Assignment position='4' unit='555'/>" +
				" <Assignment position='6' unit='555'/>" +
				"</HasAssignment>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn("p20/5u111/1u555:p30/5u66/1u77:p40/5u222/1u333");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have any of the assignments");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <HasAssignment debug-user='ngienglishbishop' position='1' unit='555'/>  user has none of the assignments");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	
	@Test
	public void testHasAssignmentEmptySessionValDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasAssignment position='1' unit='555' " +
				" debug-user='ngienglishbishop'>" +
				" <Assignment position='4' unit='555'/>" +
				" <Assignment position='6' unit='555'/>" +
				"</HasAssignment>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn(null);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have any of the assignments");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <HasAssignment debug-user='ngienglishbishop' position='1' unit='555'/>  assignments not in session");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testBadValuesDontGetAddedToCfg() throws EvaluationException {
		HasAssignment ha = new HasAssignment();
		ha.init("syntax", new HashMap<String,String>());
		Assert.assertEquals(ha.cfgVals.size(), 0);
		
		Map<String,String> vals = new HashMap<String,String>();
		vals.put("position", "");
		ha.init("syntax", vals);
		Assert.assertEquals(ha.cfgVals.size(), 0);
		
		vals = new HashMap<String,String>();
		vals.put("position", "5");
		vals.put("unit", null);
		ha.init("syntax", vals);
		Assert.assertEquals(ha.cfgVals.size(), 0);
		
		vals = new HashMap<String,String>();
		vals.put("position", "5");
		vals.put("unit", "");
		ha.init("syntax", vals);
		Assert.assertEquals(ha.cfgVals.size(), 0);
	}
	
	@Test(expectedExceptions = {EvaluationException.class})	
	public void testInvalidEvaluatorNotAllowed() throws EvaluationException {
		HasAssignment ha = new HasAssignment();
		ha.addEvaluator(new OR());
	}
	
	@Test
	public void testAddCfgVal() {
		HasAssignment ha = new HasAssignment();
		ha.addCfgVal("5", "10");
		AssignmentValues av = ha.cfgVals.get(0);
		Assert.assertTrue(av != null);
		
		ha.addCfgVal("5", "10"); // should result in another insertion.
		AssignmentValues av2 = ha.cfgVals.get(0);
		Assert.assertSame(av2, av);
		
	}

	@Test
	public void testAssignmentValues() {
		AssignmentValues av = new AssignmentValues("1", "50");
		Assert.assertEquals(av.hashCode(), av.id.hashCode());
		
		av.id = null;
		
		Assert.assertEquals(av.hashCode(), 0);
		Assert.assertFalse(av.equals(new Object()));
		
		AssignmentValues null1 = new AssignmentValues("","");
		null1.id = null;
		AssignmentValues null2 = new AssignmentValues("","");
		null2.id = null;
		Assert.assertTrue(null1.equals(null2));
		
		AssignmentValues real = new AssignmentValues("3", "345");
		Assert.assertFalse(null1.equals(real));
		Assert.assertFalse(real.equals(null1));
		
	}
}
