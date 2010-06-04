package org.lds.sso.appwrap.proxy;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
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

public class SignMeInOutTest {

    private Service service;

    @BeforeClass
    public void setUpSimulator() throws Exception {
        StringBuffer config = new StringBuffer("string:")
        .append("<?xml version='1.0' encoding='UTF-8'?>")
        .append("<config console-port='auto' proxy-port='auto' rest-version='CD-OESv1'>")
        .append(" <sso-cookie name='lds-policy' domain='.lds.org'/>")
        .append(" <sso-sign-in-url value='http://127.0.0.1:{{console-port}}/admin/selectUser.jsp'/>")
        .append(" <sso-traffic>")
        .append("  <by-site scheme='http' host='127.0.0.1' port='{{proxy-port}}'>") 
        .append("    <cctx-mapping cctx='/is-alive*' thost='127.0.0.1' tport='{{console-port}}' tpath='/is-alive*'/>") 
        .append("    <unenforced cpath='/is-alive'/>") 
        .append("    <unenforced cpath='/is-alive?*'/>") 
        .append("  </by-site>")
        .append(" </sso-traffic>")
        .append(" <users>")
        .append("  <user name='user1' pwd='pwd'/>")
        .append("  <user name='user2' pwd='pwd'/>")
        .append(" </users>")
        .append("</config>");

        service = new Service(config.toString());
        service.start();
    }
    
    @AfterClass
    public void tearDownSimulator() throws Exception {
        service.stop();
    }
    
    @Test
    public void test_signMeInOut() throws HttpException, IOException {
        // first try to hit without session but with signin indicator
        Config cfg = Config.getInstance();
        String endpointWSignin = "http://127.0.0.1:" + cfg.getProxyPort() + ImAliveHandler.IS_ALIVE_PATH + "?" + GlobalHeaderNames.SIGNIN_VALUE;
        
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(endpointWSignin);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        
        Assert.assertEquals(status, 302);
        Header loc = method.getResponseHeader("location");
        Assert.assertNotNull(loc, "location header not returned for redirect");

        // now sign user in
        String authEp = loc.getValue().replace("/admin/selectUser.jsp", "/admin/action/set-user/user1");
        HttpMethod authM = new GetMethod(authEp);
        authM.setFollowRedirects(false);
        status = client.executeMethod(authM);
        
        Assert.assertEquals(status, 302);
        Header setCk = authM.getResponseHeader("set-cookie");
        Assert.assertNotNull(setCk, "set-cookie header not in sign-in response");
        String rawCk = setCk.getValue();
        Assert.assertTrue(rawCk.contains("lds-policy="));
        int start = rawCk.indexOf("lds-policy=") + "lds-policy".length();
        int end = rawCk.indexOf(";", start);
        String token = rawCk.substring(start + 1, end);
        
        // now try again with session and verify we get through
        method = new GetMethod(endpointWSignin);
        method.setFollowRedirects(false);
        method.setRequestHeader("cookie", "lds-policy=" + token);
        status = client.executeMethod(method);
        
        Assert.assertEquals(status, 200);
        String content = method.getResponseBodyAsString();
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");

        // now hit with session and signout indicator
        String endpointWSignout = "http://127.0.0.1:" + cfg.getProxyPort() + ImAliveHandler.IS_ALIVE_PATH + "?" + GlobalHeaderNames.SIGNOUT_VALUE;
        method = new GetMethod(endpointWSignout);
        method.setFollowRedirects(false);
        method.setRequestHeader("cookie", "lds-policy=" + token);
        status = client.executeMethod(method);
        Assert.assertEquals(status, 302);
        setCk = method.getResponseHeader("set-cookie");
        Assert.assertNotNull(setCk, "set-cookie header not in sign-out response");
        Assert.assertTrue(setCk.getValue().contains("cookie-monster"), "cookie for clearing not sent");

        // now if we hit again with signout indicator it should go through since session terminated
        method = new GetMethod(endpointWSignout);
        method.setFollowRedirects(false);
        method.setRequestHeader("cookie", "lds-policy=" + token);
        status = client.executeMethod(method);
        Assert.assertEquals(status, 200, "should have allowed request through");
        content = method.getResponseBodyAsString();
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
    }

    @Test
    public void test_isAlive() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        String endpoint = "http://127.0.0.1:" + cfg.getConsolePort() + "/is-alive";
        
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        
        Assert.assertEquals(status, 200);
        String content = method.getResponseBodyAsString();
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
    }
}
