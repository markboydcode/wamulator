package org.lds.sso.appwrap.proxy;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.ui.ImAliveHandler;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests if a domain of localhost can be used for the cookie and functions
 * correctly.
 * 
 * @author BOYDMR
 *
 */
public class LocalHostTest {
    private Service service = null;
    private int sitePort;

    @BeforeClass
    public void setUpSimulator () throws Exception {
        // first clear any config residue
        Config cfg = new Config();

        // now set up the shim to verify empty headers are injected
    	System.getProperties().remove("non-existent-sys-prop");
        service = Service.getService("string:"
            + "<?xml version='1.0' encoding='UTF-8'?>"
        	+ "<?system-alias usr-src-props=non-existent-sys-prop default="
            + "\"xml="
            + " <users>"
            + "  <user name='ngiwb1' pwd='password1'/>"
            + " </users>"
        	+ "\"?>"
            + "<config console-port='auto' proxy-port='auto'>"
            + " <console-recording sso='true' rest='true' max-entries='100' enable-debug-logging='true' />"
            + " <sso-cookie name='lds-policy' domain='localhost' />"
            + " <proxy-timeout inboundMillis='400000' outboundMillis='400000'/>"
            + " <sso-traffic strip-empty-headers='true'>"
            + "  <by-site scheme='http' host='localhost' port='{{proxy-port}}'>"
            + "    <cctx-mapping cctx='/is-alive*' thost='127.0.0.1' tport='{{console-port}}' tpath='/is-alive*'/>"
            + "    <allow action='GET' cpath='/is-alive'/>"
            + "    <allow action='GET' cpath='/is-alive?*'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + " <user-source type='xml'>{{usr-src-props}}</user-source>"
            + "</config>");
        service.start();
        sitePort = Config.getInstance().getProxyPort();
    }

    @AfterClass
    public void tearDownSimulator() throws Exception {
        service.stop();
    }

    @Test
    public void test_signMeInOut() throws HttpException, IOException {
        ///// first try to hit without session 
        Config cfg = Config.getInstance();
        String endpointWSignin = "http://localhost:" + cfg.getProxyPort() + ImAliveHandler.IS_ALIVE_PATH;
        HttpClient client = new HttpClient();

        HttpMethod method = new GetMethod(endpointWSignin);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String content = method.getResponseBodyAsString();

        Assert.assertEquals(status, 302);
        Header loc = method.getResponseHeader("location");
        Assert.assertNotNull(loc, "location header not returned for redirect");
        Assert.assertTrue(loc.getValue().contains("localhost"), "location header does not contain 'localhost'");

        ///// now sign user in via console port
        String authEp = loc.getValue().replace("/admin/selectUser.jsp", "/admin/action/set-user/ngiwb1");
        HttpMethod authM = new GetMethod(authEp);
        authM.setFollowRedirects(false);

        status = client.executeMethod(authM);

        Assert.assertEquals(status, 302);
        Header setCk = authM.getResponseHeader("set-cookie");
        Assert.assertNotNull(setCk, "set-cookie header not in sign-in response");
        String rawCk = setCk.getValue();
        Assert.assertTrue(rawCk.contains("lds-policy="));
        // when using localhost the domain should NOT be set otherwise
        // browser will not submit back to server
        Assert.assertFalse(rawCk.toLowerCase().contains("domain="), "domian should not be set when cookie domain is 'localhost'");
        int start = rawCk.indexOf("lds-policy=") + "lds-policy".length();
        int end = rawCk.indexOf(";", start);
        String token = rawCk.substring(start + 1, end);

        ///// now try again with session and verify we get through
        method = new GetMethod(endpointWSignin);
        method.setFollowRedirects(false);
        method.setRequestHeader("cookie", "lds-policy=" + token);
        status = client.executeMethod(method);

        Assert.assertEquals(status, 200);
        content = method.getResponseBodyAsString();
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
    }
}
