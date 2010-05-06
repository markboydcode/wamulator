package org.lds.sso.appwrap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class EntitlementsManagerTest {

    @Test
    public void test() throws Exception {
        System.setProperty("is-employee-syntax", "<IsEmployee/>");
        System.setProperty("is-524735-member-syntax", "<MemberOfUnit id='524735'/>");
        System.setProperty("is-cdol-syntax", 
                "<OR>\r\n" + 
                "<HasPosition id='1'/>\r\n" + 
                "<HasPosition id='4'/>\r\n" + 
                "<HasPosition id='52'/>\r\n" + 
                "<HasPosition id='57'/>\r\n" + 
                "</OR>");
        
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias is-employee=system:is-employee-syntax?>"
            + "<?alias is-cdol=system:is-cdol-syntax?>"
            + "<?alias is-in-524735=system:is-524735-member-syntax?>"
            + "<config console-port='88' proxy-port='45'>"
            + "  <users>"
            + "   <user name='aaa' pwd='password1'>"
            + "    <sso-header name='policy-dn' value='cn=jeremy, ou=int, o=lds'/>" // is employee, not bishop
            + "   </user>"
            + "   <user name='bbb' pwd='password1'>"
            + "    <sso-header name='policy-dn' value='cn=jeremy, ou=ext, o=lds'/>" // is NOT employee, is bishop
            + "    <sso-header name='policy-positions' value='p4/7u56030/5u524735/1u791040/'/>"
            + "    <sso-header name='policy-units' value='7u56030/5u524735/1u791040/'/>"
            + "   </user>"
            + "  </users>"
            + "  <sso-entitlements policy-domain='lds.org'>"
            + "   <allow action='GET' urn='/leader/focus/page' condition='{{is-employee}}'/>" // employees only
            + "   <allow action='GET' urn='/leader/list/page'/>" // all users
            + "   <allow action='GET' urn='/leader/bishop/page' condition='{{is-cdol}}'/>" // only bishops
            + "   <allow action='GET' urn='/leader/ward/page' condition='{{is-in-524735}}'/>" // only members of unit 524735
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
        Assert.assertFalse(emgr.isAllowed("GET", "lds.org/leader/bishop/page", umgr.getUser("aaa")));
        Assert.assertTrue(emgr.isAllowed("GET", "lds.org/leader/bishop/page", umgr.getUser("bbb")));
        Assert.assertFalse(emgr.isAllowed("GET", "lds.org/leader/ward/page", umgr.getUser("aaa")));
        Assert.assertTrue(emgr.isAllowed("GET", "lds.org/leader/ward/page", umgr.getUser("bbb")));
    }
}
