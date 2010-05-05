package org.lds.sso.appwrap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class EntitlementsManagerTest {

    @Test
    public void test() throws Exception {
        System.setProperty("is-employee-syntax", "<IsEmployee/>");
        
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias is-employee=system:is-employee-syntax?>"
            + "<config console-port='88' proxy-port='45'>"
            + "  <users>"
            + "   <user name='aaa' pwd='password1'>"
            + "    <sso-header name='policy-dn' value='cn=jeremy, ou=int, o=lds'/>" // is employee
            + "   </user>"
            + "   <user name='bbb' pwd='password1'>"
            + "    <sso-header name='policy-dn' value='cn=jeremy, ou=ext, o=lds'/>" // is NOT employee
            + "   </user>"
            + "  </users>"
            + "  <sso-entitlements policy-domain='lds.org'>"
            + "   <allow action='GET' urn='/leader/focus/page' condition='{{is-employee}}'/>" // employees only
            + "   <allow action='GET' urn='/leader/list/page'/>" // all users
            + "  </sso-entitlements>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        EntitlementsManager emgr = cfg.getEntitlementsManager();
        UserManager umgr = cfg.getUserManager();
        Assert.assertTrue(emgr.isAllowed("GET", "lds.org/leader/focus/page", umgr.getUser("aaa")));
        Assert.assertFalse(emgr.isAllowed("GET", "lds.org/leader/focus/page", umgr.getUser("bbb")));
        Assert.assertTrue(emgr.isAllowed("GET", "lds.org/leader/list/page", umgr.getUser("aaa")));
        Assert.assertTrue(emgr.isAllowed("GET", "lds.org/leader/list/page", umgr.getUser("bbb")));
    }
}
