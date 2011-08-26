package org.lds.sso.appwrap.proxy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.TestUtilities;
import org.lds.sso.appwrap.proxy.header.HeaderDef;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RequestHandlerIntegrationTest {
    protected static final String MSG4_404_TEST = "don't request this resource. it isn't there.";
    private Service service = null;
    private Thread server = null;
    private int sitePort;
    private int serverPort;
    private int freePort;
    
    /**
     * App entry point if we want to run the back-end server standalone for some
     * testing.
     * 
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        new RequestHandlerIntegrationTest().setUpTestServer();
        while (true) {
            Thread.sleep(3000);
        }
    }
    
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

                        if (input.contains("/wamulat-35/test/")) {
                            int idx = inputLC.indexOf("dude=joe|blah");

                            if (idx == -1) {
                                req = "wamulat_35_pipe_char_test";
                                output =
                                    "HTTP/1.1 500 Pipe char didn't pass through" + RequestHandler.CRLF;
                            }
                            else {
                                answer = "<html><body>Pipe came through just fine.</body></html>";
                            }
                        }
                        else if (input.contains("/preserve/host/test/")) {
                            answer = HeaderDef.Host.getNameWithColon() + " ???";
                            String hstHdrKey = RequestHandler.CRLF + HeaderDef.Host.getLcNameWithColon();
                            int idx = inputLC.indexOf(hstHdrKey);

                            if (idx != -1) {
                                int cr = input.indexOf(RequestHandler.CRLF, idx+1);
                                String val = null;
                                if (cr == -1) {
                                    // last header
                                    val = input.substring(idx+hstHdrKey.length()).trim();
                                }
                                else {
                                    val = input.substring(idx+hstHdrKey.length(), cr).trim();
                                }
                                answer = HeaderDef.Host.getNameWithColon() + " " + val;
                            }
                            req = "preserve-host-test";
                        }
                        else if (input.contains("/no-preserve/host/test/")) {
                            answer = HeaderDef.Host.getNameWithColon() + " ???";
                            String hstHdrKey = RequestHandler.CRLF + HeaderDef.Host.getLcNameWithColon();
                            int idx = inputLC.indexOf(hstHdrKey);

                            if (idx != -1) {
                                int cr = input.indexOf(RequestHandler.CRLF, idx+1);
                                String val = null;
                                if (cr == -1) {
                                    // last header
                                    val = input.substring(idx+hstHdrKey.length()).trim();
                                }
                                else {
                                    val = input.substring(idx+hstHdrKey.length(), cr).trim();
                                }
                                answer = HeaderDef.Host.getNameWithColon() + " " + val;
                            }
                            req = "no-preserve-host-test";
                        }
                        else if (input.contains("/host-header/test/")) {
                            answer = HeaderDef.Host.getNameWithColon() + " ???";
                            String hstHdrKey = RequestHandler.CRLF + HeaderDef.Host.getLcNameWithColon();
                            int idx = inputLC.indexOf(hstHdrKey);

                            if (idx != -1) {
                                int cr = input.indexOf(RequestHandler.CRLF, idx+1);
                                String val = null;
                                if (cr == -1) {
                                    // last header
                                    val = input.substring(idx+hstHdrKey.length()).trim();
                                }
                                else {
                                    val = input.substring(idx+hstHdrKey.length(), cr).trim();
                                }
                                answer = HeaderDef.Host.getNameWithColon() + " " + val;
                            }
                            req = "host-header-test";
                        }
                        else if (input.contains("/wamulat-48/rfc2047/test/")) {
                            answer = "preferredname: ???";
                            String hdrKey = RequestHandler.CRLF + "preferredname:";
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
                                answer = "preferredname: " + val;
                            }
                            req = "wamulat-48-test";
                        }
                        else if (input.contains("/restricted/test/")) {
                            answer = "You made it";
                            req = "restricted-test";
                        }
                        else if (input.contains("/conditional/test/")) {
                            answer = "You made it";
                            req = "conditional-test";
                        }
                        else if (input.contains("/bad/gateway/message")) {
                            // simulates a message that looks like a request.
                            // shouldn't ever happen but testing since processing
                            // of reqs and resps uses the same getHttpPackage method.
                            output = "GET /bad/gateway/message/ HTTP/1.1" + RequestHandler.CRLF + RequestHandler.CRLF;
                            req = "bad-gateway-message";
                        }
                        else if (input.contains("/bad/response/startline")) {
                            // simulates a wacked out message with no space in the response's start line
                            output = "HTTP/1.1404NotFound" + RequestHandler.CRLF + RequestHandler.CRLF;
                            req = "bad-startline-message";
                        }
                        else if (input.contains("/404/test-wcl")) {
                            // test to verify that html content for 404 and 500 level
                            // response codes makes it through to the browser.
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            String html = "<html><head>\r\n" 
                                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>\r\n" 
                                + "<title>Error 404 NOT_FOUND</title>\r\n" 
                                + "</head>\r\n" 
                                + "<body><h2>HTTP ERROR: 404</h2><pre>NOT_FOUND</pre>\r\n" 
                                + "<p>" + MSG4_404_TEST + " </p>\r\n" 
                                + "<p><i><small>we really mean it.</small></i></p>\r\n" 
                                + "<br/></body></html>";
                            baos.write(html.getBytes());
                            output = "HTTP/1.1 404 Not Found" + RequestHandler.CRLF
                                + "Content-Type: text/html; charset=iso-8859-1" + RequestHandler.CRLF
                                + "Server: test-harness" + RequestHandler.CRLF
                                + "Content-Length: " + baos.size()
                                + RequestHandler.CRLF + RequestHandler.CRLF
                                + new String(baos.toByteArray());
                        }
                        else if (input.contains("/404/test-ncl")) {
                            // test to verify that html content for 404 and 500 level
                            // response codes makes it through to the browser
                            // even when no content-length response header is
                            // included.
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            String html = "<html><head>\r\n" 
                                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>\r\n" 
                                + "<title>Error 404 NOT_FOUND</title>\r\n" 
                                + "</head>\r\n" 
                                + "<body><h2>HTTP ERROR: 404</h2><pre>NOT_FOUND</pre>\r\n" 
                                + "<p>" + MSG4_404_TEST + " </p>\r\n" 
                                + "<p><i><small>we really mean it.</small></i></p>\r\n" 
                                + "<br/></body></html>";
                            baos.write(html.getBytes());
                            output = "HTTP/1.1 404 Not Found" + RequestHandler.CRLF
                                + "Content-Type: text/html; charset=iso-8859-1" + RequestHandler.CRLF
                                + "Server: test-harness"
                                + RequestHandler.CRLF + RequestHandler.CRLF
                                + new String(baos.toByteArray());
                        }
                        else if (input.contains("/bad/gateway/empty/msg")) {
                            output = RequestHandler.CRLF + RequestHandler.CRLF + RequestHandler.CRLF;
                            req = "bad-gateway-empty-message";
                        }
                        else if (input.contains("/slow/no/content/length/")) {
                            System.out.println();
                            System.out.println("*** test server detected request: " + req);
                            System.out.println("--- received ---");
                            System.out.println(input);

                            output = "HTTP/1.1 200 Not Found" + RequestHandler.CRLF
                                + "Content-Type: text/html; charset=iso-8859-1" + RequestHandler.CRLF
                                + "Server: test-harness" + RequestHandler.CRLF
                                + RequestHandler.CRLF + RequestHandler.CRLF
                                + "<html><head>\r\n" 
                                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>\r\n" 
                                + "<title>Really slow content</title>\r\n" 
                                + "</head>\r\n";
                            out.write(output.getBytes());
                            out.flush();
                            System.out.println("--- returned ---");
                            System.out.println(output);
                            System.out.println("---> simulating slow connection by waiting 5s...");
                            Thread.sleep(5000);
                            output = "<body><h2>Really slow content</h2>\r\n" 
                                + "<p>Content served with no content-length and a wait time.</p>\r\n" 
                                + "<br/></body></html>";
                            System.out.println(output);
                            out.write(output.getBytes());
                            out.flush();
                            is.close();
                            out.close();
                            alreadyHandled = true;
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
        ServerSocket portFinder = new ServerSocket();
        portFinder.setReuseAddress(true);
        portFinder.bind(null);
        freePort = portFinder.getLocalPort();
        try {
            portFinder.close();
        }
        catch(Exception e) {
            System.out.println("Exception releasing server port for use as bad gateway test " + e);
        }

        // spin up the backend test server fronted by the simulator
        this.setUpTestServer();
        
        // now set up the shim to verify various handling characteristics
        service = Service.getService("string:"
            + "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='auto' proxy-port='auto'>"
            + " <console-recording sso='true' rest='true' max-entries='100' enable-debug-logging='true' />"
            + " <sso-cookie name='lds-policy' domain='.lds.org' />"
            + " <proxy-timeout inboundMillis='400000' outboundMillis='400000'/>"
            + " <conditions>"
            + "  <condition alias='app-bbb'>"
            + "   <HasLdsApplication value='bbb'/>"
            + "  </condition>"
            + " </conditions>"
            + " <sso-traffic strip-empty-headers='true'>"
            + "  <by-site scheme='http' host='local.lds.org' port='{{proxy-port}}'>"
            + "   <cctx-mapping cctx='/wamulat-35/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/wamulat-35/*'/>"
            + "   <unenforced cpath='/wamulat-35/*?*'/>"
            + "   <cctx-mapping cctx='/preserve/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/preserve/*' preserve-host='true'/>"
            + "   <unenforced cpath='/preserve/*'/>"
            + "   <cctx-mapping cctx='/no-preserve/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/no-preserve/*' preserve-host='false'/>"
            + "   <unenforced cpath='/no-preserve/*'/>"
            + "   <cctx-mapping cctx='/host-header/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/host-header/*' host-header='host.lds.org:2445'/>"
            + "   <unenforced cpath='/host-header/*'/>"
            + "   <cctx-mapping cctx='/wamulat-48/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/wamulat-48/*'/>"
            + "   <unenforced cpath='/wamulat-48/*'/>"
            
            + "   <cctx-mapping cctx='/restricted/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/restricted/*' preserve-host='false'/>"
            + "   <allow action='GET' cpath='/restricted/*'/>"
            + "   <cctx-mapping cctx='/conditional/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/conditional/*' preserve-host='false'/>"
            + "   <allow action='GET' cpath='/conditional/*' condition='{{app-bbb}}'/>"

            + "   <cctx-mapping cctx='/bad/gateway/test*' thost='127.0.0.1' tport='" + freePort + "' tpath='/bad/gateway/test*'/>"
            + "   <cctx-mapping cctx='/bad/gateway/empty/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/bad/gateway/empty/*'/>"
            + "   <cctx-mapping cctx='/bad/gateway/message*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/bad/gateway/message*'/>"
            + "   <unenforced cpath='/bad/gateway/*'/>"
            + "   <cctx-mapping cctx='/bad/response/startline*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/bad/startline*'/>"
            + "   <unenforced cpath='/bad/response/startline*'/>"
            + "   <cctx-mapping cctx='/404/test-*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/404/test-*'/>"
            + "   <unenforced cpath='/404/test-*'/>"
            + "   <cctx-file cctx='/file/cp/relative/*' file='classpath:*' content-type='text/html'/>"
            + "   <cctx-file cctx='/file/cp/fixed/*' file='classpath:RequestHandlerIntegrationTestFile1.txt' content-type='text/html'/>"
            + "   <cctx-file cctx='/file/local/relative/*' file='*' content-type='text/plain'/>"
            + "   <cctx-file cctx='/file/local/fixed*' file='sample-output.txt' content-type='text/plain'/>"
            + "   <unenforced cpath='/file/*'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + " <users>"
            + "  <user name='ngiwb1' pwd='password1'/>"
            //+ "  <user name='ngiwb2' pwd='password1'>"
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

    @AfterClass
    public void tearDownSimulator() throws Exception {
        service.stop();
        server.interrupt();
    }

    @Test
    public void test_404_payload_with_content_length_is_passed_through() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        System.out.println("----> test_404_payload_is_passed_through");
        String uri = "http://local.lds.org:" + sitePort + "/404/test-wcl/req";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String resp = method.getResponseBodyAsString();
        if (resp != null) {
            System.out.println(resp);
        }
        Assert.assertEquals(status, 404, "should have returned http 404 not found");
        Assert.assertNotNull(resp, "404 response payload should have been passed-through.");
        Assert.assertTrue(resp.contains(MSG4_404_TEST), "page content should have contained: " + MSG4_404_TEST);
        method.releaseConnection();
    }

    @Test
    public void test_404_payload_without_content_length_is_passed_through() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        System.out.println("----> test_404_payload_is_passed_through");
        String uri = "http://local.lds.org:" + sitePort + "/404/test-ncl/req";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String resp = method.getResponseBodyAsString();
        if (resp != null) {
            System.out.println(resp);
        }
        Assert.assertEquals(status, 404, "should have returned http 404 not found");
        Assert.assertNotNull(resp, "404 response payload should have been passed-through.");
        Assert.assertTrue(resp.contains(MSG4_404_TEST), "page content should have contained: " + MSG4_404_TEST);
        method.releaseConnection();
    }

    @Test
    public void test_local_relative_file_mapping() throws HttpException, IOException {
        System.out.println("----> test_local_relative_file_mapping");
        // first make sure that we have a sample file
        File file = new File("sample-output2.txt");
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos = new FileOutputStream("sample-output2.txt");
        fos.write("---Sample Local File Serving 2---".getBytes());
        fos.flush();
        fos.close();

        // now serve it up
        String uri = "http://local.lds.org:" + sitePort + "/file/local/relative/sample-output2.txt";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);

        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");
        Header type = method.getResponseHeader("content-type");
        Assert.assertEquals(type.getValue(), "text/plain", "type should be text/plain as defined in the mapping");
        Assert.assertTrue(response.contains("Sample Local File Serving 2"), "Content of response should contain 'Sample Local File Serving 2' from auto-generated file sample-output2.txt");
        method.releaseConnection();
    }


    @Test
    public void test_local_fixed_file_mapping() throws HttpException, IOException {
        System.out.println("----> test_local_fixed_file_mapping");
        // first make sure that we have a sample file
        File file = new File("sample-output.txt");
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos = new FileOutputStream("sample-output.txt");
        fos.write("---Sample Local File Serving---".getBytes());
        fos.flush();
        fos.close();

        // now serve it up
        String uri = "http://local.lds.org:" + sitePort + "/file/local/fixed/any-name-gets-same-content.html";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);

        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");
        Header type = method.getResponseHeader("content-type");
        Assert.assertEquals(type.getValue(), "text/plain", "type should be text/plain as defined in the mapping");
        Assert.assertTrue(response.contains("Sample Local File Serving"), "Content of response should contain 'Sample Local File Serving' from auto-generated file sample-output.txt");
        method.releaseConnection();
    }

    @Test
    public void test_fixed_classpath_file_mapping() throws HttpException, IOException {
        System.out.println("----> test_fixed_classpath_file_mapping");
        String uri = "http://local.lds.org:" + sitePort + "/file/cp/fixed/any-name-gets-same-content.html";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);

        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");
        Header type = method.getResponseHeader("content-type");
        Assert.assertEquals(type.getValue(), "text/html", "type should be text/html as defined in the mapping even though the extension of the file indicates just plain text");
        Assert.assertTrue(response.contains("Single File Serving Sample 1"), "Content of response should contain 'Single File Serving Sample 1' from the file RequestHandlerIntegrationTestFile1.txt mapping.");
        method.releaseConnection();
    }

    @Test
    public void test_relative_classpath_file_mapping1() throws HttpException, IOException {
        System.out.println("----> test_relative_classpath_file_mapping1");
        String uri = "http://local.lds.org:" + sitePort + "/file/cp/relative/RequestHandlerIntegrationTestFile1.txt";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);

        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");
        Header type = method.getResponseHeader("content-type");
        Assert.assertEquals(type.getValue(), "text/html", "type should be text/html as defined in the mapping even though the extension of the file indicates just plain text");
        Assert.assertTrue(response.contains("Single File Serving Sample 1"), "Content of response should contain 'Single File Serving Sample 1' from the file RequestHandlerIntegrationTestFile1.txt mapping.");
        method.releaseConnection();
    }

    @Test
    public void test_relative_classpath_file_mapping2() throws HttpException, IOException {
        System.out.println("----> test_relative_classpath_file_mapping2");
        String uri = "http://local.lds.org:" + sitePort + "/file/cp/relative/RequestHandlerIntegrationTestFile2.txt";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);

        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");
        Header type = method.getResponseHeader("content-type");
        Assert.assertEquals(type.getValue(), "text/html", "type should be text/html as defined in the mapping even though the extension of the file indicates just plain text");
        Assert.assertTrue(response.contains("Single File Serving Sample 2"), "Content of response should contain 'Single File Serving Sample 2' from the file RequestHandlerIntegrationTestFile2.txt mapping.");
        method.releaseConnection();
    }

    @Test
    public void test_relative_classpath_file_mapping3() throws HttpException, IOException {
        System.out.println("----> test_relative_classpath_file_mapping3");
        String uri = "http://local.lds.org:" + sitePort + "/file/cp/relative/non-existent-file.txt";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);

        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        Assert.assertEquals(status, 404, "should have returned http 404 OK");
        method.releaseConnection();
    }

    @Test
    public void test_preserve_host() throws HttpException, IOException {
        System.out.println("----> test_preserve_host ");
        String uri = "http://local.lds.org:" + sitePort + "/preserve/host/test/";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);

        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");
        Assert.assertEquals(response, HeaderDef.Host.getNameWithColon() + " local.lds.org:" + sitePort);
        method.releaseConnection();
    }

    /**
     *  @see https://tech.lds.org/jira/browse/WAMULAT-35
     *  
     * @throws HttpException
     * @throws IOException
     */
    @Test
    public void test_wamulat_35_pipe_char_in_url_passes_to_app() throws HttpException, IOException {
        System.out.println("----> test_wamulat_35_pipe_char_in_url_passes_to_app ");

        Socket sock = new Socket("127.0.0.1", sitePort);
        sock.setSoTimeout(400000); // make sure we have a long timeout
        
        // send request
        // url = http://local.lds.org:<sitePort>/wamulat-35/test/?dude=joe|blah;
        OutputStream out = sock.getOutputStream();
        out.write(("GET /wamulat-35/test/?dude=joe|blah Http/1.1" 
                + RequestHandler.CRLF).getBytes());
        out.write(("Host: local.lds.org:" + sitePort 
                + RequestHandler.CRLF + RequestHandler.CRLF).getBytes());
        out.flush();

        // read response
        InputStream in = sock.getInputStream();
        byte[] bytes = new byte[4096];
        int read = in.read(bytes);
        in.close();
        out.close();
        sock.close();
        
        String http = new String(bytes, 0, read);
        Assert.assertTrue(http.startsWith("HTTP/1.1 "), "Should start with 'HTTP/1.1 '");
        int idx = http.indexOf(" ", "HTTP/1.1 ".length());
        Assert.assertTrue(idx != -1, "Should contain response code followed by space");
        String sCode = http.substring("HTTP/1.1 ".length(), idx);
        int code = Integer.parseInt(sCode);
        Assert.assertEquals(code, 200, "should have returned http 200 OK");
    }
    
    /**
     *  @see https://tech.lds.org/jira/browse/WAMULAT-35
     *  
     * @throws HttpException
     * @throws IOException
     */
    @Test
    public void test_wamulat_35_pipe_char_in_full_reqLnUrl_passes_to_app() throws HttpException, IOException {
        System.out.println("----> test_wamulat_35_pipe_char_in_full_reqLnUrl_passes_to_app ");

        Socket sock = new Socket("127.0.0.1", sitePort);
        sock.setSoTimeout(400000); // make sure we have a long timeout
        
        // send request
        // url = http://local.lds.org:<sitePort>/wamulat-35/test/?dude=joe|blah;
        OutputStream out = sock.getOutputStream();
        out.write(("GET http://local.lds.org:" + sitePort + "/wamulat-35/test/?dude=joe|blah Http/1.1" 
                + RequestHandler.CRLF + RequestHandler.CRLF).getBytes());
        out.flush();

        // read response
        InputStream in = sock.getInputStream();
        byte[] bytes = new byte[4096];
        int read = in.read(bytes);
        in.close();
        out.close();
        sock.close();
        
        String http = new String(bytes, 0, read);
        Assert.assertTrue(http.startsWith("HTTP/1.1 "), "Should start with 'HTTP/1.1 '");
        int idx = http.indexOf(" ", "HTTP/1.1 ".length());
        Assert.assertTrue(idx != -1, "Should contain response code followed by space");
        String sCode = http.substring("HTTP/1.1 ".length(), idx);
        int code = Integer.parseInt(sCode);
        Assert.assertEquals(code, 200, "should have returned http 200 OK");
    }
    
    @Test
    public void test_dont_preserve_host() throws HttpException, IOException {
        System.out.println("----> test_dont_preserve_host ");
        String uri = "http://local.lds.org:" + sitePort + "/no-preserve/host/test/";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);

        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");
        Assert.assertEquals(response, HeaderDef.Host.getNameWithColon() + " 127.0.0.1:" + serverPort);
        method.releaseConnection();
    }

    @Test
    public void test_wamulat_48() throws HttpException, IOException {
        System.out.println("----> test_wamulat_48 ");
        
        Config cfg = Config.getInstance();
        String token = TestUtilities.authenticateUser("ngiwb2", cfg.getConsolePort(), "local.lds.org");
        System.out.println(" auth'd ngiwb2");

        String uri = "http://local.lds.org:" + sitePort + "/wamulat-48/rfc2047/test/";
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
        Assert.assertEquals(response, "preferredname: =?UTF-8?Q?Jay_=E9=87=91<script>alert(0)</script>=E8=99=AC_Admin?=");
        method.releaseConnection();
    }

    @Test
    public void test_host_header() throws HttpException, IOException {
        System.out.println("----> test_host_header ");
        String uri = "http://local.lds.org:" + sitePort + "/host-header/test/";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);

        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        Assert.assertEquals(status, 200, "should have returned http 200 OK");
        Assert.assertEquals(response, HeaderDef.Host.getNameWithColon() + " host.lds.org:2445");
        method.releaseConnection();
    }

    @Test
    public void test_req_with_no_cctx_match() throws HttpException, IOException {
        System.out.println("----> test_req_with_no_cctx_match");
        String uri = "http://local.lds.org:" + sitePort + "/unconfigured/path/";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);

        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        Assert.assertEquals(status, 501, "should have returned http 501 Not Implemented");
        method.releaseConnection();
    }

    @Test
    public void test_restricted_with_no_session_redir_2_signin() throws HttpException, IOException {
        System.out.println("----> test_restricted_with_no_session_redir_2_signin");
        String uri = "http://local.lds.org:" + sitePort + "/restricted/path/";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);

        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        Assert.assertEquals(status, 302, "should have returned http 302");
        Header loc = method.getResponseHeader("location");

        Assert.assertEquals(loc.getValue(), Config.getInstance().getLoginPage()
                + "?goto=" + URLEncoder.encode(uri, "utf-8"),
                "redirect location was wrong.");
        method.releaseConnection();
    }

    @Test
    public void test_restricted_with_good_session_proxied() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        String token = TestUtilities.authenticateUser("ngiwb1", cfg.getConsolePort(), "local.lds.org");
        System.out.println("----> test_restricted_with_good_session_proxied");
        String uri = "http://local.lds.org:" + sitePort + "/restricted/test/";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setRequestHeader(new Header("cookie", cfg.getCookieName() + "=" + token));
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        Assert.assertEquals(status, 200, "should have returned http 200");
        String content = method.getResponseBodyAsString();
        Assert.assertEquals(content, "You made it", "wrong content returned.");
        method.releaseConnection();
    }

    @Test
    public void test_restricted_with_good_session_not_cond_403() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        String token = TestUtilities.authenticateUser("ngiwb1", cfg.getConsolePort(), "local.lds.org");
        System.out.println("----> test_restricted_with_good_session_not_cond_403");
        String uri = "http://local.lds.org:" + sitePort + "/conditional/test/";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setRequestHeader(new Header("cookie", cfg.getCookieName() + "=" + token));
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String resp = method.getResponseBodyAsString();
        if (status != 403 && resp != null) {
            System.out.println(resp);
        }
        Assert.assertEquals(status, 403, "should have returned http 403");
        method.releaseConnection();
    }

    @Test
    public void test_forward_proxying_blocked() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        System.out.println("----> test_forward_proxying_blocked");
        String uri = "http://unmapped-host.lds.org:" + sitePort + "/any/path/";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String resp = method.getResponseBodyAsString();
        if (status != 501 && resp != null) {
            System.out.println(resp);
        }
        Assert.assertEquals(status, 501, "should have returned http 501");
        method.releaseConnection();
    }

    @Test
    public void test_bad_response_startline_no_space() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        System.out.println("----> test_bad_response_startline_no_space");
        String uri = "http://local.lds.org:" + sitePort + "/bad/response/startline/test";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String resp = method.getResponseBodyAsString();
        if (status != 502 && resp != null) {
            System.out.println(resp);
        }
        Assert.assertEquals(status, 502, "should have returned http 502 bad gateway");
        method.releaseConnection();
    }

    @Test
    public void test_bad_request_startline_no_space() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        System.out.println("----> test_bad_request_startline_no_space");
        // note: we don't need to include this in the simulator config in
        // setUpSimulator() since this bad request fails during parsing and
        // won't make it to the enforcement point in RequestHandler.java.
        String uri = "http://local.lds.org:" + sitePort + "/bad/request/startline/test";
        Socket sock = new Socket("127.0.0.1", sitePort);
        sock.setSoTimeout(400000); // make sure we have a long timeout
        OutputStream out = sock.getOutputStream();
        out.write(("GET/some/unexistent/pathHttp/1.1" 
                + RequestHandler.CRLF + RequestHandler.CRLF).getBytes());
        out.flush();
        InputStream in = sock.getInputStream();
        byte[] bytes = new byte[4096];
        int read = in.read(bytes);
        out.close();
        in.close();
        sock.close();
        
        String http = new String(bytes, 0, "http/1.1 ".length());
        Assert.assertEquals(http.toLowerCase(), "http/1.1 ");
    }

    @Test
    public void test_bad_gateway_when_connecting() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        System.out.println("----> test_bad_gateway_when_connecting");
        String uri = "http://local.lds.org:" + sitePort + "/bad/gateway/test";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String resp = method.getResponseBodyAsString();
        if (status != 502 && resp != null) {
            System.out.println(resp);
        }
        Assert.assertEquals(status, 502, "should have returned http 502 bad gateway");
        method.releaseConnection();
    }

    @Test
    public void test_bad_gateway_empty_message() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        System.out.println("----> test_bad_gateway_empty_message");
        String uri = "http://local.lds.org:" + sitePort + "/bad/gateway/empty/msg";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String resp = method.getResponseBodyAsString();
        if (status != 502 && resp != null) {
            System.out.println(resp);
        }
        Assert.assertEquals(status, 502, "should have returned http 502 bad gateway");
        method.releaseConnection();
    }

    @Test
    public void test_bad_gateway_message() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        System.out.println("----> test_bad_gateway_message");
        String uri = "http://local.lds.org:" + sitePort + "/bad/gateway/message";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String resp = method.getResponseBodyAsString();
        if (status != 502 && resp != null) {
            System.out.println(resp);
        }
        Assert.assertEquals(status, 502, "should have returned http 502 bad gateway");
        method.releaseConnection();
    }

    @Test
    public void test_restricted_with_expired_session_redir_2_signin() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        String token = TestUtilities.authenticateUser("ngiwb1", cfg.getConsolePort(), "local.lds.org");
        cfg.getSessionManager().terminateAllSessions();
        System.out.println("----> test_restricted_with_expired_session_redir_2_signin");
        String uri = "http://local.lds.org:" + sitePort + "/restricted/path/";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        method.setRequestHeader(new Header("cookie", cfg.getCookieName() + "=" + token));
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        Assert.assertEquals(status, 302, "should have returned http 302");
        Header loc = method.getResponseHeader("location");

        Assert.assertEquals(loc.getValue(), Config.getInstance().getLoginPage()
                + "?goto=" + URLEncoder.encode(uri, "utf-8"),
                "redirect location was wrong.");
        method.releaseConnection();
    }

    @Test
    public void test_empty_bad_request() throws HttpException, IOException {
        System.out.println("----> test_empty_bad_request");
        Socket soc = new Socket("127.0.0.1", sitePort);
        soc.setSoTimeout(2000); // force the input stream to wait 2 seconds for response
        OutputStream out = soc.getOutputStream();
        InputStream in = soc.getInputStream();
        out.write(RequestHandler.CRLF.getBytes());
        out.flush();
        String resp = TestUtilities.readAllHttpHeaders(in);
        //No need to assert.  here as long as an exception isn't thrown we're good. WAMULAT-53
        //Assert.assertTrue(resp.startsWith("HTTP/1.1 404 Bad Request"), "Response should have started with 'HTTP/1.1 404 Bad Request' but was: '" + resp + "'");
    }

    @Test
    public void test_client_request_timeout() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        int old = cfg.getProxyInboundSoTimeout();
        try {
        System.out.println("----> test_client_request_timeout");
        // get SoTimeout for proxy client inputstream and set really low for this test
        System.out.println("setting proxy inbound timeout to 1");
        cfg.setProxyInboundSoTimeout(1); // 1 millisecond
        Socket soc = new Socket("127.0.0.1", sitePort);
        InputStream in = soc.getInputStream();
        String resp = null;
        resp = TestUtilities.readAllHttpHeaders(in);
        Assert.assertTrue(resp.startsWith("HTTP/1.1 408 Request Timeout"), "Response should have started with 'HTTP/1.1 408 Request Timeout' but was: '" + resp + "'");
        } finally {
            // stick old value back in
            System.out.println("restoring proxy inbound timeout to " + old);
            cfg.setProxyInboundSoTimeout(old);
        }
    }
}
