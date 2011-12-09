package org.lds.sso.appwrap.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.TestUtilities;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.lds.sso.appwrap.proxy.header.HeaderDef;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ServiceUrlInjected4RootUrlTest {
    protected static final String MSG4_404_TEST = "don't request this resource. it isn't there.";
    private Service service = null;
    private Thread server = null;
    private int sitePort;
    private int serverPort;
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
                        	answer = UserHeaderNames.SERVICE_URL + ": ???";

                            String hdrKey = RequestHandler.CRLF + UserHeaderNames.SERVICE_URL + ":";
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
                                answer = UserHeaderNames.SERVICE_URL + ": " + val;
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

        // find a free port on which nothing is listening
        consolePort = getAvailablePort("console-port");
        proxyPort = getAvailablePort("proxy-port");
        // spin up the backend test server fronted by the simulator
        this.setUpTestServer();
        
        // now set up the shim to verify various handling characteristics
        service = Service.getService("string:"
            + "<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias console-port=" + consolePort + "?>"
            + "<?alias proxy-port=" + proxyPort + "?>"
            + "<config console-port='{{console-port}}' proxy-port='{{proxy-port}}'>"
            + " <console-recording sso='true' rest='true' max-entries='100' enable-debug-logging='true' />"
            + " <sso-cookie name='lds-policy' domain='.ldschurch.org' />"
            + " <proxy-timeout inboundMillis='400000' outboundMillis='400000'/>"
            + "<sso-sign-in-url value='http://local.ldschurch.org:{{console-port}}/admin/codaUserSelect.jsp'/>"
            + "<sso-header name='policy-service-url' value='http://local.ldschurch.org:{{console-port}}/rest/'/>"
            + "<sso-header name='connection' value='close'/>"
            + " <sso-traffic strip-empty-headers='true'>"
            + "  <by-site scheme='http' host='local.ldschurch.org' port='{{proxy-port}}'>"
            + "   <cctx-mapping cctx='/global*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/global*'/>"
            + "   <allow cpath='/global/*' action='GET,POST'/>"
            + "   <allow cpath='/global/*?*' action='GET,POST'/>"
            + "   <cctx-mapping cctx='/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/*'/>"
            + "   <allow cpath='/*' action='GET,POST'/>"
            + "   <allow cpath='/*?*' action='GET,POST'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + " <users>"
            + "  <user name='ngiwb2'>"
            + "   <ldsApplications value='aaa,bbb,ccc'/>"
            + "   <sso-header name='preferredname' value='Jay 金&lt;script>alert(0)&lt;/script>虬 Admin'/>"
            + "   <sso-header name='policy-givenname' value='Jay Admin'/>"
            + "   <sso-header name='policy-preferredlanguage' value='eng'/>"
            + "   <sso-header name='policy-preferred-name' value='Jay 金&lt;script>alert(0)&lt;/script>虬 Admin'/>"
            + "   <sso-header name='policy-given-name' value='Jay Admin'/>"
            + "   <sso-header name='policy-preferred-language' value='eng'/>"
            + "   <sso-header name='policy-preferred_name' value='Jay 金&lt;script>alert(0)&lt;/script>虬 Admin'/>"
            + "   <sso-header name='policy-given_name' value='Jay Admin'/>"
            + "   <sso-header name='policy-preferred_language' value='eng'/>"
            + "  </user>"
            + " </users>"
            + "</config>");
        service.start();
        sitePort = Config.getInstance().getProxyPort();
        System.out.println(); // to leave a gap before test output.


    }

    private int getAvailablePort(String portName) throws IOException {
    	int port = -1;
        ServerSocket portFinder = new ServerSocket();
        portFinder.setReuseAddress(true);
        portFinder.bind(null);
        port = portFinder.getLocalPort();
        try {
            portFinder.close();
        }
        catch(Exception e) {
            System.out.println("Exception releasing server port for use as " + portName + e);
        }
		return port;
	}

	@AfterClass
    public void tearDownSimulator() throws Exception {
        service.stop();
        server.interrupt();
    }

@Test
    public void test_policy_service_url_injected_4_globalpath() throws HttpException, IOException {
    	Config cfg = Config.getInstance();
        System.out.println("----> test_policy_service_url_injected_4_globalpath ");
        String token = TestUtilities.authenticateUser("ngiwb2", cfg.getConsolePort(), "local.ldschurch.org");
        String uri = "http://local.ldschurch.org:" + sitePort + "/global/some/path/";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setRequestHeader(new Header("cookie", cfg.getCookieName() + "=" + token));
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");
        Assert.assertEquals(response, "policy-service-url: http://local.ldschurch.org:" + cfg.getConsolePort() + "/oes/v{version}/rest/local.ldschurch.org/");
        method.releaseConnection();
    }

@Test
    public void test_policy_service_url_injected_4_root_path() throws HttpException, IOException {
    	Config cfg = Config.getInstance();
        System.out.println("----> test_policy_service_url_injected_4_rootpath ");
        String token = TestUtilities.authenticateUser("ngiwb2", cfg.getConsolePort(), "local.ldschurch.org");
        String uri = "http://local.ldschurch.org:" + sitePort + "/";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setRequestHeader(new Header("cookie", cfg.getCookieName() + "=" + token));
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");
        Assert.assertEquals(response, "policy-service-url: http://local.ldschurch.org:" + cfg.getConsolePort() + "/oes/v{version}/rest/local.ldschurch.org/");
        method.releaseConnection();
    }

@Test
    public void test_policy_service_url_injected_4_root_path_match() throws HttpException, IOException {
    	Config cfg = Config.getInstance();
        System.out.println("----> test_policy_service_url_injected_4_root_path_match ");
        String token = TestUtilities.authenticateUser("ngiwb2", cfg.getConsolePort(), "local.ldschurch.org");
        String uri = "http://local.ldschurch.org:" + sitePort + "/resources/css/pages/home.css";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setRequestHeader(new Header("cookie", cfg.getCookieName() + "=" + token));
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");
        Assert.assertEquals(response, "policy-service-url: http://local.ldschurch.org:" + cfg.getConsolePort() + "/oes/v{version}/rest/local.ldschurch.org/");
        method.releaseConnection();
    }

}
