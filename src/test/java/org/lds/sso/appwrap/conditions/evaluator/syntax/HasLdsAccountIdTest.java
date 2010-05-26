package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.TreeMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HasLdsAccountIdTest extends TestBaseClass {
	
	@Test
	public void testHasLdsAccountIdDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasLdsAccountId" +
				" id='12345'" +
				" username='ngienglishbishop'" +
				" debug-user='ngienglishbishop'/>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.LDS_ACCOUNT_ID)).andReturn("12345");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have ldsAccountId 12345");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "T  <HasLdsAccountId debug-user='ngienglishbishop' id='12345' username='ngienglishbishop'/>  user has the id 12345");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasLdsAccountIdNotDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasLdsAccountId" +
				" id='12345'" +
				" username='ngienglishbishop'" +
				" debug-user='ngienglishbishop'/>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.LDS_ACCOUNT_ID)).andReturn("777");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have ldsAccountId 12345");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <HasLdsAccountId debug-user='ngienglishbishop' id='12345' username='ngienglishbishop'/>  user does not have the id");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasLdsAccountIdNotAnyDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasLdsAccountId" +
				" id='12345'" +
				" username='ngienglishbishop'" +
				" debug-user='ngienglishbishop'>" +
				" <LdsAccount id='888'/>" +
				"</HasLdsAccountId>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.LDS_ACCOUNT_ID)).andReturn("777");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have ldsAccountId 12345");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <HasLdsAccountId debug-user='ngienglishbishop' id='12345' username='ngienglishbishop'/>  user does not have any of the ids");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	
	@Test
	public void testHasLdsAccountIdEmptyDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasLdsAccountId" +
				" id='12345'" +
				" username='ngienglishbishop'" +
				" debug-user='ngienglishbishop'/>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.LDS_ACCOUNT_ID)).andReturn(UserHeaderNames.EMPTY_VALUE_INDICATOR);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have ldsAccountId 12345");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <HasLdsAccountId debug-user='ngienglishbishop' id='12345' username='ngienglishbishop'/>  user id = empty");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasLdsAccountIdNotInSessionDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasLdsAccountId" +
				" id='12345'" +
				" username='ngienglishbishop'" +
				" debug-user='ngienglishbishop'/>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.LDS_ACCOUNT_ID)).andReturn(null);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have ldsAccountId 12345");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <HasLdsAccountId debug-user='ngienglishbishop' id='12345' username='ngienglishbishop'/>  user id not in session");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasLdsAccountIdAnyDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasLdsAccountId" +
				" id='*'" +
				" username='ngienglishbishop'" +
				" debug-user='ngienglishbishop'/>");;
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.LDS_ACCOUNT_ID)).andReturn("12345");
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have an ldsAccountId");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "T  <HasLdsAccountId debug-user='ngienglishbishop' id='*' username='ngienglishbishop'/>  user has an id");
		Assert.assertEquals(br.readLine(), "----- env -----");
		Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
        Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}
}
