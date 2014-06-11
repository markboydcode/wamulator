package org.lds.sso.appwrap;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Tests that start the simulator with the sample config files embedded with the
 * simulator to ensure that changes don't break these files. If they do then
 * things need to be fixed allowing them to keep working or they must be updated
 * accordingly along with any tutorials the indicate their content and use.
 *
 * @author BoydMR
 *
 */
public class ConfigSamplesVerificationTest {

    @Test
    public void test_ConsoleOnly_xml() throws Exception {

        new Config(); // clear out old config
        String xml = "<?xml version='1.0' encoding='UTF-8'?>" +
                "<config proxy-port='auto' console-port='auto' rest-version='CD-OESv1'>" +
                "    <console-recording sso='true' rest='true' max-entries='1000' enable-debug-logging='true'/>" +
                "    <sso-traffic>" +
                "      <by-site host='local.lds.org' port='{{proxy-port}}'>" +
                "      </by-site>" +
                "    </sso-traffic>" +
                "</config>";
        Service service = Service.getService("string:" + xml);
        service.start();
        Config cfg = Config.getInstance();
        hitRest_getCookieName("http://127.0.0.1:" + cfg.getConsolePort() + Config.WAMULATOR_SERVICE_BASE + "/oes/v1.0/rest/local.lds.org/getCookieName");
        service.stop();
    }

    /**
     * Not thread safe intentionally. Only one thread should be romping through
     * the tests in this file at a time.
     * @throws IOException
     * @throws HttpException
     */
    private void hitRest_getCookieName(String endpoint) throws IOException {
        Config cfg = Config.getInstance();

        // hit the getCookieName rest endpoint
        CloseableHttpClient client = TestUtilities.createNonProxiedHttpClient();
        HttpGet method = new HttpGet(endpoint);
        CloseableHttpResponse response = client.execute(method);
        int status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 200);
        String content = TestUtilities.readHttpComponentsStringEntity(response.getEntity());
        Assert.assertNotNull(content);
        Assert.assertEquals(content, cfg.getCookieName());
    }

    @Test
    public void test_ConsoleOnly_ldsPolicyCookie_xml() throws Exception {
        new Config(); // clear out old stuff
        Config cfg = Config.getInstance();
        String xml = "<?xml version='1.0' encoding='UTF-8'?>" +
                "<config proxy-port='auto' console-port='auto' rest-version='CD-OESv1'>" +
                "    <console-recording sso='true' rest='true' max-entries='1000' enable-debug-logging='true'/>" +
                "    <sso-cookie name='lds-policy' domain='.lds.org'/>" +
                "    <sso-traffic>" +
                "    <by-site host='local.lds.org' port='{{proxy-port}}'>" +
                "    </by-site>" +
                "    </sso-traffic>" +
                "</config>";
        Service service = Service.getService("string:" + xml);
        service.start();
        Assert.assertEquals(cfg.getCookieName(), "lds-policy");
        hitRest_getCookieName("http://127.0.0.1:" + cfg.getConsolePort() + Config.WAMULATOR_SERVICE_BASE + "/oes/v1.0/rest/local.lds.org/getCookieName");
        service.stop();
    }

    @Test
    public void test_sesFileServingSample_xml() throws Exception {

        Config cfg = Config.getInstance();
        StringBuffer xmlBuf = new StringBuffer()
            .append("<?xml version='1.0' encoding='UTF-8'?>")
            .append("<config proxy-port='auto' console-port='auto' rest-version='CD-OESv1'>")
            .append("<console-recording sso='true' rest='true' max-entries='1000' enable-debug-logging='true'/>")
            .append("<sso-cookie name='lds-policy' domain='.lds.org'/>")
            .append("<sso-traffic>")
            .append("<by-site host='local.lds.org' port='{{proxy-port}}'>")
            .append("<cctx-file cctx='/church-calendar/services/lucrs/evt/0/*' ")
            .append("   file='classpath:config-samples/ngiwb1-cal-data.json' ")
            .append("   content-type='application/json'/>")
            .append("</by-site>")
            .append("</sso-traffic>")
            .append("</config>");

        Service service = Service.getService("string:" + xmlBuf.toString());
        service.start();

        // first hit URL and verify file contents served correctly
        String endpoint = "http://local.lds.org:" + cfg.getProxyPort() + "/church-calendar/services/lucrs/evt/0/";

        CloseableHttpClient client = TestUtilities.createWamulatorProxiedHttpClient(cfg.getProxyPort());
        HttpGet method = new HttpGet(endpoint);
        CloseableHttpResponse response = client.execute(method);
        int status = response.getStatusLine().getStatusCode();

        Assert.assertEquals(status, 200);
        Header[] hdrs = response.getHeaders("content-type");
        Assert.assertEquals(hdrs[0].getValue(), "application/json");

        String content = TestUtilities.readHttpComponentsJsonEntityAsString(response.getEntity());
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains("b-ball game"), "json text served is missing the basketball event.");

    }
}
