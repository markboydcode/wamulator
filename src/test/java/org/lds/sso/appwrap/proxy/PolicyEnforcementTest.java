package org.lds.sso.appwrap.proxy;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.impl.client.CloseableHttpClient;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.TestUtilities;
import org.lds.sso.appwrap.ui.ImAliveHandler;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class PolicyEnforcementTest {

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
        .append("  <authentication scheme='login' name='default-login-access' />")
        .append("  <authorization failure-redirect-url='denied.html'>")
        .append("    <default format='exposee' value='Allow Authenticated Users'>")
        .append("      <headers>")
        .append("        <success>")
        .append("        </success>")
        .append("        <failure>")
        .append("          <redirect value='/denied.html'/>")
        .append("        </failure>")
        .append("      </headers>")
        .append("    </default>")
        .append("    <rule name='Allow Authenticated Users' enabled='true' allow-takes-precedence='true'>")
        .append("     <allow>")
        .append("      <condition type='role' value='Anyone' />")
        .append("     </allow>")
        .append("    </rule>")
        .append("  </authorization>")
        .append("  <policy name='wamulator/service/is-alive_OPTIONS'>")
        .append("   <url>wamulator/service/is-alive</url>")
        .append("   <operations>OPTIONS</operations>")
        .append("   <authentication scheme='anonymous' name='unenforceD-for-OPTIONS-method' />")
        .append("  </policy>")
        .append(" </application>")
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
    public void test_isAlive_OPTIONS_anonymous() throws IOException {
        ///// hit without session using OPTIONS method
        Config cfg = Config.getInstance();
        String isAlive = "http://localhost.lds.org:" + cfg.getProxyPort() + ImAliveHandler.IS_ALIVE_PATH;

        CloseableHttpClient wmltProxiedClient = TestUtilities.createWamulatorProxiedHttpClient(cfg.getProxyPort());
        HttpOptions viaOptions = new HttpOptions(isAlive);
        CloseableHttpResponse response = wmltProxiedClient.execute(viaOptions);
        int status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 200, "is-alive via OPTIONS method should be anonymously accessible");
        HttpEntity entity = response.getEntity();
        String content = TestUtilities.readHttpComponentsStringEntity(entity);

        Assert.assertNotNull(content, "is-alive via OPTIONS method should have returned content");

        boolean hasIt = content.contains(ImAliveHandler.IS_ALIVE);
        Assert.assertTrue(hasIt, "is-alive via OPTIONS method should have contained '" + ImAliveHandler.IS_ALIVE +"'");
        response.close();
    }


    @Test
    public void test_isAlive_GET_requires_authN() throws IOException {
        ///// hit without session using GET method
        Config cfg = Config.getInstance();
        String isAlive = "http://localhost.lds.org:" + cfg.getProxyPort() + ImAliveHandler.IS_ALIVE_PATH;

        CloseableHttpClient wmltProxiedClient = TestUtilities.createWamulatorProxiedHttpClient(cfg.getProxyPort());
        HttpGet viaGet = new HttpGet(isAlive);
        CloseableHttpResponse response = wmltProxiedClient.execute(viaGet);
        int status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 302, "is-alive via GET method should require authN");
        Header[] locs = response.getHeaders("location");
        Assert.assertNotNull(locs, "location header not returned for redirect to sign-in page");
        Assert.assertEquals(locs.length, 1, "should only be one location header for redirect");
        System.out.println("------ location: " + locs[0]);
        Assert.assertTrue(locs[0].getValue().contains(Config.WAMULATOR_SIGNIN_PAGE_PATH),
                "location header should have contained '" + Config.WAMULATOR_SIGNIN_PAGE_PATH + "'");
        response.close();






