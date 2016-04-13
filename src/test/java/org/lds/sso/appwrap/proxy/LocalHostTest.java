package org.lds.sso.appwrap.proxy;

import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.util.EntityUtils;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.ui.ImAliveHandler;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

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
            + "<?file-alias policy-src-xml=non-existent-sys-prop default="
            + "\"xml="
            + "<deployment at='2012-11-30_11:00:46.208-0700'>"
            + " <environment id='dev' host='dev.lds.org (exposee)' />"
            + " <application id='localhost/' authHost='localhost' cctx='/'>"
            + "  <authentication scheme='anonymous' name='default-anonymous' />"
            + "  <authorization>"
            + "   <rule name='Allow Authenticated Users' enabled='true' allow-takes-precedence='true'>"
            + "    <allow>"
            + "     <condition type='role' value='Anyone' />"
            + "    </allow>"
            + "   </rule>"
            + "  </authorization>"
            + "  <policy name='is-alive{/.../*,*}'>"
            + "   <url>/wamulator/service/is-alive{/.../*,*}</url>"
            + "   <operations>GET</operations>"
            + "   <authentication scheme='login' name='WAM-DEV LDS Login Form' />"
            + "   <authorization format='exposee' value='Allow Authenticated Users'>"
            + "    <headers>"
            + "     <success>"
            + "      <fixed-value name='policy-fixed-value' value='test-value' type='HeaderVar' />"
            + "      <profile-att name='policy-ldspositions' attribute='ldsposv2' type='HeaderVar' />"
            + "      <profile-att name='policy-ldsunits' attribute='ldsunit' type='HeaderVar' />"
            + "     </success>"
            + "     <failure>"
            + "      <redirect value='/denied.html' />"
            + "     </failure>"
            + "    </headers>"
            + "   </authorization>"
            + "  </policy>"
            + " </application>"
            + "</deployment>"
            + "\"?>"
        	+ "<?system-alias usr-src-props=non-existent-sys-prop default="
            + "\"xml="
            + " <users>"
            + "  <user name='ngiwb1' pwd='password1'/>"
            + " </users>"
        	+ "\"?>"
            + "<config console-port='auto' proxy-port='auto'>"
            + " <console-recording sso='true' rest='true' max-entries='100' enable-debug-logging='true' />"
            + " <sso-cookie name='lds-policy' domain='localhost' />"
            + " <port-access local-traffic-only='false' />"
            + " <proxy-timeout inboundMillis='400000' outboundMillis='400000'/>"
            + " <sso-traffic strip-empty-headers='true'>"
            + "  <by-site scheme='http' host='localhost' port='{{proxy-port}}'>"
            + "    <cctx-mapping thost='127.0.0.1' tport='{{console-port}}'>"
            + "      <policy-source>{{policy-src-xml}}</policy-source>"
            + "    </cctx-mapping>"
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
    public void test_signMeInOut() throws IOException {
        System.out.println("----> test_signMeInOut");
        ///// first try to hit without session 
        Config cfg = Config.getInstance();

        SocketConfig scd = SocketConfig.DEFAULT;
        System.out.println("default socket config for http client is: " + scd.toString());
        String endpointWSignin = "http://localhost:" + cfg.getProxyPort() + ImAliveHandler.IS_ALIVE_PATH;
        CloseableHttpClient client = HttpClients.custom()
                .disableAutomaticRetries()
                .disableRedirectHandling()
                .setDefaultSocketConfig(
                        // ensures that if a server error drops the connection or doesn't
                        // respond we don't wait for the default 30 seconds of inactivity
                        // before TCP throwing a socket timeout error.
                        SocketConfig.custom().setSoTimeout(1000).build()
                )
                .build();

        HttpGet get = new HttpGet(endpointWSignin);
        CloseableHttpResponse response = client.execute(get);
        int status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 302, "Should have redirected to sign-in page");
        Header[] locs = response.getHeaders("location");
        Assert.assertNotNull(locs, "no location header found in response");
        Assert.assertTrue(locs.length >= 1, "0 location headers found in response");

        Header loc = locs[0]; // will only ever be one location header in a 302 redirect
        Assert.assertTrue(loc.getValue().contains("localhost"), "location header does not contain 'localhost'");

        /// now sign user in via console port
        String authEp = loc.getValue().replace(Config.WAMULATOR_SIGNIN_PAGE_PATH, cfg.getWamulatorServiceUrlBase() + "/action/set-user/ngiwb1");
        HttpGet authM = new HttpGet(authEp);
        response  = client.execute(authM);
        status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 302, "should have redirected after sign-in");

        // when using localhost the domain should NOT be set otherwise
        // browser will not submit back to server
        HeaderIterator scItr = response.headerIterator("set-cookie");
        Assert.assertTrue(scItr.hasNext(), "no set-cookie header found in sign-in response");
        HeaderElementIterator scEItr = new BasicHeaderElementIterator(scItr);

        boolean foundSignInCk = false;
        String token = null;

        while(scEItr.hasNext()) {
            HeaderElement elem = scEItr.nextElement();
            if (elem.getName().equals("lds-policy")){
                foundSignInCk = true;
                NameValuePair d = elem.getParameterByName("domain");
                Assert.assertNull(d, "domain should not be set when cookie domain is 'localhost'");
                token = elem.getValue();
            }
        }

        // now try again with session and verify we get through
        // cookie is being held in the client
        HttpGet get2 = new HttpGet(endpointWSignin);
        response = client.execute(get2);
        status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 200);
        HttpEntity e = response.getEntity();

        String content = EntityUtils.toString(e);
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains(ImAliveHandler.IS_ALIVE), "missing is alive output text.");
    }
}
