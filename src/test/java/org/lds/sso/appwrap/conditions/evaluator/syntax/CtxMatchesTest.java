package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.util.HashMap;
import java.util.Map;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.EntitlementsManager;
import org.lds.sso.appwrap.UserManager;
import org.lds.sso.appwrap.XmlConfigLoader2;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.LogicalSyntaxEvaluationEngine;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CtxMatchesTest {

    @Test
    public void test_regex_bad_alias_parsing() throws Exception {
        CtxMatches m = new CtxMatches();
        Map<String,String> atts = new HashMap<String,String>();
        atts.put("regex", "1234{$alias5678");
        try {
            m.init("syntax-goes-here", atts);
            Assert.fail("should have thrown EvaluationException for unterminated alias.");
        }
        catch(EvaluationException e) {
            
        }
    }

    @Test
    public void test_regex_alias_parsing() throws Exception {
        CtxMatches m = new CtxMatches();
        Map<String,String> atts = new HashMap<String,String>();
        atts.put("regex", "1234{$alias$}5678");
        m.init("syntax-goes-here", atts);
        Assert.assertEquals(m.aliases.size(),1);
        Assert.assertEquals(m.aliases.get(0), "alias");
        Assert.assertEquals(m.regex, "1234{0}5678");
    }

    @Test
    public void test_regex_att_unchanged_after_init() throws Exception {
        CtxMatches m = new CtxMatches();
        Map<String,String> atts = new HashMap<String,String>();
        atts.put("regex", "bogus-value");
        m.init("syntax-goes-here", atts);
        Assert.assertEquals(m.regex, "bogus-value");
    }

    @Test
    public void test_ex_when_no_header_att() throws Exception {
        CtxMatches m = new CtxMatches();
        Map<String,String> atts = new HashMap<String,String>();
        atts.put("regex", "bogus-value");
        m.addEvaluator(new Unit());
        m.init("syntax-goes-here", atts);
        
        try {
            m.validate();
            Assert.fail("should have throw exception since no header attribute.");
        }
        catch(EvaluationException e) {
        }
    }

    @Test
    public void test_ex_when_empty_header_att() throws Exception {
        CtxMatches m = new CtxMatches();
        Map<String,String> atts = new HashMap<String,String>();
        atts.put("regex", "bogus-value");
        atts.put("header", "");
        m.addEvaluator(new Unit());

        try {
            m.init("syntax-goes-here", atts);
            m.validate();
            Assert.fail("should have throw exception since empty header attribute.");
        }
        catch(EvaluationException e) {
        }
    }

    @Test
    public void test_ex_when_no_regex_att() throws Exception {
        CtxMatches m = new CtxMatches();
        Map<String,String> atts = new HashMap<String,String>();
        atts.put("header", "bogus-value");
        m.addEvaluator(new Unit());
        m.init("syntax-goes-here", atts);
        
        try {
            m.validate();
            Assert.fail("should have throw exception since no regex attribute.");
        }
        catch(EvaluationException e) {
        }
    }

    @Test
    public void test_ex_when_empty_regex_att() throws Exception {
        CtxMatches m = new CtxMatches();
        Map<String,String> atts = new HashMap<String,String>();
        atts.put("header", "bogus-value");
        atts.put("regex", "");
        m.addEvaluator(new Unit());
        m.init("syntax-goes-here", atts);

        try {
            m.validate();
            Assert.fail("should have throw exception since empty regex attribute.");
        }
        catch(EvaluationException e) {
        }
    }

    @Test
    public void test_ex_when_no_nested_evaluator() throws Exception {
        CtxMatches m = new CtxMatches();
        Map<String,String> atts = new HashMap<String,String>();
        atts.put("header", "bogus-value");
        atts.put("regex", "bogus-value");
        m.init("syntax-goes-here", atts);

        try {
            m.validate();
            Assert.fail("should have throw exception since no nested evaluators.");
        }
        catch(EvaluationException e) {
        }
    }

    @Test
    public void test_only_valid_child_evaluators_accepted() throws Exception {
        CtxMatches m = new CtxMatches();
        m.addEvaluator(new Assignment());
        Assert.assertEquals(m.evaluators.size(), 1);
        m.addEvaluator(new Position());
        Assert.assertEquals(m.evaluators.size(), 2);
        m.addEvaluator(new Unit());
        Assert.assertEquals(m.evaluators.size(), 3);

        m.addEvaluator(new IsMember());
        Assert.assertEquals(m.evaluators.size(), 3);
        m.addEvaluator(new IsEmployee());
        Assert.assertEquals(m.evaluators.size(), 3);
    }

    @Test
    public void test() throws Exception {
        String id = UserHeaderNames.LDS_ACCOUNT_ID;
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='88' proxy-port='45'>"

            + "  <conditions>"
            + "   <condition alias='bish-of-viewed-unit'>"
            + "    <CtxMatches header='policy-ldspositions' regex='^.*p{$position.id$}/7u{$ctx.unit-being-viewed$}/.*$'>"
            + "     <Position id='4' desc='Bishop'/>"
            + "    </CtxMatches>"
            + "   </condition>"

            + "   <condition alias='has-lds-account-id'>"
            + "    <CtxMatches header='policy-ldsaccountid' regex='^[0-9].*$' desc='checks for non-empty, numerically filled ldsaccountid'>"
            + "     <Position id='not-used' desc='placed-here-since-child-evaluator-mandatory'/>"
            + "    </CtxMatches>"
            + "   </condition>"
            + "  </conditions>"

            + "  <sso-traffic>"
            + "   <by-site host='local.lds.org' port='80'>"
            + "    <entitlements>"
            + "     <allow action='GET' urn='/maps/can/update/phone' condition='{{bish-of-viewed-unit}}'/>" 
            + "     <allow action='GET' urn='/has/lds/account/id' condition='{{has-lds-account-id}}'/>" 
            + "    </entitlements>"
            + "   </by-site>"
            + "  </sso-traffic>"
            + "  <users>"
            + "   <user name='b1000' pwd='password1'>"
            + "    <sso-header name='policy-ldspositions' value='p4/7u1000/5u524735/1u791040/'/>"
            + "    <sso-header name='policy-ldsaccountid' value='1234567890'/>"
            + "   </user>"
            + "   <user name='b2000' pwd='password1'>"
            + "    <sso-header name='policy-ldspositions' value='p1/5u524735/1u791040/:p4/7u2000/5u524735/1u791040/'/>"
            + "   </user>"
            + "   <user name='b3000' pwd='password1'>"
            + "    <sso-header name='policy-ldspositions' value='p1/5u524735/1u791040/:p4/7u3000/5u524735/1u791040/:p54/5u524735/1u791040/'/>"
            + "   </user>"
            + "  </users>"
            + "</config>";
        Config cfg = new Config();
        XmlConfigLoader2.load(xml);
        EntitlementsManager emgr = cfg.getEntitlementsManager();
        UserManager umgr = cfg.getUserManager();
        Map<String,String> ctx = new HashMap<String,String>();
        ctx.put("unit-being-viewed", "1000");
        Assert.assertTrue(emgr.isAllowed("local.lds.org", "GET", "/maps/can/update/phone", umgr.getUser("b1000"), ctx));
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/maps/can/update/phone", umgr.getUser("b2000"), ctx));
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/maps/can/update/phone", umgr.getUser("b3000"), ctx));

        ctx.put("unit-being-viewed", "2000");
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/maps/can/update/phone", umgr.getUser("b1000"), ctx));
        Assert.assertTrue(emgr.isAllowed("local.lds.org", "GET", "/maps/can/update/phone", umgr.getUser("b2000"), ctx));
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/maps/can/update/phone", umgr.getUser("b3000"), ctx));
        
        ctx.put("unit-being-viewed", "3000");
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/maps/can/update/phone", umgr.getUser("b1000"), ctx));
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/maps/can/update/phone", umgr.getUser("b2000"), ctx));
        Assert.assertTrue(emgr.isAllowed("local.lds.org", "GET", "/maps/can/update/phone", umgr.getUser("b3000"), ctx));
        
        // interesting twist on use but works
        Assert.assertTrue(emgr.isAllowed("local.lds.org", "GET", "/has/lds/account/id", umgr.getUser("b1000"), null));
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/has/lds/account/id", umgr.getUser("b2000"), null));
        Assert.assertFalse(emgr.isAllowed("local.lds.org", "GET", "/has/lds/account/id", umgr.getUser("b3000"), null));
    }
}
