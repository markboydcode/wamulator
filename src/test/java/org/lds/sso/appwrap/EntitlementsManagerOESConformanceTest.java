package org.lds.sso.appwrap;

import org.lds.sso.appwrap.identity.User;
import org.lds.sso.appwrap.identity.UserManager;
import org.testng.Assert;
import org.testng.annotations.Test;

public class EntitlementsManagerOESConformanceTest {

	// The WAMulator no longer emulates OES.
	
    //@BeforeClass
    public void load() throws Exception {
    	System.getProperties().remove("non-existent-sys-prop");
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
        	+ "<?system-alias usr-src-xml=non-existent-sys-prop default="
            + "\""
            + "  <users>"
            + "   <user name='clark' pwd='?'>"
            + "    <att name='apps' value='superman'/>"
            + "   </user>"
            + "   <user name='pres' pwd='?'>"
            + "    <att name='positions' value='p1/5u524735/1u791040/'/>"
            + "    <att name='units' value='7u56030/5u524735/1u791040/'/>"
            + "   </user>"
            + "   <user name='bish' pwd='?'>"
            + "    <att name='positions' value='p4/7u56030/5u524735/1u791040/'/>" 
            + "   </user>"
            + "  </users>"
        	+ "\"?>"
            + "<config console-port='88' proxy-port='45'>"

            + "  <conditions>"
            + "   <condition alias='super-user'>"
            + "    <Attribute name='apps' operation='equals' value='superman'/>"
            + "   </condition>"
            
            + "   <condition alias='bishop'>"
            + "     <Attribute name='positions' operation='equals' value='p4/*'/>" 
            + "   </condition>"
            
            + "   <condition alias='stake-pres'>"
            + "     <Attribute name='positions' operation='equals' value='p1/*'/>"  
            + "   </condition>"
            + "  </conditions>"

            + "  <sso-traffic>"
            + "   <by-site host='local.lds.org' port='80'>"
            + "    <entitlements>"
            + "     <allow action='POST' urn='/root/some/ward/page/'  condition='{{stake-pres}}'/>" 
            + "     <allow action='GET'  urn='/root/some/ward/page/'  condition='{{bishop}}'/>" 
            + "     <allow action='GET'  urn='/root/some/stake/page/' condition='{{stake-pres}}'/>"
            
            + "     <allow action='GET' urn='/root/ward/stake' condition='{{stake-pres}}'/>" 
            + "     <allow action='GET' urn='/root/ward' condition='{{bishop}}'/>"
            + "     <allow action='GET' urn='/' condition='{{super-user}}'/>" 
            + "    </entitlements>"
            + "   </by-site>"
            + "  </sso-traffic>"
            + "  <user-source type='xml'>xml={{usr-src-xml}}</user-source>"

            + "</config>";
        
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
    }
    
    //@Test
    public void test_entitlement_evaluation() throws Exception {
        Config cfg = Config.getInstance();
        
        EntitlementsManager em = cfg.getEntitlementsManager();
        UserManager um = cfg.getUserManager();
        User clark = um.getUser("clark");
        User bish = um.getUser("bish");
        User pres = um.getUser("pres");
        
        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/some/ward/page", clark, null));
        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/some/ward/page", bish, null));
        Assert.assertFalse(em.isAllowed("local.lds.org", "POST", "/root/some/ward/page", bish, null));
        Assert.assertTrue(em.isAllowed("local.lds.org", "POST", "/root/some/ward/page", pres, null));
        Assert.assertFalse(em.isAllowed("local.lds.org", "GET", "/root/some/ward/page", pres, null));

        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/some/stake/page", clark, null));
        Assert.assertFalse(em.isAllowed("local.lds.org", "GET", "/root/some/stake/page", bish, null));
        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/some/stake/page", pres, null));

        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/ward/stake", clark, null));
        // next one is true since evaluation is hierarchically traversed and once granted access at any level 
        // the user gets all levels below that. Hence bish gets /root/ward and therefore also gets this one.
        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/ward/stake", bish, null));
        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/ward/stake", pres, null));

        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/ward", clark, null));
        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/ward", bish, null));
        Assert.assertFalse(em.isAllowed("local.lds.org", "GET", "/root/ward", pres, null));
    }

    //@Test
    public void test_entitlement_evaluation_w_terminating_slash() throws Exception {
        Config cfg = Config.getInstance();
        
        EntitlementsManager em = cfg.getEntitlementsManager();
        UserManager um = cfg.getUserManager();
        User clark = um.getUser("clark");
        User bish = um.getUser("bish");
        User pres = um.getUser("pres");
        
        // verify terminating slashes make no difference
        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/some/ward/page/", clark, null));
        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/some/ward/page/", bish, null));
        Assert.assertFalse(em.isAllowed("local.lds.org", "GET", "/root/some/ward/page/", pres, null));

        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/some/stake/page/", clark, null));
        Assert.assertFalse(em.isAllowed("local.lds.org", "GET", "/root/some/stake/page/", bish, null));
        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/some/stake/page/", pres, null));

        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/ward/stake/", clark, null));
        // next one is true since evaluation is hierarchically traversed and once granted access at any level 
        // the user gets all levels below that. Hence bish gets /root/ward and therefore also gets this one.
        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/ward/stake/", bish, null));
        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/ward/stake/", pres, null));

        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/ward/", clark, null));
        Assert.assertTrue(em.isAllowed("local.lds.org", "GET", "/root/ward/", bish, null));
        Assert.assertFalse(em.isAllowed("local.lds.org", "GET", "/root/ward/", pres, null));
    }

    //@Test
    public void test_root_level_entitlement() throws Exception {
        Config cfg = Config.getInstance();
        
        EntitlementsManager em = cfg.getEntitlementsManager();
        Assert.assertNotNull(em.entitlements.get("GET:local.lds.org/"));
        Assert.assertEquals(em.entitlements.get("GET:local.lds.org/").getConditionId(), "super-user");
    }    

    //@Test
    public void test_terminating_slash_removed() throws Exception {
        Config cfg = Config.getInstance();
        
        EntitlementsManager em = cfg.getEntitlementsManager();
        Assert.assertNotNull(em.entitlements.get("GET:local.lds.org/root/some/ward/page"));
    }    

    //@Test
    public void test_multiple_actions_and_conditions_per_urn() throws Exception {
        Config cfg = Config.getInstance();
        
        EntitlementsManager em = cfg.getEntitlementsManager();
        Assert.assertNotNull(em.entitlements.get("GET:local.lds.org/root/some/ward/page"));
        Assert.assertEquals(em.entitlements.get("GET:local.lds.org/root/some/ward/page").getConditionId(), "bishop");
        Assert.assertNotNull(em.entitlements.get("POST:local.lds.org/root/some/ward/page"));
        Assert.assertEquals(em.entitlements.get("POST:local.lds.org/root/some/ward/page").getConditionId(), "stake-pres");
    }    
}
