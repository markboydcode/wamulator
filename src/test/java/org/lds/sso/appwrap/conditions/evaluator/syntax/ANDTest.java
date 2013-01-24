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

public class ANDTest extends TestBaseClass 
{
	@Test
	public void testHasPosition_AND_NOTDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "(&(employeeStatus=A)(!(ldsPosv2=p3/*)))", true);
		
		// stuff evaluator will go after
		String[] posAtts = new String[] {new String("p4/7u345/5u897/1u2001/")};
		String[] empAtts = new String[] {new String("A")};
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("ldsposv2")).andReturn(posAtts);
        EasyMock.expect(usr.getAttribute("employeestatus")).andReturn(empAtts);
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

		Assert.assertEquals(br.readLine(), "T  <AND debug='true'>");
		Assert.assertEquals(br.readLine(), "T    <Attribute debug='true' name='employeeStatus' operation='EQUALS' value='A'/>  user has attribute that matches value, actual: A");
		Assert.assertEquals(br.readLine(), "T    <NOT debug='true'>");
		Assert.assertEquals(br.readLine(), "F      <Attribute debug='true' name='ldsPosv2' operation='EQUALS' value='p3/*'/>  user does not have attribute that matches value, actual: p4/7u345/5u897/1u2001/");
		Assert.assertEquals(br.readLine(), "     </NOT>");
		Assert.assertEquals(br.readLine(), "   </AND>");
		Assert.assertEquals(br.readLine(), "----- env -----");
		Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasPosition_AND_DebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "(&(ldsPosv2=p3/*)(employeeStatus=A))", true);

		// stuff evaluator will go after
		String[] posAtts = new String[] {new String("p3/7u345/5u897/1u2001/")};
		String[] empAtts = new String[] {new String("A")};
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("ldsposv2")).andReturn(posAtts);
        EasyMock.expect(usr.getAttribute("employeestatus")).andReturn(empAtts);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have position 3 and should be an employee");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);

		Assert.assertEquals(br.readLine(), "T  <AND debug='true'>");
		Assert.assertEquals(br.readLine(), "T    <Attribute debug='true' name='ldsPosv2' operation='EQUALS' value='p3/*'/>  user has attribute that matches value, actual: p3/7u345/5u897/1u2001/");
		Assert.assertEquals(br.readLine(), "T    <Attribute debug='true' name='employeeStatus' operation='EQUALS' value='A'/>  user has attribute that matches value, actual: A");
		Assert.assertEquals(br.readLine(), "   </AND>");
		Assert.assertEquals(br.readLine(), "----- env -----");
		Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "---------------");
	}

	@Test
	public void testHasPosition_AND_notDebugOutput() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "(&(ldsPosv2=p12/*)(employeeStatus=A))", false);
		
		// stuff evaluator will go after
		String[] posAtts = new String[] {new String("p12/7u345/5u897/1u2001/")};
		String[] empAtts = new String[] {new String("T")};
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("ldsposv2")).andReturn(posAtts);
        EasyMock.expect(usr.getAttribute("employeestatus")).andReturn(empAtts);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

		Map<String,String> env = new TreeMap<String,String>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should have position 12 and should be an employee");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);

		Assert.assertEquals(br.readLine(), null);
	}

	@Test
	public void test_debug_with_hashtable_in_env() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "(&(ldsPosv2=p3/*)(employeeStatus=A))", true);
		
		// stuff evaluator will go after
		String[] posAtts = new String[] {new String("p3/7u345/5u897/1u2001/")};
		String[] empAtts = new String[] {new String("A")};
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("ldsposv2")).andReturn(posAtts);
        EasyMock.expect(usr.getAttribute("employeestatus")).andReturn(empAtts);
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

        Map<String, Object> randomStuff = new TreeMap<String, Object>();
		randomStuff.put("random1", "345");
		randomStuff.put("random2", new Integer(345));
		
		Map<String, Object> env = new TreeMap<String, Object>();
		env.put("somevar1", "somevalue1");
		env.put("somevar2", "somevalue2");
		env.put("somevar3", "somevalue3");
		env.put("somevar4", randomStuff);

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have position 3 and should be an employee");

		String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);

		Assert.assertEquals(br.readLine(), "T  <AND debug='true'>");
		Assert.assertEquals(br.readLine(), "T    <Attribute debug='true' name='ldsPosv2' operation='EQUALS' value='p3/*'/>  user has attribute that matches value, actual: p3/7u345/5u897/1u2001/");
		Assert.assertEquals(br.readLine(), "T    <Attribute debug='true' name='employeeStatus' operation='EQUALS' value='A'/>  user has attribute that matches value, actual: A");
		Assert.assertEquals(br.readLine(), "   </AND>");
		Assert.assertEquals(br.readLine(), "----- env -----");
		Assert.assertEquals(br.readLine(), "somevar1 = somevalue1");
		Assert.assertEquals(br.readLine(), "somevar2 = somevalue2");
		Assert.assertEquals(br.readLine(), "somevar3 = somevalue3");
		Assert.assertEquals(br.readLine(), "somevar4 = {random1=345, random2=345}");
		Assert.assertEquals(br.readLine(), "---------------");
		Assert.assertEquals(br.readLine(), null);
	}

}
