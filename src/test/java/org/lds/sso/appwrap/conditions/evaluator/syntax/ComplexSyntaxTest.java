package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.NvPair;
import org.lds.sso.appwrap.User;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
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
		IEvaluator eval = eng.getEvaluator(syntax);
		
		// stuff evaluator will go after
        User usr = EasyMock.createMock(User.class);
        EasyMock.expect(usr.getProperty(UserHeaderNames.DN)).andReturn("id=ngiwc1,ou=user,ou=people,o=lds").anyTimes();
        EasyMock.expect(usr.getProperty(UserHeaderNames.UNITS)).andReturn("/7u56030/5u524735/1u791040/");
        EasyMock.expect(usr.getAttributes()).andReturn(new NvPair[] {new NvPair(User.LDSAPPS_ATT, "3431968841690661")});
        EasyMock.expect(usr.getProperty(UserHeaderNames.POSITIONS)).andReturn("p57/7u56030/5u524735/1u791040/").anyTimes();
        EasyMock.expect(usr.getUsername()).andReturn("ngienglishbishop");
        EasyMock.replay(usr);
		
		Map<String,String> env = new TreeMap<String,String>();

		EvaluationContext ctx = new EvaluationContext(usr, env);
		Assert.assertTrue(eval.isConditionSatisfied(ctx), "should have position 57 and correct lds account id");
	}
}
