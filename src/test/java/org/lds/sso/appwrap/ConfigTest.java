package org.lds.sso.appwrap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ConfigTest {

    /**
     * Test the various cookie header format: netscape, rfc2109, rfc2965
     */
    @Test
    public void test_getTokenFromCookie() {
        Config cfg = new Config();
        cfg.setCookieName("lds-policy");
        Assert.assertEquals(cfg.getTokenFromCookie("lds-policy=user1-13165790"), "user1-13165790");
        Assert.assertEquals(cfg.getTokenFromCookie("lds-policy=\"user1-13165790\""), "user1-13165790");
        Assert.assertEquals(cfg.getTokenFromCookie("Cookie: $Version=\"1\"; lds-policy=\"user1-13165790\"; $Path=\"/\"; $Domain=\".lds.org\""), "user1-13165790");
        Assert.assertEquals(cfg.getTokenFromCookie("Cookie: $Version=\"1\"; lds-policy=\"user1-13165790\"; $Path=\"/\"; $Domain=\".lds.org\"; $Port"), "user1-13165790");
    }
}
