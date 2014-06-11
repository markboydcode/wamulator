package org.lds.sso.appwrap.proxy;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.lds.sso.appwrap.AppEndPoint;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.TestUtilities;
import org.lds.sso.appwrap.conditions.evaluator.GlobalHeaderNames;
import org.lds.sso.appwrap.proxy.header.HeaderDef;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServiceUrlInjected4RootUrlTest {
    protected static final String MSG4_404_TEST = "don't request this resource. it isn't there.";
    private Service service = null;
    private Thread server = null;
    private int sitePort;
    private int serverPort;
    private CloseableHttpClient client;
    private int freePort;
	private int consolePort;
	private int proxyPort;

    private void setUpTestServer() throws IOException {
        // get socket of server emulator
        final ServerSocket sss = new ServerSocket();
        sss.setReuseAddress(true);
        sss.bind(null);
        serverPort = sss.getLocalPort();
        System.out.println("RequestHandlerIntegrationTest server-port = " + serverPort);

        // now start server to spool back responses to various requests
        server = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Socket sock = sss.accept();
                        InputStream is = sock.getInputStream();
                        String input = null;
                        try {
                            input = TestUtilities.readAllHttpHeaders(is);
                        } catch(Exception e) {
                            System.out.println("---> test server encountered exception reading stream listening for next connection.");
                            e.printStackTrace();
                            continue;
                        }
                        String inputLC = input.toLowerCase();
                        OutputStream out = sock.getOutputStream();
                        String req = null;
                        String output = null;
                        String answer = null;
                        String hostHdrLC = HeaderDef.Host.getNameWithColon().toLowerCase();
                        boolean alreadyHandled = false;

                        if (input.startsWith("GET /global/some/path/ HTTP/1.1") ||
                        		input.startsWith("GET / HTTP/1.1") ||
                        		input.startsWith("GET /resources/css/pages/home.css HTTP/1.1")) {
                        	answer = GlobalHeaderNames.SERVICE_URL + ": ???";

                            String hdrKey = RequestHandler.CRLF + GlobalHeaderNames.SERVICE_URL + ":";
                            int idx = inputLC.indexOf(hdrKey);

                            if (idx != -1) {
                                int cr = input.indexOf(RequestHandler.CRLF, idx+1);
                                String val = null;
                                if (cr == -1) {
                                    // last header
                                    val = input.substring(idx+hdrKey.length()).trim();
                                }
                                else {
                                    val = input.substring(idx+hdrKey.length(), cr).trim();
                                }
                                answer = GlobalHeaderNames.SERVICE_URL + ": " + val;
                            }
                            req = "policy-service-url-injection-test";
                        }
                        else {
                            req = "UNEXPECTED";
                            output =
                                "HTTP/1.1 500 Internal Server Error due to unexpected request" + RequestHandler.CRLF;
                        }

                        if (! alreadyHandled) {
                            if (output == null) {
                                output =
                                    "HTTP/1.1 200 OK" + RequestHandler.CRLF
                                    + "Content-type: text/plain" + RequestHandler.CRLF
                                    + "Content-length: " + answer.toCharArray().length + RequestHandler.CRLF
                                    + RequestHandler.CRLF
                                    + answer;
                            }

                            System.out.println();
                            System.out.println("*** test server detected request: " + req);
                            System.out.println("--- received ---");
                            System.out.println(input);
                            System.out.println("--- returned ---");
                            System.out.println(output);

                            // add header/body termination indicators.
                            output += RequestHandler.CRLF + RequestHandler.CRLF;

                            out.write(output.getBytes());
                            out.flush();
                            is.close();
                            out.close();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Server test thread incurred Exception, exiting.");
                    e.printStackTrace();
                }

                if (sss != null && sss.isBound()) {
                    try {
                        sss.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        server.start();
    }

    @BeforeClass
    public void setUpSimulator () throws Exception {
        // first clear any config residue
        Config cfg = new Config();

        // spin up the backend test server fronted by the simulator
        this.setUpTestServer();

        // now set up the shim to verify various handling characteristics
        System.getProperties().remove("non-existent-sys-prop"); // make sure such a prop doesn't exist so uses defaults
        StringBuffer sb = new StringBuffer()
                .append("string:<?file-alias policy-src-xml=non-existent-sys-prop default=")
                .append("\"xml=")
                .append("<deployment at='2012-11-30_11:00:46.208-0700'>")
                .append(" <environment id='dev' host='dev.lds.org (exposee)' />")
                .append(" <application id='local.lds.org/' authHost='labs-local.lds.org' cctx='/'>")
                .append("  <authentication scheme='login' name='WAM-DEV LDS Login Form' />")
                .append("  <authorization>")
                .append("   <default format='exposee' value='Allow Authenticated Users'></default>")
                .append("   <rule name='Allow Authenticated Users' enabled='true' allow-takes-precedence='true'>")
                .append("    <allow>")
                .append("     <condition type='role' value='Anyone' />")
                .append("    </allow>")
                .append("   </rule>")
                .append("  </authorization>")
                .append("  <policy name='global/{/.../*,*}'>")
                .append("   <url>global/{/.../*,*}</url>")
                .append("   <operations>GET,POST</operations>")
                .append("   <authentication scheme='login' name='WAM-DEV LDS Login Form' />")
                .append("   <authorization format='exposee' value='Allow Authenticated Users'/>")
                .append("  </policy>")
                .append(" </application>")
                .append("</deployment>")
                .append("\"?>")
                .append("<?system-alias user-src-props=non-existent-sys-prop default=")
                .append("\"xml=")
                .append(" <users>")
                .append("  <user name='ngiwb2'>")
                .append("   <Att name='apps' value='aaa,bbb,ccc'/>")
                .append("   <Att name='preferredname' value='Jay 金&lt;script>alert(0)&lt;/script>虬 Admin'/>")
                .append("   <Att name='policy-givenname' value='Jay Admin'/>")
                .append("   <Att name='policy-preferredlanguage' value='eng'/>")
                .append("   <Att name='policy-preferred-name' value='Jay 金&lt;script>alert(0)&lt;/script>虬 Admin'/>")
                .append("   <Att name='policy-given-name' value='Jay Admin'/>")
                .append("   <Att name='policy-preferred-language' value='eng'/>")
                .append("   <Att name='policy-preferred_name' value='Jay 金&lt;script>alert(0)&lt;/script>虬 Admin'/>")
                .append("   <Att name='policy-given_name' value='Jay Admin'/>")
                .append("   <Att name='policy-preferred_language' value='eng'/>")
                .append("  </user>")
                .append(" </users>")
                .append("\"?>")
                .append("<config console-port='auto' proxy-port='auto'>")
                .append(" <console-recording sso='true' rest='true' max-entries='100' enable-debug-logging='true' />")
                .append(" <sso-cookie name='lds-policy' domain='.ldschurch.org' />")
                .append(" <proxy-timeout inboundMillis='400000' outboundMillis='400000'/>")
                .append(" <sso-traffic strip-empty-headers='true'>")
                .append("  <by-site scheme='http' host='local.ldschurch.org' port='{{proxy-port}}'>")
                .append("   <cctx-mapping thost='127.0.0.1' tport='")
                .append(serverPort)
                .append("'>")
                .append("    <policy-source>{{policy-src-xml}}</policy-source>")
                .append("   </cctx-mapping>")
                .append("  </by-site>")
                .append(" </sso-traffic>")
                .append(" <user-source type='xml'>{{user-src-props}}</user-source>")
                .append("</config>");
        service = Service.getService(sb.toString());
        service.start();
        sitePort = Config.getInstance().getProxyPort();
        client = TestUtilities.createWamulatorProxiedHttpClient(sitePort);
    }

	@AfterClass
    public void tearDownSimulator() throws Exception {
        client.close();
        service.stop();
        server.interrupt();
    }

    @Test
    public void test_policy_service_url_injected_4_globalpath() throws IOException {
    	Config cfg = Config.getInstance();
        System.out.println("----> test_policy_service_url_injected_4_globalpath ");
        String token = TestUtilities.authenticateUser("ngiwb2", "local.ldschurch.org");
        String uri = "http://local.ldschurch.org:" + sitePort + "/global/some/path/";

        HttpGet method = new HttpGet(uri);
        method.addHeader("cookie", cfg.getCookieName() + "=" + token);
        CloseableHttpResponse response = client.execute(method);
        int status = response.getStatusLine().getStatusCode();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");

        String content = TestUtilities.readHttpComponentsStringEntity(response.getEntity());
        String expected = GlobalHeaderNames.SERVICE_URL + ": " + AppEndPoint.getPolicyServiceUrlHeaderValue(cfg, "local.ldschurch.org", null);

        Assert.assertEquals(content, expected);
        response.close();
    }

    @Test
    public void test_policy_service_url_injected_4_root_path() throws IOException {
    	Config cfg = Config.getInstance();
        System.out.println("----> test_policy_service_url_injected_4_rootpath ");
        String token = TestUtilities.authenticateUser("ngiwb2", "local.ldschurch.org");
        String uri = "http://local.ldschurch.org:" + sitePort + "/";

        HttpGet method = new HttpGet(uri);
        method.addHeader("cookie", cfg.getCookieName() + "=" + token);
        CloseableHttpResponse response = client.execute(method);
        int status = response.getStatusLine().getStatusCode();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");

        String content = TestUtilities.readHttpComponentsStringEntity(response.getEntity());
        String expected = GlobalHeaderNames.SERVICE_URL + ": " + AppEndPoint.getPolicyServiceUrlHeaderValue(cfg, "local.ldschurch.org", null);

        Assert.assertEquals(content, expected);
        response.close();
    }

    @Test
    public void test_policy_service_url_injected_4_root_path_match() throws IOException {
    	Config cfg = Config.getInstance();
        System.out.println("----> test_policy_service_url_injected_4_root_path_match ");
        String token = TestUtilities.authenticateUser("ngiwb2", "local.ldschurch.org");
        String uri = "http://local.ldschurch.org:" + sitePort + "/resources/css/pages/home.css";

        CloseableHttpClient client = TestUtilities.createWamulatorProxiedHttpClient(sitePort);
        HttpGet method = new HttpGet(uri);
        method.addHeader("cookie", cfg.getCookieName() + "=" + token);
        CloseableHttpResponse response = client.execute(method);
        int status = response.getStatusLine().getStatusCode();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");

        String content = TestUtilities.readHttpComponentsStringEntity(response.getEntity());
        String expected = GlobalHeaderNames.SERVICE_URL + ": " + AppEndPoint.getPolicyServiceUrlHeaderValue(cfg, "local.ldschurch.org", null);

        Assert.assertEquals(content, expected);
        response.close();
    }

}
