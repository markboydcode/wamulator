package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.NvPair;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.identity.User;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ComplexSyntaxTest extends TestBaseClass {

	@Test
	public void testComplex() throws EvaluationException, IOException {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("ComplexSyntaxTest.xml");
		DataInputStream dis = new DataInputStream(is);
		byte[] bytes = new byte[dis.available()];
		dis.read(bytes);
		String syntax = new String(bytes);
		IEvaluator eval = eng.getEvaluator("test-evaluator", syntax);
		
		// stuff evaluator will go after
		NvPair[] pos = new NvPair[] {new NvPair("position", "p100/7u56030/5u524735/1u791040/"),
				new NvPair("position", "p57/7u56030/5u524735/1u791040/"),
				new NvPair("position", "p300/7u56030/5u524735/1u791040/")};
		NvPair[] acctid = new NvPair[] {new NvPair("acctid", "3454270000000090")};

        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getAttribute("units")).andReturn(new NvPair[] {new NvPair("units", "/7u56030/5u524735/1u791040/")}).atLeastOnce();
        EasyMock.expect(usr.getAttribute("acctid")).andReturn(new NvPair[] {new NvPair("acctid", "3454270000000090")}).atLeastOnce();
        EasyMock.expect(usr.getAttribute("position")).andReturn(pos).atLeastOnce();
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);
		
		Map<String,String> env = new TreeMap<String,String>();

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(eval.isConditionSatisfied(ctx), "should have position 57 and correct lds account id");
	}
}
