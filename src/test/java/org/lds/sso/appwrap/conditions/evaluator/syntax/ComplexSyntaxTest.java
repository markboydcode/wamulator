package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.identity.User;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ComplexSyntaxTest extends TestBaseClass {

	@Test
	public void testComplex() throws EvaluationException, IOException {
		IEvaluator eval = eng.getEvaluator("test-evaluator", "(&(|(acctid=0000000003696776)(acctid=3400000001743018)(acctid=3445200000072567)(acctid=3454270000000090)(acctid=3455950000000491)(units=3516069)(units=4327144)(units=473890)(units=562233521))(|(position=p1/*)(position=p1/*)(position=p4/*)(position=p57/*)))");
		
		// stuff evaluator will go after
		String[] pos = new String[] {"p100/7u56030/5u524735/1u791040/", "p57/7u56030/5u524735/1u791040/", "p300/7u56030/5u524735/1u791040/"};
		String[] acctid = new String[] {"3454270000000090"};

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("units")).andReturn(new String[] {"/7u56030/5u524735/1u791040/"}).atLeastOnce();
        EasyMock.expect(usr.getAttribute("acctid")).andReturn(acctid).atLeastOnce();
        EasyMock.expect(usr.getAttribute("position")).andReturn(pos).atLeastOnce();
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);
		
		Map<String,String> env = new TreeMap<String,String>();

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(eval.isConditionSatisfied(ctx), "should have position 57 and correct lds account id");
	}
}
