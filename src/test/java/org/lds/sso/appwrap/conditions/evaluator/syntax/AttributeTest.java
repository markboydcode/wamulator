package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.NvPair;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: gmdayley
 * Date: 2/25/11
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class AttributeTest extends TestBaseClass {

    @Test
    public void testAttributeNotFoundInUser() throws Exception {
        IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='equals' value='test'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("test")).andReturn(null);
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
        Assert.assertFalse(ev.isConditionSatisfied(ctx), "should be false since user doesn't have attribute called test");

    }

    @Test
    public void testAttributeOperationEquals() throws Exception {
        IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='equals' value='test'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("test")).andReturn(new NvPair[]{new NvPair("test", "test")});
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
        Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have attribute called test with value test");

    }

    @Test
    public void testAttributeOperationEqualsMultiValued() throws Exception {
        IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='equals' value='test'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("test")).andReturn(new NvPair[]{new NvPair("test", "testAAA"), new NvPair("test", "test"), new NvPair("test", "testBBB")});
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
        Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have attribute called test with value test");

    }

    @Test
	public void testAttributeOperationEqualsWithWildcardTrue() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='equals' value='this * test' debug='true'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("test")).andReturn(new NvPair[]{new NvPair("test", "this is a super test")});
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have attribute called test with value that matches: this * test");

	}

    @Test
	public void testAttributeOperationEqualsWithEndingWildcardTrue() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='equals' value='this * test *' debug='true'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("test")).andReturn(new NvPair[]{new NvPair("test", "this is a super test don't you think")});
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have attribute called test with value that matches: this * test *");

	}

    @Test
	public void testAttributeOperationEqualsNotEndingWildcardFalse() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='equals' value='this * test'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("test")).andReturn(new NvPair[]{new NvPair("test", "this is a super test don't you think")});
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have attribute called test that matches: this * test");

	}
    @Test
	public void testAttributeOperationEqualsWithWildcardFalse() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='equals' value='this is not * test'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("test")).andReturn(new NvPair[]{new NvPair("test", "this is a super test")});
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertFalse(ev.isConditionSatisfied(ctx), "should not have attribute called test that matches: this is not * test");

	}

    @Test(expectedExceptions = {EvaluationException.class})
	public void testAttributeOperationNotSupported() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='bogus' value='this is not * test'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("test")).andReturn(new NvPair[]{new NvPair("test", "this is a super test")});
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
		ev.isConditionSatisfied(ctx);
	}

    @Test
	public void testAttributeOperationEqualsWithWildcardEscapedWildcard() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='equals' value='this is not \\* test'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("test")).andReturn(new NvPair[]{new NvPair("test", "this is not * test")});
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have attribute called test that matches: this is not \\* test");
	}


    @Test
	public void testAttributeOperationExists() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='exists' value='test'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.hasAttribute("test")).andReturn(true);
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have attribute called test");
	}

    @Test
	public void testAttributeOperationEqualsDebug() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='equals' value='this is not \\* test' debug='true'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("test")).andReturn(new NvPair[]{new NvPair("test", "this is not * test")});
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have attribute called test with value test");

        String output = swa.getLogOutput();
		StringReader sr = new StringReader(output);
		BufferedReader br = new BufferedReader(sr);

		Assert.assertEquals(br.readLine(), "T  <Attribute debug='true' name='test' operation='equals' value='this is not \\* test'/>  user has attribute that matches value, actual: this is not * test");
	}


    @Test(expectedExceptions = {EvaluationException.class})
	public void testAttributeValueRequired() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='equals'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("test")).andReturn(new NvPair[]{new NvPair("test", "this is not * test")});
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
		ev.isConditionSatisfied(ctx);
	}
}
