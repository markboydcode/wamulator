package org.lds.sso.appwrap.proxy;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.TestUtilities;
import org.lds.sso.appwrap.conditions.evaluator.GlobalHeaderNames;
import org.lds.sso.appwrap.ui.ImAliveHandler;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class SignMeInOutTest {

    private Service service;

    @BeforeClass
    public void setUpSimulator() throws Exception {
        // clear out any config residue from other tests
        new Config();

    	System.getProperties().remove("non-existent-sys-prop");
        StringBuffer config = new StringBuffer("string:")
        .append("<?system-alias policy-src-xml=non-existent-sys-prop default=")
        .append("\"xml=")
        .append("<deployment>")
        .append("  <environment id='dev' host='dev.lds.org (exposee)' />")
        .append(" <application id='not-used-by-wamulator/' authHost='not-used-by-wamulator' cctx='/'>")
        .append(" <authentication scheme='anonymous' name='default-anonymous' />")
        .append("   <authorization>")
        .append("    <rule name='Allow Authenticated Users' enabled='true' allow-takes-precedence='true'>")
        .append("     <allow>")
        .append("      <condition type='role' value='Anyone' />")
        .append("     </allow>")
        .append("    </rule>")
        .append("   </authorization>")
        .append("   <policy name='is-alive{/.../*,*}'>")
        .append("    <url>is-alive{/.../*,*}</url>")
        .append("    <operations>GET</operations>")
        .append("    <authentication scheme='anonymous' name='default-anonymous' />")
        .append("   </policy>")
        .append("  </application>")
        .append("</deployment>")
        .append("\"?>")
        .append("<?system-alias usr-src-props=non-existent-sys-prop default=")
        .append("\"xml=")
        .append(" <users>")
        .append("  <user name='user1' pwd='pwd'/>")
        .append("  <user name='user2' pwd='pwd'/>")
        .append(" </users>")
        .append("\"?>")
        .append("<config console-port='auto' proxy-port='auto' rest-version='CD-OESv1'>")
        .append(" <sso-cookie name='lds-policy' domain='.lds.org'/>")
        .append(" <sso-traffic>")
        .append("  <by-site scheme='http' host='localhost.lds.org' port='{{proxy-port}}'>")
        .append("    <cctx-mapping thost='127.0.0.1' tport='{{console-port}}'>")
        .append("      <policy-source>{{policy-src-xml}}</policy-source>")
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
    public void test_signMeInOut() throws IOException {
        ///// first try to hit without session but with signin indicator
        Config cfg = Config.getInstance();
        String endpointWSignin = "http://localhost.lds.org:" + cfg.getProxyPort() + ImAliveHandler.IS_ALIVE_PATH + "?" + GlobalHeaderNames.SIGNIN_VALUE;

        CloseableHttpClient wmltProxiedClient = TestUtilities.createWamulatorProxiedHttpClient(cfg.getProxyPort());
        HttpGet get = new HttpGet(endpointWSignin);
        CloseableHttpResponse response = wmltProxiedClient.execute(get);
        int status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 302);
        Header[] locs = response.getHeaders("location");
        Assert.assertNotNull(locs, "location header not returned for redirect");
        Assert.assertEquals(locs.length, 1, "should only be one location header for redirect");
        Header loc = locs[0];
        response.close();

        ///// now sign user in via console port
        HttpGet signin = new HttpGet("http://localhost.lds.org:" + cfg.getConsolePort() + cfg.getWamulatorServiceUrlBase() + "/action/set-user/user1");
        CloseableHttpClient straightConnectingClient = TestUtilities.createNonProxiedHttpClient();
        response = straightConnectingClient.execute(signin);
        status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 200);
        Header[] sets = response.getHeaders("set-cookie");
        Assert.assertNotNull(sets, "set-cookie header not in sign-in response");
        Assert.assertEquals(sets.length, 1, "should only be one set-cookie header in sign-in response");
        Header hdr = sets[0];
        HeaderElement[] elms = hdr.getElements();
        HeaderElement elm = elms[0];
        Assert.assertEquals(elm.getName(), cfg.getCookieName(), "Cookie name should be " + cfg.getCookieName());
        String token = elm.getValue();
        response.close();

        ///// now try again with session and verify we get through
        HttpGet accessIt = new HttpGet(endpointWSignin);
        accessIt.addHeader("cookie", "lds-policy=" + token);
        response = wmltProxiedClient.execute(accessIt);
        status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 200);
        String content = TestUtilities.readHttpComponentsStringEntity(response.getEntity());
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
        response.close();

        ////// now hit with session and signout indicator
        String endpointWSignout = "http://localhost.lds.org:" + cfg.getProxyPort() + ImAliveHandler.IS_ALIVE_PATH + "?" + GlobalHeaderNames.SIGNOUT_VALUE;
        HttpGet signout = new HttpGet(endpointWSignout);
        signout.addHeader("cookie", "lds-policy=" + token);

        response = wmltProxiedClient.execute(signout);
        status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 302);
        System.out.println("---- 1");
        sets = response.getHeaders("set-cookie");
        Assert.assertNotNull(sets, "set-cookie header not in sign-out response");
        Assert.assertEquals(sets.length, 1, "should only be one set-cookie header in sign-out response");
        System.out.println("---- 2");
        hdr = sets[0];
        elms = hdr.getElements();
        elm = elms[0];
        Assert.assertTrue(elm.getValue().contains("cookie-monster"), "cookie for clearing not sent");
        System.out.println("---- 3");

        ///// now if we hit again with signout indicator it should go through since session terminated
        HttpGet last = new HttpGet(endpointWSignout);

        // note that we are hitting proxy port due to hostconfig above
        response = wmltProxiedClient.execute(last);
        status = response.getStatusLine().getStatusCode();
        Assert.assertEquals(status, 200, "should have allowed request through");
        System.out.println("---- 4");

        content = TestUtilities.readHttpComponentsStringEntity(response.getEntity());

        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
        System.out.println("---- 5");
        response.close();
        wmltProxiedClient.close();
    }

    @Test
    public void test_isAlive() throws IOException {
        System.out.println("---- 6");

        Config cfg = Config.getInstance();
        String endpoint = "http://localhost.lds.org:" + cfg.getConsolePort() + cfg.getWamulatorServiceUrlBase() + "/is-alive";

        CloseableHttpClient client = TestUtilities.createWamulatorProxiedHttpClient(cfg.getConsolePort());

        HttpGet method = new HttpGet(endpoint);
        CloseableHttpResponse response = client.execute(method);
        int status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 200);
        System.out.println("---- 7");

        HttpEntity entity = response.getEntity();
        String content = TestUtilities.readHttpComponentsStringEntity(entity);

        Assert.assertNotNull(content);
        System.out.println("---- 8");

        boolean hasIt = content.contains(ImAliveHandler.IS_ALIVE);
        Assert.assertTrue(hasIt, "missing is alive output text.");
        System.out.println("---- 9");

    }

}
