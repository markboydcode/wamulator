package org.lds.sso.appwrap.proxy;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.conditions.evaluator.GlobalHeaderNames;
import org.lds.sso.appwrap.ui.ImAliveHandler;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * TODO get this unit test finished; looks like a copy that wasn't completed.
 * 
 * Test case for evaluating cross domain single sign on.
 * 
 * To support cross domain sso the domain of each by-site directive must
 * reside in the domain of the sso-cookie declaration or a nested cdsso
 * declaration. If it does not 
 * then there can be no <allow> declarations in that by-site directive.
 * If there are then an exception must be thrown indicating that cdsso
 * can not take place since there is no defined cookie for that domain
 * allowing the simulator to sign a user in and track their session 
 * accordingly for that domain.
 * 
 * To support more than one domain the sso-cookie declaration now supports
 * nested cdsso declarations containing the attribute "domain". The domain
 * declared in the sso-cookie directive becomes the cdsso master which means 
 * that it is the one that contains the sign-in page.
 * 
 * CDSSO takes place for the master
 * domain in the trivial sso manner, they are redirected to the sign-in
 * page, authentication results in a cookie being set, and the user is
 * redirected back to the original resource.
 * 
 * For non-master domains the user is redirected to the sign-in page in
 * the master domain, the user authenticates and the http response is a
 * redirect back to the original resource. As part of that redirect a
 * a cookie is set in the master domain as with trivial SSO. But in 
 * addition, since the targeted resource in in a different domain than
 * that of the master a query parameter is added to the redirect to
 * indicate that CDSSO is in progress and the value of that query 
 * parameter is the session token in the master domain. Upon receipt the
 * non-master domain accepts that token, validates it, and redirects to 
 * the resource without the query parameter. In that redirect is a 
 * set-cookie directive to set the token in that non-master domain.
 *  
 * Signing out takes place by targeting a resource in some domain with 
 * the signmeout query parameter. Upon receipt, if there is only one 
 * domain declared, the simulator will terminate the session and 
 * redirect to the resource without the signmeout parameter.
 * 
 * Upon receipt with multiple domains, a page will be 
 * served up indicating that they are being signed-out and please wait
 * one moment. That page will leverage javascript to hit a signmeout
 * request in all declared domains. Upon completion of all such requests
 * the page will redirect them back to the originally targeted resource
 * but without the signmeout query parameter.
 */ 
