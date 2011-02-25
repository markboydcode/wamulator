package org.lds.sso.appwrap.conditions.evaluator.syntax;

import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: gmdayley
 * Date: 2/25/11
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class AttributeTest extends TestBaseClass {

    @Test
	public void testAttributeOperationEquals() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='equals' value='test'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.hasAttributeValue("test", "test")).andReturn(true);
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have attribute called test with value test");

	}


    @Test
	public void testAttributeOperationExists() throws Exception {
		IEvaluator ev = eng.getEvaluator("test-evaluator", "<Attribute name='test' operation='exists' value='test'/>");

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.hasAttributeValue("test", "test")).andReturn(true);
        EasyMock.replay(usr);

        Map<String,String> env = new TreeMap<String,String>();

        EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(ev.isConditionSatisfied(ctx), "should have attribute called test");
	}

}
