package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HasPositionTest extends TestBaseClass {

	@Test
	public void testHasPositionDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasPosition" +
				" id='4'" +
				" debug-user='ngienglishbishop'/>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn("p4/7u345/5u897/1u2001/");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have position 4");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "T  <HasPosition debug-user='ngienglishbishop' id='4'/>  user has position 4");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasLdsPositionNotDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasPosition" +
				" id='4'" +
				" debug-user='ngienglishbishop'/>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn("p111/7u345/5u897/1u2001/");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have position 4");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <HasPosition debug-user='ngienglishbishop' id='4'/>  user has none of the positions");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasLdsPositionsNotDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasPosition id='4' debug-user='ngienglishbishop'>" +
				" <Position id='20'/>" +
				" <Position id='30'/>" +
				"</HasPosition>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn("p111/7u345/5u897/1u2001/:p34/7u25/5u97/1u100/");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have position 4");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <HasPosition debug-user='ngienglishbishop' id='4'/>  user has none of the positions");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasLdsPositionNOTDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<NOT debug-user='ngienglishbishop'>" +
				" <HasPosition" +
				"  id='4'/>" +
				"</NOT>");
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn("p4/7u345/5u897/1u2001/");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have position 4");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <NOT debug-user='ngienglishbishop'>");
		Assert.assertEquals(br.readLine(), "T    <HasPosition id='4'/>  user has position 4");
		Assert.assertEquals(br.readLine(), "   </NOT>");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasPosition_AND_NOTDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<AND debug-user='ngienglishbishop'>" +
				" <NOT>" +
				"  <HasPosition id='3'/>" +
				" </NOT>" +
				" <IsEmployee/>" +
				"</AND>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn("p4/7u345/5u897/1u2001/");
        EasyMock.expect(usr.getProperty(UserHeaderNames.DN)).andReturn("id=ngiEnglishBishop,ou=int,ou=people,o=lds");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should not have position 3 and should be an employee");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);

		// for AND if all criteria were met then we simplify output and don't 
		// record all nested evaluator output
		Assert.assertEquals(br.readLine(), "T  <AND debug-user='ngienglishbishop'>");
		Assert.assertEquals(br.readLine(), "   </AND>");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

}
