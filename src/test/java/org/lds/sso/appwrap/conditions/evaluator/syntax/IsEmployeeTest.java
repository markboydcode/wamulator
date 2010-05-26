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

public class IsEmployeeTest extends TestBaseClass {
    
	@Test
	public void testIsEmployeeDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<IsEmployee debug-user='ngienglishbishop'/>");
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.DN)).andReturn("id=ngiEnglishBishop,ou=int,ou=people,o=lds");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be employee");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);

		Assert.assertEquals(br.readLine(), "T  <IsEmployee debug-user='ngienglishbishop'/>  user has ou=int in their dn");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testIsEmployeeDnNotInSessionDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<IsEmployee debug-user='ngienglishbishop'/>");
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.DN)).andReturn(null);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not be employee");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);

		Assert.assertEquals(br.readLine(), "F  <IsEmployee debug-user='ngienglishbishop'/>  dn not in session");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testIsEmployeeNotDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<IsEmployee debug-user='ngienglishbishop'/>");
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.DN)).andReturn("id=ngiEnglishBishop,ou=ext,ou=people,o=lds");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new HashMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not be employee");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);

		Assert.assertEquals(br.readLine(), "F  <IsEmployee debug-user='ngienglishbishop'/>  user does not have ou=int in their dn");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testIsEmployeeNoDebugOutputForWrongUser() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<IsEmployee debug-user='someuser'/>");
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.DN)).andReturn("id=ngiEnglishBishop,ou=int,ou=people,o=lds");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should be employee");

		String output = swa.getLogOutput();
		// since wrong user no debug output should have been injected.
		Assert.assertEquals(output, "");
	}

}
