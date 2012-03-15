package org.lds.sso.appwrap;

import org.lds.sso.appwrap.identity.UserManager;
import org.testng.Assert;
import org.testng.annotations.Test;

public class EntitlementsManagerTest {

    @Test
    public void test() throws Exception {
        System.setProperty("is-employee-syntax", "<Attribute name='emp' operation='equals' value='yes'/>");
        System.setProperty("is-524735-member-syntax", "<Attribute name='units' operation='equals' value='*u524735/*'/>");
        System.setProperty("is-cdol-syntax", 
                "<OR>\r\n" + 
                "<Attribute name='position' operation='equals' value='p1/*'/>\r\n" + 
                "<Attribute name='position' operation='equals' value='p4/*'/>\r\n" + 
                "<Attribute name='position' operation='equals' value='p52/*'/>\r\n" + 
                "<Attribute name='position' operation='equals' value='p57/*'/>\r\n" + 
                "</OR>");
    	System.getProperties().remove("non-existent-sys-prop");
        
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias is-employee=system:is-employee-syntax?>"
            + "<?alias is-cdol=system:is-cdol-syntax?>"
            + "<?alias is-in-524735=system:is-524735-member-syntax?>"
        	+ "<?system-alias usr-src-xml=non-existent-sys-prop default="
            + "\""
            + "  <users>"
            + "   <user name='aaa' pwd='password1'>"
            + "    <att name='emp' value='yes'/>" // is employee, not bishop
            + "   </user>"
            + "   <user name='bbb' pwd='password1'>"
            + "    <att name='position' value='p4/7u56030/5u524735/1u791040/'/>" // is NOT employee, is bishop
            + "    <att name='units' value='7u56030/5u524735/1u791040/'/>"
            + "   </user>"
            + "   <user name='ngiwb1' pwd='password1'>"
            + "    <att name='position' value='p4/7u56030/5u524735/1u791040/'/>" // bishop
            + "   </user>"
            + "  </users>"
        	+ "\"?>"
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
            + " <user-source type='xml'>xml={{usr-src-xml}}</user-source>"
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
