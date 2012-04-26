package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.identity.User;
import org.testng.Assert;
import org.testng.annotations.Test;

public class NOTTest extends TestBaseClass {

    @Test
	public void testHasLdsPositionNOTDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", 
				"<NOT debug-user='ngienglishbishop'>" +
				" <Attribute name='position' operation='EQUALS' value='p4*'/>" +
				"</NOT>");
		
		// stuff evaluator will go after
		String[] posAtts = new String[] {"p4/7u345/5u897/1u2001/"};
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("position")).andReturn(posAtts);
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
		Assert.assertEquals(br.readLine(), "T    <Attribute name='position' operation='EQUALS' value='p4*'/>  user has attribute that matches value, actual: p4/7u345/5u897/1u2001/");
		Assert.assertEquals(br.readLine(), "   </NOT>");
		Assert.assertEquals(br.readLine(), "----- env -----");
		Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
        Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

    @Test
	public void testHasLdsPositionNOTNOTDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", 
				"<NOT debug-user='ngienglishbishop'>" +
				" <NOT>" +
				"  <Attribute name='position' operation='EQUALS' value='p4*'/>" +
				" </NOT>" +
				"</NOT>");
		
		// stuff evaluator will go after
		String[] posAtts = new String[] {"p4/7u345/5u897/1u2001/"};
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("position")).andReturn(posAtts);
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
		
		Assert.assertEquals(br.readLine(), "T  <NOT debug-user='ngienglishbishop'>");
		Assert.assertEquals(br.readLine(), "F    <NOT>");
		Assert.assertEquals(br.readLine(), "T      <Attribute name='position' operation='EQUALS' value='p4*'/>  user has attribute that matches value, actual: p4/7u345/5u897/1u2001/");
		Assert.assertEquals(br.readLine(), "     </NOT>");
		Assert.assertEquals(br.readLine(), "   </NOT>");
		Assert.assertEquals(br.readLine(), "----- env -----");
		Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
        Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

    @Test
	public void testDoesntHaveLdsPositionNOTNOTDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", 
				"<NOT debug-user='ngienglishbishop'>" +
				" <NOT>" +
				"  <Attribute name='position' operation='EQUALS' value='p10*'/>" +
				" </NOT>" +
				"</NOT>");
		
		// stuff evaluator will go after
		String[] posAtts = new String[] {"p4/7u345/5u897/1u2001/"};
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("position")).andReturn(posAtts);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "shouldn't have position 10");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);
		
		Assert.assertEquals(br.readLine(), "F  <NOT debug-user='ngienglishbishop'>");
		Assert.assertEquals(br.readLine(), "T    <NOT>");
		Assert.assertEquals(br.readLine(), "F      <Attribute name='position' operation='EQUALS' value='p10*'/>  user does not have attribute that matches value, actual: p4/7u345/5u897/1u2001/");
		Assert.assertEquals(br.readLine(), "     </NOT>");
		Assert.assertEquals(br.readLine(), "   </NOT>");
		Assert.assertEquals(br.readLine(), "----- env -----");
		Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
        Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}
}
