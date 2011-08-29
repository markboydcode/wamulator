package org.lds.sso.appwrap;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.testng.Assert;
import org.testng.annotations.Test;

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
        Service service = Service.getService("classpath:config-samples/console-only.xml");
        service.start();
        Config cfg = Config.getInstance();
        hitRest_getCookieName("http://localhost:" + cfg.getConsolePort() + "/oes/v1.0/rest/local.lds.org/getCookieName");
        service.stop();
    }

    /**
     * Not thread safe intentionally. Only one thread should be romping through
     * the tests in this file at a time.
     * @throws IOException
     * @throws HttpException
     */
    private void hitRest_getCookieName(String endpoint) throws HttpException, IOException {
        Config cfg = Config.getInstance();

        // hit the getCookieName rest endpoint
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);

        Assert.assertEquals(status, 200);
        String content = method.getResponseBodyAsString();
        Assert.assertNotNull(content);
        Assert.assertEquals(content, cfg.getCookieName());
    }

    @Test
    public void test_ConsoleOnly_ldsPolicyCookie_xml() throws Exception {
        new Config(); // clear out old stuff
        Config cfg = Config.getInstance();
        Service service = Service.getService("classpath:config-samples/console-only-lds-policy-cookie.xml");
        service.start();
        Assert.assertEquals(cfg.getCookieName(), "lds-policy");
        hitRest_getCookieName("http://127.0.0.1:" + cfg.getConsolePort() + "/oes/v1.0/rest/local.lds.org/getCookieName");
        service.stop();
    }

    @Test
    public void test_sesFileServingSample_xml() throws Exception {

        Config cfg = Config.getInstance();
        Service service = Service.getService("classpath:config-samples/ses_file_serving_sample.xml");
        service.start();

        // first hit URL and verify file contents served correctly
        String endpoint = "http://127.0.0.1:" + cfg.getProxyPort() + "/church-calendar/services/lucrs/evt/0/";

        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);

        Assert.assertEquals(status, 200);
        String content = method.getResponseBodyAsString();
        Assert.assertNotNull(content);
        Assert.assertTrue(content.contains("b-ball game"), "json text served is missing the basketball event.");

        Header hdr = method.getResponseHeader("content-type");
        Assert.assertEquals(hdr.getValue(), "application/json");
    }
}