//        ///// now sign user in via console port
//        HttpGet signin = new HttpGet("http://localhost.lds.org:" + cfg.getConsolePort() + cfg.getWamulatorServiceUrlBase() + "/action/set-user/user1");
//        CloseableHttpClient straightConnectingClient = TestUtilities.createNonProxiedHttpClient();
//        response = straightConnectingClient.execute(signin);
//        status = response.getStatusLine().getStatusCode();
//
//        Assert.assertEquals(status, 200);
//        Header[] sets = response.getHeaders("set-cookie");
//        Assert.assertNotNull(sets, "set-cookie header not in sign-in response");
//        Assert.assertEquals(sets.length, 1, "should only be one set-cookie header in sign-in response");
//        Header hdr = sets[0];
//        HeaderElement[] elms = hdr.getElements();
//        HeaderElement elm = elms[0];
//        Assert.assertEquals(elm.getName(), cfg.getCookieName(), "Cookie name should be " + cfg.getCookieName());
//        String token = elm.getValue();
//        response.close();
//
//        ///// now try again with session and verify we get through
//        HttpGet accessIt = new HttpGet(endpointWSignin);
//        accessIt.addHeader("cookie", "lds-policy=" + token);
//        response = wmltProxiedClient.execute(accessIt);
//        status = response.getStatusLine().getStatusCode();
//
//        Assert.assertEquals(status, 200);
//        String content = TestUtilities.readHttpComponentsStringEntity(response.getEntity());
//        Assert.assertNotNull(content);
//        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
//        response.close();
//
//        ////// now hit with session and signout indicator
//        String endpointWSignout = "http://localhost.lds.org:" + cfg.getProxyPort() + ImAliveHandler.IS_ALIVE_PATH + "?" + GlobalHeaderNames.SIGNOUT_VALUE;
//        HttpGet signout = new HttpGet(endpointWSignout);
//        signout.addHeader("cookie", "lds-policy=" + token);
//
//        response = wmltProxiedClient.execute(signout);
//        status = response.getStatusLine().getStatusCode();
//
//        Assert.assertEquals(status, 302);
//        System.out.println("---- 1");
//        sets = response.getHeaders("set-cookie");
//        Assert.assertNotNull(sets, "set-cookie header not in sign-out response");
//        Assert.assertEquals(sets.length, 1, "should only be one set-cookie header in sign-out response");
//        System.out.println("---- 2");
//        hdr = sets[0];
//        elms = hdr.getElements();
//        elm = elms[0];
//        Assert.assertTrue(elm.getValue().contains("cookie-monster"), "cookie for clearing not sent");
//        System.out.println("---- 3");
//
//        ///// now if we hit again with signout indicator it should go through since session terminated
//        HttpGet last = new HttpGet(endpointWSignout);
//
//        // note that we are hitting proxy port due to hostconfig above
//        response = wmltProxiedClient.execute(last);
//        status = response.getStatusLine().getStatusCode();
//        Assert.assertEquals(status, 200, "should have allowed request through");
//        System.out.println("---- 4");
//
//        content = TestUtilities.readHttpComponentsStringEntity(response.getEntity());
//
//        Assert.assertNotNull(content);
//        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
//        System.out.println("---- 5");
//        response.close();
//        wmltProxiedClient.close();
    }

    @Test
    public void test_isAlive_GET_accessible_after_authN() throws IOException {
        ///// hit without session using GET method
        Config cfg = Config.getInstance();
        String isAlive = "http://localhost.lds.org:" + cfg.getProxyPort() + ImAliveHandler.IS_ALIVE_PATH;
        CloseableHttpClient wmltProxiedClient = TestUtilities.createWamulatorProxiedHttpClient(cfg.getProxyPort());

        ///// now sign user in via console port
        HttpGet signin = new HttpGet("http://localhost.lds.org:" + cfg.getConsolePort()
                + cfg.getWamulatorServiceUrlBase() + "/action/set-user/user1");
        CloseableHttpClient straightConnectingClient = TestUtilities.createNonProxiedHttpClient();
        CloseableHttpResponse response = straightConnectingClient.execute(signin);
        int status = response.getStatusLine().getStatusCode();

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

        ///// now is-alive with session and verify we get through
        HttpGet accessIt = new HttpGet(isAlive);
        accessIt.addHeader("cookie", "lds-policy=" + token);
        response = wmltProxiedClient.execute(accessIt);
        status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 200);
        String content = TestUtilities.readHttpComponentsStringEntity(response.getEntity());
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
        response.close();
        wmltProxiedClient.close();
    }

//    @Test
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
        System.out.println("---- 9***");

    }

}
