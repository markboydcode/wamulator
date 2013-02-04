package org.lds.sso.appwrap.proxy;

import java.io.File;
import java.io.IOException;
import java.net.URL;

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

public class SignMeInOutTest {

    private Service service;

    @BeforeClass
    public void setUpSimulator() throws Exception {
        // clear out any config residue from other tests
        new Config();

    	System.getProperties().remove("non-existent-sys-prop");
    	URL filePath = SignMeInOutTest.class.getClassLoader().getResource("SignMeInOutTestConfig.xml");
        StringBuffer config = new StringBuffer("string:")
        .append("<?file-alias policy-src-xml=\"" + filePath.getPath() + "\"?>")
        .append("<?system-alias usr-src-props=non-existent-sys-prop default=")
        .append("\"xml=")
        .append(" <users>")
        .append("  <user name='user1' pwd='pwd'/>")
        .append("  <user name='user2' pwd='pwd'/>")
        .append(" </users>")
        .append("\"?>")
        .append("<config console-port='auto' proxy-port='auto' rest-version='CD-OESv1'>")
        .append(" <sso-cookie name='lds-policy' domain='.lds.org'/>")
        .append(" <sso-sign-in-url value='http://local.lds.org:{{console-port}}/admin/selectUser.jsp'/>")
        .append(" <sso-traffic>")
        .append("  <by-site scheme='http' host='local.lds.org' port='{{proxy-port}}'>")
        .append("    <cctx-mapping thost='127.0.0.1' tport='{{console-port}}'>")
        .append("      <policy-source>xml={{policy-src-xml}}</policy-source>")
        .append("    </cctx-mapping>")
        .append("  </by-site>")
        .append(" </sso-traffic>")
        .append(" <user-source type='xml'>{{usr-src-props}}</user-source>")
        .append("</config>");

        service = Service.getService(config.toString());
        service.start();
    }

    @AfterClass
    public void tearDownSimulator() throws Exception {
        service.stop();
    }

    @Test
    public void test_signMeInOut() throws HttpException, IOException {
        ///// first try to hit without session but with signin indicator
        Config cfg = Config.getInstance();
        String endpointWSignin = "http://local.lds.org:" + cfg.getProxyPort() + ImAliveHandler.IS_ALIVE_PATH + "?" + GlobalHeaderNames.SIGNIN_VALUE;

        HttpClient client = new HttpClient();

        // prevent needing dns resolution of local.lds.org and route req to 127.0.0.1/proxy port
        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", cfg.getProxyPort());
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(endpointWSignin);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);

        Assert.assertEquals(status, 302);
        Header loc = method.getResponseHeader("location");
        Assert.assertNotNull(loc, "location header not returned for redirect");

        ///// now sign user in via console port
        String authEp = loc.getValue().replace("/admin/selectUser.jsp", "/admin/action/set-user/user1");
        HttpMethod authM = new GetMethod(authEp);
        authM.setFollowRedirects(false);

        // prevent needing dns resolution of local.lds.org and route req to 127.0.0.1/console port this time since redir is to that port
        hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", cfg.getConsolePort());
        client.setHostConfiguration(hcfg);

        status = client.executeMethod(authM);

        Assert.assertEquals(status, 302);
        loc = method.getResponseHeader("location");
        Assert.assertNotNull(loc, "location header not returned for redirect after sign-in");
        Header setCk = authM.getResponseHeader("set-cookie");
        Assert.assertNotNull(setCk, "set-cookie header not in sign-in response");
        String rawCk = setCk.getValue();
        Assert.assertTrue(rawCk.contains("lds-policy="));
        int start = rawCk.indexOf("lds-policy=") + "lds-policy".length();
        int end = rawCk.indexOf(";", start);
        String token = rawCk.substring(start + 1, end);

        ///// now try again with session and verify we get through
        method = new GetMethod(endpointWSignin);
        method.setFollowRedirects(false);
        method.setRequestHeader("cookie", "lds-policy=" + token);
        status = client.executeMethod(method);

        Assert.assertEquals(status, 200);
        String content = method.getResponseBodyAsString();
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");

        ////// now hit with session and signout indicator
        String endpointWSignout = "http://local.lds.org:" + cfg.getProxyPort() + ImAliveHandler.IS_ALIVE_PATH + "?" + GlobalHeaderNames.SIGNOUT_VALUE;
        method = new GetMethod(endpointWSignout);
        method.setFollowRedirects(false);
        method.setRequestHeader("cookie", "lds-policy=" + token);

        // prevent needing dns resolution of local.lds.org and route req to 127.0.0.1/proxy port again
        hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", cfg.getProxyPort());
        client.setHostConfiguration(hcfg);

        status = client.executeMethod(method);
        Assert.assertEquals(status, 302);
        setCk = method.getResponseHeader("set-cookie");
        Assert.assertNotNull(setCk, "set-cookie header not in sign-out response");
        Assert.assertTrue(setCk.getValue().contains("cookie-monster"), "cookie for clearing not sent");

        ///// now if we hit again with signout indicator it should go through since session terminated
        method = new GetMethod(endpointWSignout);
        method.setFollowRedirects(false);
        method.setRequestHeader("cookie", "lds-policy=" + token);

        // note that we are hitting proxy port due to hostconfig above
        status = client.executeMethod(method);
        Assert.assertEquals(status, 200, "should have allowed request through");
        content = method.getResponseBodyAsString();
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
    }

    @Test
    public void test_isAlive() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        String endpoint = "http://local.lds.org:" + cfg.getConsolePort() + "/is-alive";

        HttpClient client = new HttpClient();

        // prevent needing dns resolution of local.lds.org and route req to 127.0.0.1
        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", cfg.getConsolePort());
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String content = method.getResponseBodyAsString();

        Assert.assertEquals(status, 200);
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
    }
}
