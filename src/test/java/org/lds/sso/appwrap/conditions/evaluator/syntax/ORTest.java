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

public class ORTest extends TestBaseClass {
	
	@Test
	public void testHasPosition_OR_DebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "(|(employeeStatus=A)(ldsPosv2=p3/*))", true);
		
		// stuff evaluator will go after
		String[] posAtts = new String[] {};
		String[] empAtts = new String[] {"A"};
		User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("ldsposv2")).andReturn(posAtts);
        EasyMock.expect(usr.getAttribute("employeestatus")).andReturn(empAtts);
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

		Assert.assertEquals(br.readLine(), "T  <OR debug='true'>");
		Assert.assertEquals(br.readLine(), "T    <Attribute debug='true' name='employeeStatus' operation='EQUALS' value='A'/>  user has attribute that matches value, actual: A");
		Assert.assertEquals(br.readLine(), "   </OR>");
		Assert.assertEquals(br.readLine(), "----- env -----");
		Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "---------------");
        Assert.assertEquals(br.readLine(), null);
	}
	@Test
	public void testHasPosition_OR_notDebugOutput() throws Exception {
        IEvaluator ev = eng.getEvaluator("test-evaluator", "(|(employeeStatus=A)(ldsPosv2=p4/*))");
		
		// stuff evaluator will go after
		String[] posAtts = new String[] {};
		String[] empAtts = new String[] {"T"};
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("ldsposv2")).andReturn(posAtts);
        EasyMock.expect(usr.getAttribute("employeestatus")).andReturn(empAtts);
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
		Assert.assertEquals(br.readLine(), null);
	}
}
