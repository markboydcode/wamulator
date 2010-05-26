package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.LogicalSyntaxEvaluationEngine;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ORTest extends TestBaseClass {
	
	@Test
	public void testHasPosition_OR_DebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<OR debug-user='ngienglishbishop'>" +
				" <HasPosition id='3'/>" +
				" <IsEmployee/>" +
				"</OR>");;
		
		// stuff evaluator will go after
		User usr = EasyMock.createMock(User.class);
		EasyMock.expect(usr.getProperty(UserHeaderNames.DN)).andReturn("id=ngiEnglishBishop,ou=int,ou=people,o=lds");
		EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn(null);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
		EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be an employee");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);

		Assert.assertEquals(br.readLine(), "T  <OR debug-user='ngienglishbishop'>");
		Assert.assertEquals(br.readLine(), "T    <IsEmployee/>  user has ou=int in their dn");
		Assert.assertEquals(br.readLine(), "   </OR>");
		Assert.assertEquals(br.readLine(), "----- env -----");
		Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
        Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}
	@Test
	public void testHasPosition_OR_notDebugOutput() throws Exception {
        IEvaluator ev = eng.getEvaluator(
				"<OR debug-user='ngienglishbishop'>" +
				" <HasPosition id='3'/>" +
				" <IsEmployee/>" +
				"</OR>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.DN)).andReturn("id=ngiEnglishBishop,ou=ext,ou=people,o=lds");
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn(null);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not be an employee not have position 3");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);

		// for OR if no criteria were met then we simplify output and don't 
		// record all nested evaluator output
		Assert.assertEquals(br.readLine(), "F  <OR debug-user='ngienglishbishop'>");
		Assert.assertEquals(br.readLine(), "   </OR>");
		Assert.assertEquals(br.readLine(), "----- env -----");
		Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
        Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}
}
