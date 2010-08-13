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
            + "  <sso-traffic>"
            + "   <by-site host='local.lds.org' port='80'>"
            + "    <entitlements>"
            // note that entitlement 3 contains entitlement 1's urn hierarchically
            // hence, 1's urn will also be granted to bishops
            + "     <allow action='GET' urn='/leader/focus/page' condition='{{is-employee}}'/>" // employees only
            + "     <allow action='GET' urn='/leader/bishop/page' condition='{{is-cdol}}'/>" // only bishops
            + "     <allow action='GET' urn='/leader/focus' condition='{{is-cdol}}'/>" // only bishops
            + "     <allow action='GET' urn='/leader/ward/page' condition='{{is-in-524735}}'/>" // only members of unit 524735
            + "    </entitlements>"
            + "   </by-site>"
            + "  </sso-traffic>"
            + "  <users>"
            + "   <user name='aaa' pwd='password1'>"
            + "    <sso-header name='policy-dn' value='cn=jeremy, ou=int, o=lds'/>" // is employee, not bishop
            + "   </user>"
            + "   <user name='bbb' pwd='password1'>"
            + "    <sso-header name='policy-dn' value='cn=jeremy, ou=ext, o=lds'/>" // is NOT employee, is bishop
            + "    <sso-header name='policy-ldspositions' value='p4/7u56030/5u524735/1u791040/'/>"
            + "    <sso-header name='policy-ldsunits' value='7u56030/5u524735/1u791040/'/>"
            + "   </user>"
            + "   <user name='ngiwb1' pwd='password1'>"
            + "    <sso-header name='policy-ldspositions' value='p4/7u56030/5u524735/1u791040/'/>" // bishop
            + "   </user>"
            + "  </users>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        EntitlementsManager emgr = cfg.getEntitlementsManager();
        UserManager umgr = cfg.getUserManager();
        Assert.assertTrue(emgr.isAllowed("local.lds.org", "GET", "/leader/focus", umgr.getUser("ngiwb1"), null));
        Assert.assertTrue(emgr.isAllowed("local.lds.org", "GET", "/leader/focus/page", umgr.getUser("aaa"), null));
        Assert.assertTrue(emgr.isAllowed("local.lds.org", "GET", "/leader/focus/page", umgr.getUser("bbb"), null));
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/leader/list/page", umgr.getUser("aaa"), null));
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/leader/list/page", umgr.getUser("bbb"), null));
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/leader/bishop/page", umgr.getUser("aaa"), null));
        Assert.assertTrue(emgr.isAllowed("local.lds.org", "GET", "/leader/bishop/page", umgr.getUser("bbb"), null));
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/leader/ward/page", umgr.getUser("aaa"), null));
        Assert.assertTrue(emgr.isAllowed("local.lds.org", "GET", "/leader/ward/page", umgr.getUser("bbb"), null));
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/LINK/local.lds.org_leader_ward_page", umgr.getUser("aaa"), null));
    }

    @Test
    public void testOldFormat() throws Exception {
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-header name='policy-service-url' value='irrelevant'/>"
            + " <sso-entitlements policy-domain='lds.org'>"
            + "  <allow action='GET' urn='/leader/focus/page' condition='{{is-employee}}'/>" // employees only
            + " </sso-entitlements>"
            + "</config>";
        Config cfg = new Config();
        try {
            XmlConfigLoader2.load(xml);
            Assert.fail("Should have thrown an IllegalArgumentException");
        }
        catch(Exception e) {
            if (e.getCause() == null || e.getCause().getClass() != IllegalArgumentException.class) {
                Assert.fail("Should have thrown an IllegalArgumentException", e);
            }
        }
    }
}
