package org.lds.sso.appwrap.opensso;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UnitsChainTranslatorTest {

	@Test
	public void test() {
		Assert.assertEquals(UnitsChainInjector.stripOutTypeChars("W123:S456:A789"), "123:456:789");
		Assert.assertEquals(UnitsChainInjector.stripOutTypeChars("S456:A789"), "456:789");
		Assert.assertEquals(UnitsChainInjector.stripOutTypeChars("A789"), "789");
	}
}