@Test(singleThreaded = true)
public class CrossDomainSignMeInOutTest {
/*
    private Service service;
    private static final String signInUrl = "http://local.lds.org:{{proxy-port}}/admin/selectUser.jsp";

    
    public void setUpSimulator() throws Exception {
        // clear out any config residue from other tests
        new Config();

        StringBuffer config = new StringBuffer("string:")
        .append("<?xml version='1.0' encoding='UTF-8'?>")
        .append("<config console-port='auto' proxy-port='auto' rest-version='CD-OESv1'>")
        .append(" <sso-sign-in-url value='").append(signInUrl).append("' />")
        .append(" <console-recording sso='true' rest='true' max-entries='100' enable-debug-logging='true'/>")
        .append(" <sso-cookie name='lds-policy' domain='.lds.org'              >")
        .append("  <cdsso                       domain='local.mormon.org'     />")
        .append("  <cdsso                       domain='local.josephsmith.net'/>")
        .append(" </sso-cookie>")
        .append(" <sso-traffic>")
        .append("  <by-site scheme='http' host='local.lds.org' port='{{proxy-port}}'>")
        .append("    <cctx-mapping cctx='/secure/is-alive*' thost='127.0.0.1' tport='{{console-port}}' tpath='/is-alive*'/>")
        .append("    <allow cpath='/secure/is-alive' action='GET'/>")
        .append("    <cctx-mapping cctx='/admin/signmeout.*' thost='127.0.0.1' tport='{{console-port}}' tpath='/admin/signmeout.*'/>")
        .append("    <unenforced cpath='/admin/signmeout.*?*' />")
        .append("    <cctx-mapping cctx='/admin/*' thost='127.0.0.1' tport='{{console-port}}' tpath='/admin/*'/>")
        .append("    <unenforced cpath='/admin/*?*' />")
        .append("    <unenforced cpath='/admin/*' />")
        .append("  </by-site>")
        .append("  <by-site scheme='http' host='another.lds.org' port='{{proxy-port}}'>")
        .append("    <cctx-mapping cctx='/secure/is-alive*' thost='127.0.0.1' tport='{{console-port}}' tpath='/is-alive*'/>")
        .append("    <allow cpath='/secure/is-alive' action='GET'/>")
        .append("    <cctx-mapping cctx='/admin/signmeout.*' thost='127.0.0.1' tport='{{console-port}}' tpath='/admin/signmeout.*'/>")
        .append("    <unenforced cpath='/admin/signmeout.*?*' />")
        .append("    <unenforced cpath='/admin/signmeout.*' />")
        .append("  </by-site>")
        .append("  <by-site scheme='http' host='local.mormon.org' port='{{proxy-port}}'>")
        .append("    <cctx-mapping cctx='/secure/is-alive*' thost='127.0.0.1' tport='{{console-port}}' tpath='/is-alive*'/>")
        .append("    <allow cpath='/secure/is-alive' action='GET'/>")
        .append("    <cctx-mapping cctx='/admin/signmeout.*' thost='127.0.0.1' tport='{{console-port}}' tpath='/admin/signmeout.*'/>")
        .append("    <unenforced cpath='/admin/signmeout.*?*' />")
        .append("    <unenforced cpath='/admin/signmeout.*' />")
        .append("  </by-site>")
        .append("  <by-site scheme='http' host='local.josephsmith.net' port='{{proxy-port}}'>")
        .append("    <cctx-mapping cctx='/secure/is-alive*' thost='127.0.0.1' tport='{{console-port}}' tpath='/is-alive*'/>")
        .append("    <allow cpath='/secure/is-alive' action='GET'/>")
        .append("    <cctx-mapping cctx='/admin/signmeout.*' thost='127.0.0.1' tport='{{console-port}}' tpath='/admin/signmeout.*'/>")
        .append("    <unenforced cpath='/admin/signmeout.*?*' />")
        .append("    <unenforced cpath='/admin/signmeout.*' />")
        .append("  </by-site>")
        .append(" </sso-traffic>")
        .append(" <users>")
        .append("  <user name='user1' pwd='pwd'/>")
        .append("  <user name='user2' pwd='pwd'/>")
        .append(" </users>")
        .append("</config>");

        service = Service.getService(config.toString());
        service.start();
    }

    
    public void tearDownSimulator() throws Exception {
        service.stop();
    }

    
    public void test_signMeInForSecuredResource() throws HttpException, IOException {
        testIsAliveInMasterDomain("local.lds.org", ".lds.org");
        testIsAliveInSecondaryDomain("local.mormon.org", "local.mormon.org", "local.lds.org", ".lds.org", true);
        testIsAliveInSecondaryDomain("local.josephsmith.net", "local.josephsmith.net", "local.lds.org", ".lds.org", true);
    }

    private void testIsAliveInMasterDomain(String domain, String cookieDomain) throws HttpException, IOException {
        Config cfg = Config.getInstance();
        // hit is-alive and verify redirect to correct sign-in page location
        String endpoint = "http://" + domain + ":" + cfg.getProxyPort() 
            + "/secure" + ImAliveHandler.IS_ALIVE_PATH;

        HttpClient client = new HttpClient();

        // prevent needing dns resolution of local.lds.org and route req to 127.0.0.1/proxy port
        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", cfg.getProxyPort());
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);

        Assert.assertEquals(status, 302);
        Header loc = method.getResponseHeader("location");
        Assert.assertNotNull(loc, "location header not returned for redirect");
        // verify that it gets redirected to master-domain which is the domain
        // of the first by-site entry in document order that has a domain 
        // which (a) matches that of the first sso-cookie declaration in the case
        // of a full domain or (b) is a subdomain of the first sso-cookie declaration
        // when that declaration specifies a domain-wide cookie syntax where the 
        // declared domain begins with a period.
        String loginPage = Config.getInstance().getLoginPage();
        Assert.assertTrue(loc.getValue().startsWith(loginPage));
        
        ///// now redirect to sign-in with cookie from master domain and it 
        // should redirect back to goto url with token query param added
        String authEp = loc.getValue();
        HttpMethod authM = new GetMethod(authEp);
        authM.setFollowRedirects(false);

        // prevent needing dns resolution of local.lds.org and route req to 127.0.0.1/console port this time since redir is to that port
        hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", cfg.getProxyPort());
        client.setHostConfiguration(hcfg);

        status = client.executeMethod(authM);

        Assert.assertEquals(status, 302);
        Header setCk = authM.getResponseHeader("set-cookie");
        Assert.assertNotNull(setCk, "set-cookie header not in sign-in response");
        String rawCk = setCk.getValue();
        Assert.assertTrue(rawCk.contains("Domain=" + cookieDomain));
        Assert.assertTrue(rawCk.contains("lds-policy="));
        int start = rawCk.indexOf("lds-policy=") + "lds-policy".length();
        int end = rawCk.indexOf(";", start);
        String token = rawCk.substring(start + 1, end);

        ///// now try again with session and verify we get through
        // recreate client so that we aren't getting cached entries
        client = new HttpClient();
        // prevent needing dns resolution of local.lds.org and route req to 127.0.0.1/proxy port
        hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", cfg.getProxyPort());
        client.setHostConfiguration(hcfg);
        method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
//        method.setRequestHeader(new Header("cookie", cfg.getCookieName() + "=" + token));
        method.setRequestHeader("cookie", "lds-policy=" + token);
        status = client.executeMethod(method);

        Assert.assertEquals(status, 200);
        String content = method.getResponseBodyAsString();
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
    }
    
    private void testIsAliveInSecondaryDomain(String secondaryDomain, String secondaryCookieDomain, String masterDomain, String masterCookieDomain, boolean isAlreadyAuthdInMaster) throws HttpException, IOException {
        Config cfg = Config.getInstance();
        // hit is-alive and verify redirect to correct sign-in page location
        String endpoint = "http://" + secondaryDomain + ":" + cfg.getProxyPort() 
            + "/secure" + ImAliveHandler.IS_ALIVE_PATH;

        HttpClient client = new HttpClient();

        // prevent needing dns resolution of local...domains and route req to 127.0.0.1/proxy port
        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", cfg.getProxyPort());
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);

        Assert.assertEquals(status, 302);
        Header loc = method.getResponseHeader("location");
        Assert.assertNotNull(loc, "location header not returned for redirect");
        // verify that it gets redirected to master-domain which is the domain
        // of the first by-site entry in document order that has a domain 
        // which (a) matches that of the first sso-cookie declaration in the case
        // of a full domain or (b) is a subdomain of the first sso-cookie declaration
        // when that declaration specifies a domain-wide cookie syntax where the 
        // declared domain begins with a period.
        Assert.assertTrue(loc.getValue().startsWith(signInUrl));
        
        ///// now sign user in via console port
        client = new HttpClient();
        // prevent needing dns resolution of local...domains and route req to 127.0.0.1/proxy port
        hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", cfg.getProxyPort());
        client.setHostConfiguration(hcfg);
        HttpMethod redirToSigninInFromSecondary = new GetMethod(loc.getValue());
        redirToSigninInFromSecondary.setFollowRedirects(false); 

        status = client.executeMethod(redirToSigninInFromSecondary);
        
        if (!isAlreadyAuthdInMaster) {
            // verify that cookie gets set in master if not already authenticated
            // in that domain.
            Header setCk = redirToSigninInFromSecondary.getResponseHeader("set-cookie");
            Assert.assertNotNull(setCk, "set-cookie header not in sign-in response");
            String rawCk = setCk.getValue();
            
            Assert.assertTrue(rawCk.contains("Domain=" + masterCookieDomain));
            Assert.assertTrue(rawCk.contains("lds-policy="));
            int start = rawCk.indexOf("lds-policy=") + "lds-policy".length();
            int end = rawCk.indexOf(";", start);
            String token = rawCk.substring(start + 1, end);
        }

        // verify that redirected to original resource in secondary domain
        // and that redirect includes the set-secondary-domain-token parameter.

        Assert.assertEquals(status, 302);
        Header loc2 = method.getResponseHeader("location");
        Assert.assertNotNull(loc2, "location header not returned for redirect");
        Assert.assertTrue(loc2.getValue().contains(secondaryDomain + ":" + cfg.getProxyPort()));
        Assert.assertTrue(loc2.getValue().contains("set-secondary-domain-token="));

        ///// now hit original resource in secondary domain via location value
        String origResWtokenEp = loc2.getValue();
        HttpMethod origResWtoken = new GetMethod(origResWtokenEp);
        origResWtoken.setFollowRedirects(false);
        status = client.executeMethod(origResWtoken);

        Assert.assertEquals(status, 302, "redirect back to secondary domain should redirect to set cookie and remove token parameter");
        Header setCk = redirToSigninInFromSecondary.getResponseHeader("set-cookie");
        Assert.assertNotNull(setCk, "set-cookie header not in secondary domain handling of initial redirect after master domain sign-in");
        String rawCk = setCk.getValue();
        
        Assert.assertTrue(rawCk.contains("Domain=" + secondaryCookieDomain));
        Assert.assertTrue(rawCk.contains("lds-policy="));
        int start = rawCk.indexOf("lds-policy=") + "lds-policy".length();
        int end = rawCk.indexOf(";", start);
        String token = rawCk.substring(start + 1, end);

        Header loc3 = method.getResponseHeader("location");
        Assert.assertNotNull(loc3, "location header not returned for handling of set-secondary-domain-token redirect");
        Assert.assertTrue(loc3.getValue().contains(secondaryDomain + ":" + cfg.getProxyPort()));
        Assert.assertFalse(loc3.getValue().contains("set-secondary-domain-token="), "set-secondary-domain-token parameter should have been stripped off");

        String origResWoutTokenEp = loc2.getValue();
        HttpMethod origResWoutToken = new GetMethod(origResWoutTokenEp);
        origResWoutToken.setFollowRedirects(false);
        status = client.executeMethod(origResWoutToken);


        
        
        Assert.assertEquals(status, 200);
        String content = method.getResponseBodyAsString();
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
    }
    */
}
