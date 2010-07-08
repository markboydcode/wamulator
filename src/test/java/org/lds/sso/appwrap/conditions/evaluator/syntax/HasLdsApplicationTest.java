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
import org.lds.sso.appwrap.NvPair;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HasLdsApplicationTest extends TestBaseClass {
	
	@Test
	public void testHasLdsApplication_DebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasLdsApplication" +
				" value='app-2'" +
				" debug-user='bob'/>");;
		
		// stuff evaluator will go after
        NvPair[] apps = new NvPair[] { 
                new NvPair(User.LDSAPPS_ATT, "app-1"),
                new NvPair(User.LDSAPPS_ATT, "app-2"),
                new NvPair(User.LDSAPPS_ATT, "app-3") };
				
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttributes()).andReturn(apps);
        EasyMock.expect(usr.getUsername()).andReturn("bob");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have ldsApplication app-2");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "T  <HasLdsApplication debug-user='bob' value='app-2'/>  user has value app-2");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasLdsApplicationNot_DebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasLdsApplication" +
				" value='app-51'" +
				" debug-user='frank'/>");;
		
		// stuff evaluator will go after
		        NvPair[] apps = new NvPair[] { 
		                new NvPair(User.LDSAPPS_ATT, "app-1"),
		                new NvPair(User.LDSAPPS_ATT, "app-2"),
		                new NvPair(User.LDSAPPS_ATT, "app-3") };
		                
        User usr1 = EasyMock.createMock(User.class);
        EasyMock.expect(usr1.getAttributes()).andReturn(apps);
        EasyMock.expect(usr1.getUsername()).andReturn("frank");
        EasyMock.replay(usr1);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr1, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have ldsApplication app-51");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <HasLdsApplication debug-user='frank' value='app-51'/>  user does not have the value");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasLdsApplication_noneDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator(
				"<HasLdsApplication" +
				" value='app-51'" +
				" debug-user='sally'/>");;
		
		// stuff evaluator will go after
		NvPair[] atts = new NvPair[] {};
				
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttributes()).andReturn(atts);
        EasyMock.expect(usr.getUsername()).andReturn("sally");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have ldsApplication app-51");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <HasLdsApplication debug-user='sally' value='app-51'/>  user has no ldsApplications");
		Assert.assertEquals(br.readLine(), "----- env -----");
        Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}
}
