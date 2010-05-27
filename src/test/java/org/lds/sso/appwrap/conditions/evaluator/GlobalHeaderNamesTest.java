package org.lds.sso.appwrap.conditions.evaluator;

import org.testng.Assert;
import org.testng.annotations.Test;

public class GlobalHeaderNamesTest {

    @Test
    public void test_signMeInDetection() {
        Assert.assertTrue(GlobalHeaderNames.detectSignMeIn(GlobalHeaderNames.SIGNIN_VALUE));
        Assert.assertTrue(GlobalHeaderNames.detectSignMeIn(GlobalHeaderNames.SIGNIN_VALUE + "&a=b"));
        Assert.assertTrue(GlobalHeaderNames.detectSignMeIn("a=b&" + GlobalHeaderNames.SIGNIN_VALUE));
        Assert.assertTrue(GlobalHeaderNames.detectSignMeIn("a=b&" + GlobalHeaderNames.SIGNIN_VALUE + "&c=d"));
        Assert.assertFalse(GlobalHeaderNames.detectSignMeIn("a=b&c=d"));
        Assert.assertFalse(GlobalHeaderNames.detectSignMeIn(null));
    }

    @Test
    public void test_signMeOutDetection() {
        Assert.assertTrue(GlobalHeaderNames.detectSignMeOut(GlobalHeaderNames.SIGNOUT_VALUE));
        Assert.assertTrue(GlobalHeaderNames.detectSignMeOut(GlobalHeaderNames.SIGNOUT_VALUE + "&a=b"));
        Assert.assertTrue(GlobalHeaderNames.detectSignMeOut("a=b&" + GlobalHeaderNames.SIGNOUT_VALUE));
        Assert.assertTrue(GlobalHeaderNames.detectSignMeOut("a=b&" + GlobalHeaderNames.SIGNOUT_VALUE + "&c=d"));
        Assert.assertFalse(GlobalHeaderNames.detectSignMeOut("a=b&c=d"));
        Assert.assertFalse(GlobalHeaderNames.detectSignMeOut(null));
    }
}
