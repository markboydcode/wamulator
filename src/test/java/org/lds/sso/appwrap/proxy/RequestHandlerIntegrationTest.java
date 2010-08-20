package org.lds.sso.appwrap.proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
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
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RequestHandlerIntegrationTest {
    private Service service = null;
    private Thread server = null;
    private int sitePort;
    private int serverPort;
    private int freePort;
    
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
            System.out.println("Exception releasing server port " + e);
        }

        // get socket of server emulator
        final ServerSocket sss = new ServerSocket();
        sss.setReuseAddress(true);
        sss.bind(null);
        serverPort = sss.getLocalPort();

        // now start server to spool back a redirect
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
                        String hostHdrLC = HttpPackage.HOST_HDR.toLowerCase();
                        
                        if (input.contains("/preserve/host/test/")) {
                            answer = HttpPackage.HOST_HDR + " ???";
                            int idx = inputLC.indexOf(HttpPackage.HOST_HDR);
                            
                            if (idx != -1) {
                                int cr = input.indexOf(RequestHandler.CRLF, idx);
                                String val = null;
                                if (cr == -1) {
                                    // last header
                                    val = input.substring(idx+HttpPackage.HOST_HDR.length()).trim();
                                }
                                else {
                                    val = input.substring(idx+HttpPackage.HOST_HDR.length(), cr).trim();
                                }
                                answer = HttpPackage.HOST_HDR + " " + val;
                            }
                            req = "preserve-host-test";
                        }
                        else if (input.contains("/no-preserve/host/test/")) {
                            answer = HttpPackage.HOST_HDR + " ???";
                            int idx = inputLC.indexOf(HttpPackage.HOST_HDR);
                            
                            if (idx != -1) {
                                int cr = input.indexOf(RequestHandler.CRLF, idx);
                                String val = null;
                                if (cr == -1) {
                                    // last header
                                    val = input.substring(idx+HttpPackage.HOST_HDR.length()).trim();
                                }
                                else {
                                    val = input.substring(idx+HttpPackage.HOST_HDR.length(), cr).trim();
                                }
                                answer = HttpPackage.HOST_HDR + " " + val;
                            }
                            req = "no-preserve-host-test";
                        }
                        else if (input.contains("/host-header/test/")) {
                            answer = HttpPackage.HOST_HDR + " ???";
                            int idx = inputLC.indexOf(HttpPackage.HOST_HDR);
                            
                            if (idx != -1) {
                                int cr = input.indexOf(RequestHandler.CRLF, idx);
                                String val = null;
                                if (cr == -1) {
                                    // last header
                                    val = input.substring(idx+HttpPackage.HOST_HDR.length()).trim();
                                }
                                else {
                                    val = input.substring(idx+HttpPackage.HOST_HDR.length(), cr).trim();
                                }
                                answer = HttpPackage.HOST_HDR + " " + val;
                            }
                            req = "host-header-test";
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
                        else if (input.contains("/bad/gateway/empty/msg")) {
                            output = RequestHandler.CRLF + RequestHandler.CRLF + RequestHandler.CRLF;
                            req = "bad-gateway-empty-message";
                        }
                        else { 
                            req = "UNEXPECTED";
                            output = 
                                "HTTP/1.1 500 Internal Server Error due to unexpected request" + RequestHandler.CRLF; 
                        }

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

        // now set up the shim to verify empty headers are injected
        service = new Service("string:"
            + "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='auto' proxy-port='auto'>"
            + " <console-recording sso='true' rest='true' max-entries='100' enable-debug-logging='true' />"
            + " <proxy-timeout inboundMillis='400000' outboundMillis='400000'/>"
            + " <conditions>"
            + "  <condition alias='app-bbb'>"
            + "   <HasLdsApplication value='bbb'/>"
            + "  </condition>"
            + " </conditions>"
            + " <sso-traffic strip-empty-headers='true'>"
            + "  <by-site scheme='http' host='local.lds.org' port='{{proxy-port}}'>"
            + "   <cctx-mapping cctx='/preserve/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/preserve/*' preserve-host='true'/>"
            + "   <unenforced cpath='/preserve/*'/>"
            + "   <cctx-mapping cctx='/no-preserve/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/no-preserve/*' preserve-host='false'/>"
            + "   <unenforced cpath='/no-preserve/*'/>"
            + "   <cctx-mapping cctx='/host-header/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/host-header/*' host-header='host.lds.org:2445'/>"
            + "   <unenforced cpath='/host-header/*'/>"
            + "   <cctx-mapping cctx='/restricted/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/restricted/*' preserve-host='false'/>"
            + "   <allow action='GET' cpath='/restricted/*'/>"
            + "   <cctx-mapping cctx='/conditional/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/conditional/*' preserve-host='false'/>"
            + "   <allow action='GET' cpath='/conditional/*' condition='{{app-bbb}}'/>"
            
            + "   <cctx-mapping cctx='/bad/gateway/test*' thost='127.0.0.1' tport='" + freePort + "' tpath='/bad/gateway/test*'/>"
            + "   <cctx-mapping cctx='/bad/gateway/empty/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/bad/gateway/empty/*'/>"
            + "   <cctx-mapping cctx='/bad/gateway/message*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/bad/gateway/message*'/>"
            + "   <unenforced cpath='/bad/gateway/*'/>"
            + "   <cctx-file cctx='/file/cp/relative/*' file='classpath:*' content-type='text/html'/>"
            + "   <cctx-file cctx='/file/cp/fixed/*' file='classpath:RequestHandlerIntegrationTestFile1.txt' content-type='text/html'/>"
            + "   <cctx-file cctx='/file/local/relative/*' file='*' content-type='text/plain'/>"
            + "   <cctx-file cctx='/file/local/fixed*' file='sample-output.txt' content-type='text/plain'/>"
            + "   <unenforced cpath='/file/*'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + " <users>"
            + "  <user name='ngiwb1' pwd='password1'/>"
            + "  <user name='ngiwb2' pwd='password1'>"
            + "   <ldsApplications value='aaa,bbb,ccc'/>"
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
        Assert.assertEquals(response, HttpPackage.HOST_HDR + " local.lds.org:" + sitePort);
        method.releaseConnection();
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
        Assert.assertEquals(response, HttpPackage.HOST_HDR + " 127.0.0.1:" + serverPort);
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
        Assert.assertEquals(response, HttpPackage.HOST_HDR + " host.lds.org:2445");
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
        String token = TestUtilities.authenticateUser("ngiwb1", cfg.getConsolePort());
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
        String token = TestUtilities.authenticateUser("ngiwb1", cfg.getConsolePort());
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
        String token = TestUtilities.authenticateUser("ngiwb1", cfg.getConsolePort());
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
        Assert.assertTrue(resp.startsWith("HTTP/1.1 404 Bad Request"), "Response should have started with 'HTTP/1.1 404 Bad Request' but was: '" + resp + "'");
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
