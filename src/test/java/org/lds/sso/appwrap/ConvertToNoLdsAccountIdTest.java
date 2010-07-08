package org.lds.sso.appwrap;

import java.util.Map;

import junit.framework.Assert;

import org.lds.sso.appwrap.ConvertToNoLdsAccountId.Grouping;
import org.testng.annotations.Test;

public class ConvertToNoLdsAccountIdTest {

    @Test
    public void test () throws Exception {
        String complex = "" 
            + "<AND>"
            + " <IsEmployee/>"
            + " <OR>"
            + "  <HasLdsAccountId>" 
            + "   <LdsAccount id='111' name='ONE'/>" 
            + "   <LdsAccount id='222' name='TWO'/>" 
            + "  </HasLdsAccountId>" 
            + "  <MemberOfUnit>" 
            + "   <Unit id='66230'/>" 
            + "   <Unit id='66826'/>" 
            + "   <Unit id='105481'/>"  
            + "  </MemberOfUnit>" 
            + "  <HasLdsAccountId id='333'/>" 
            + "  <HasLdsAccountId id='444'>" 
            + "   <LdsAccount id='555' name='FIVE'/>" 
            + "   <LdsAccount id='666'/>" 
            + "  </HasLdsAccountId>" 
            + " </OR>"
            + " <NOT>"
            + "  <HasLdsAccountId>" 
            + "   <LdsAccount id='777'/>" 
            + "   <LdsAccount id='888' name='eight'/>" 
            + "  </HasLdsAccountId>" 
            + " </NOT>"
            + "</AND>";
        System.setProperty("complex-condition", complex);
        
        String simple = ""
            + "<HasLdsAccountId>" 
            + " <LdsAccount id='777'/>" 
            + " <LdsAccount id='888'/>" 
            + "</HasLdsAccountId>"; 
        System.setProperty("simple-condition", simple);
        
        String non = "<IsEmployee/>"; 
        System.setProperty("non-hlai", non);
        
        String xml = 
            "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias complex-alias= system:complex-condition ?>"
            + "<?alias simple-alias   = system:simple-condition  ?>"
            + "<?alias non-hlai-alias = system:non-hlai          ?>"
            + "<?alias non-cond-alias = some static text         ?>"
            + ""
            + "<config console-port='88' proxy-port='45'>"
            + " <sso-traffic>"
            + "  <by-site host='labs-local.lds.org' port='45' scheme='http'>"
            + "   <allow cpath='/auth/_app/*' action='GET,POST' condition='{{complex-alias}}'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + "  <users>"
            + "   <user name='u111' pwd=''>"
            + "    <sso-header name='policy-ldsaccountid' value='111'/>"
            + "   </user>"
            + "   <user name='u222' pwd=''>"
            + "    <sso-header name='policy-ldsaccountid' value='222'/>"
            + "   </user>"
            + "   <user name='u333' pwd=''>"
            + "    <sso-header name='policy-ldsaccountid' value='333'/>"
            + "   </user>"
            + "   <user name='u444' pwd=''>"
            + "    <sso-header name='policy-ldsaccountid' value='444'/>"
            + "   </user>"
            + "   <user name='u555' pwd=''>"
            + "    <sso-header name='policy-ldsaccountid' value='555'/>"
            + "   </user>"
            + "   <user name='u666' pwd=''>"
            + "    <sso-header name='policy-ldsaccountid' value='666'/>"
            + "   </user>"
            + "   <user name='u777' pwd=''>"
            + "    <sso-header name='policy-ldsaccountid' value='777'/>"
            + "   </user>"
            + "   <user name='u888' pwd=''>"
            + "    <sso-header name='policy-ldsaccountid' value='888'/>"
            + "   </user>"
            + "  </users>"
            + "  <sso-entitlements policy-domain='lds.org'>"
            + "   <allow action='GET' urn='/leader/focus/page' condition='{{complex-alias}}'/>" 
            + "   <allow action='GET' urn='/leader/focus' condition='{{simple-alias}}'/>" 
            + "   <allow action='GET' urn='/leader/ward/page' condition='{{non-hlai-alias}}'/>" 
            + "  </sso-entitlements>"
            + "</config>";

        Map<String, Grouping> dirs = ConvertToNoLdsAccountId.getGroupingDirectives(new String[] {"string:" + xml});

        validate(dirs, "complex-alias-1", "system:complex-condition", new String[] {"u111", "u222", "u333", "u444", "u555", "u666"});
        validate(dirs, "complex-alias-2", "system:complex-condition",  new String[] {"u777", "u888"});
        validate(dirs, "simple-alias", "system:simple-condition",  new String[] {"u777", "u888"});
    }

    private void validate(Map<String, Grouping> dirs, String grpName, String conditionSrc, String[] users) {
        Grouping g = dirs.get(grpName);
        Assert.assertNotNull(g);
        Assert.assertEquals(g.message, "--->>> For grouping '" + grpName + "' found in '" + conditionSrc + "' replace all <HasLdsAccountId/> elements with a single <HasLdsApplication value='" + grpName + "'/> element.");

        for(String usr : users) {
            Assert.assertNotNull(g.userMessages.get(usr));
            Assert.assertEquals(g.userMessages.get(usr), "--- for user " + usr + " add a nested element <ldsapplications value='" + grpName + "'/>");
        }
    }
}
