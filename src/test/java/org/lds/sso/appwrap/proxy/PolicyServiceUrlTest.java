package org.lds.sso.appwrap.proxy;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.lds.sso.appwrap.rest.RestVersion;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class PolicyServiceUrlTest {
	
	private Service service = null;
    private Thread server = null;
    private int sitePort = -1;
    
	@BeforeClass
	public void setupSim() throws Exception {
	    
        final ServerSocket sss = new ServerSocket();
        sss.setReuseAddress(true);
        sss.bind(null);
        int serverPort = sss.getLocalPort();

        final int[] ports = new int[] {};
        // now start server to spool back a redirect
        server = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Socket sock = sss.accept();
                        InputStream is = sock.getInputStream();
                        byte[] bytes = new byte[1024];
                        int bytesRead = is.read(bytes);
                        /*
                        int endOfReq = -1;
                        for(int i =0; i<bytesRead-8; i++) {
                            // looks for dual cr lf empty line indicator 
                            if (bytes[i] == 13 && bytes[i+1] == 10 && bytes[i+2] == 13 && bytes[i+3] == 10) {
                                endOfReq = i+1; // includes last line-terminating cr lf
                            }
                        }
                        if (endOfReq == -1) {
                            throw new RuntimeException("Didn't find end of request in " + new String(bytes));
                        }
                        */
                        String input = new String(bytes,0,bytesRead);
                        OutputStream out = sock.getOutputStream();
                        String req = null;
                        String output = null;
                        if (input.contains("/testInt/")) {
                            req = "INT";
                            output =
                                "HTTP/1.1 304 Not Modified" + RequestHandler.CRLF
                                + "Set-Cookie: lds-policy=ngia-27;Domain=.lds.org;Path=/leader-forms/dir" + RequestHandler.CRLF; 
                        }
                        else if (input.contains("/testQS/")) {
                            req = "QS";
                            output =
                                "HTTP/1.1 304 Not Modified" + RequestHandler.CRLF
                                + "Set-Cookie: lds-policy=\"ngia path=/leader-forms/dir\";Domain=.lds.org;Path=/leader-forms/dir" + RequestHandler.CRLF; 
                        }
                        else if (input.contains("/testMC/")) {
                            req = "MC";
                            output =
                                "HTTP/1.1 304 Not Modified" + RequestHandler.CRLF
                                + "Set-Cookie: JSESSIONID=D34E5E47A0227DBCFDE5E66884E4C445; Path=/leader-forms,lds-preferred-lang=eng; Expires=Wed, 02-Jun-2010 16:43:27 GMT Path=/" + RequestHandler.CRLF; 
                        }
                        else if (input.contains("/testMH/")) {
                            req = "MH";
                            output =
                                "HTTP/1.1 304 Not Modified" + RequestHandler.CRLF
                                + "Set-Cookie: JSESSIONID=D34E5E47A0227DBCFDE5E66884E4C445; Path=/leader-forms" + RequestHandler.CRLF
                                + "Set-Cookie: lds-preferred-lang=eng; Expires=Wed, 02-Jun-2010 16:43:27 GMT Path=/" + RequestHandler.CRLF; 
                        }
                        else if (input.contains("/testRR/")) {
                            req = "RR";
                            output = 
                                "HTTP/1.1 302 Not Modified"
                                + RequestHandler.CRLF
                                + "Location: http://labs-local.lds.org/test-of-redirect/some/resource.html" + RequestHandler.CRLF; 
                        }
                        else if (input.contains("/testNRR/")) {
                            req = "NRR";
                            output = 
                                "HTTP/1.1 302 Not Modified"
                                + RequestHandler.CRLF
                                + "Location: http://labs-local.lds.org/not-rewritten-path/some/resource.html" + RequestHandler.CRLF; 
                        }
                        else if (input.contains("/service-url/") ||  
                        		input.contains("/service-url2/") ||  
                        		input.contains("/gateway-url/")) {
                            req = (input.contains("/service-url/") ? "URL" 
                                    : (input.contains("/service-url2/") ? "URL2"
                                            : "GTURL"));
                            String sUrl = null;
                            int sIdx = input.indexOf(UserHeaderNames.SERVICE_URL);
                            if (sIdx == -1) {
                                sUrl = UserHeaderNames.SERVICE_URL + " not found in request headers";
                            }
                            else {
                                int cIdx = input.indexOf(":", sIdx+1);
                                int eIdx = input.indexOf(RequestHandler.CRLF, cIdx+1);
                                sUrl = input.substring(cIdx + 1, eIdx).trim();
                            }
                            output = 
                                "HTTP/1.1 302 Not Modified"
                                + RequestHandler.CRLF
                                + "X-SURL-RECEIVED: " + sUrl + RequestHandler.CRLF; 
                        }
                        else {
                            req = "UNEXPECTED";
                            output = 
                                "HTTP/1.1 500 Internal Server Error" + RequestHandler.CRLF; 
                        }
                        
                        System.out.println();
                        System.out.println(req + " request detected: ");
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

        // now set up the shim
        service = new Service("string:"
            + "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='auto' proxy-port='auto' rest-version='" + RestVersion.CD_OESv1.getVersionId() + "'>"
            + " <console-recording sso='true' rest='true' max-entries='100' enable-debug-logging='true' />"
            + " <sso-traffic>"
            + "  <by-site scheme='http' host='labs-local.lds.org' port='{{proxy-port}}'>"
            + "   <cctx-mapping cctx='/testInt/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/testInt/*'/>"
            + "   <unenforced cpath='/testInt/*'/>"
            + "   <cctx-mapping cctx='/testQS/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/testQS/*'/>"
            + "   <unenforced cpath='/testQS/*'/>"
            + "   <cctx-mapping cctx='/testMC/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/testMC/*'/>"
            + "   <unenforced cpath='/testMC/*'/>"
            + "   <cctx-mapping cctx='/testMH/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/testMH/*'/>"
            + "   <unenforced cpath='/testMH/*'/>"
            + "   <cctx-mapping cctx='/testRR/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/testRR/*'/>"
            + "   <unenforced cpath='/testRR/*'/>"
            + "   <cctx-mapping cctx='/testNRR/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/testNRR/*'/>"
            + "   <unenforced cpath='/testNRR/*'/>"
            + "   <cctx-mapping cctx='/service-url/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/service-url/*'/>"
            + "   <unenforced cpath='/service-url/*'/>"
            + "   <cctx-mapping cctx='/gateway-url/*' thost='127.0.0.1' tport='" + serverPort 
            + "'   tpath='/gateway-url/*' policy-service-url-gateway='10.10.10.10:1010'/>"
            + "   <unenforced cpath='/gateway-url/*'/>"
            + "  </by-site>"
            + "  <by-site scheme='http' host='other.lds.org' port='{{proxy-port}}'>"
            + "   <cctx-mapping cctx='/service-url2/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/service-url2/*'/>"
            + "   <unenforced cpath='/service-url2/*'/>"
            + "  </by-site>"
            + "  <rewrite-cookie from-path='/leader-forms' to-path='/mls/cr' />"
            + "  <rewrite-redirect " 
            + "    from='http://labs-local.lds.org/test-of-redirect' "
            + "    to='http://labs-local.lds.org/test' />"
            + " </sso-traffic>"
            + "</config>");
        service.start();
        sitePort = Config.getInstance().getProxyPort();
        System.out.println(); // to leave a gap before test output.
	}
	
	@AfterClass
	public void teardownSim() throws Exception {
        service.stop();
        server.interrupt();
	}
    
    @Test
    public void test_service_url_gateway() throws Exception {
        // now connect and verify we get the correct policy-service-url injected
        String uri = "http://labs-local.lds.org:" + sitePort + "/gateway-url/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        System.out.println(method.getResponseBodyAsString());
        // sanity check that we got there
        Assert.assertEquals(status, 302, "should have returned http 302 not modified");
        Header url = method.getResponseHeader("X-SURL-RECEIVED");
        
        System.out.println(UserHeaderNames.SERVICE_URL + " Received: " + url.getValue());
        String expected = "http://10.10.10.10:1010" + RestVersion.CD_OESv1.getRestUrlBase() + "labs-local.lds.org/";
        Assert.assertTrue(url.getValue().equals(expected), 
                "policy-service-url should have been '" + expected + "' but was '" + url.getValue() + "'");
    }
    
    @Test
    public void test_service_url_1() throws Exception {
        // now connect and verify we get the correct policy-service-url injected
        String uri = "http://labs-local.lds.org:" + sitePort + "/service-url/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        System.out.println(method.getResponseBodyAsString());
        // sanity check that we got there
        Assert.assertEquals(status, 302, "should have returned http 302 not modified");
        Header url = method.getResponseHeader("X-SURL-RECEIVED");
        
        System.out.println(UserHeaderNames.SERVICE_URL + " Received: " + url.getValue());
        String expected = "http://labs-local.lds.org:" + Config.getInstance().getConsolePort() + RestVersion.CD_OESv1.getRestUrlBase() + "labs-local.lds.org/";
        Assert.assertTrue(url.getValue().equals(expected), 
                "policy-service-url should have been '" + expected + "' but was '" + url.getValue() + "'");
    }

    @Test
    public void test_service_url_2() throws Exception {
        // now connect and verify we get the correct policy-service-url injected
        String uri = "http://other.lds.org:" + sitePort + "/service-url2/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        System.out.println(method.getResponseBodyAsString());
        // sanity check that we got there
        Assert.assertEquals(status, 302, "should have returned http 302 not modified");
        Header url = method.getResponseHeader("X-SURL-RECEIVED");
        
        System.out.println(UserHeaderNames.SERVICE_URL + " Received: " + url.getValue());
        String expected = "http://other.lds.org:" + Config.getInstance().getConsolePort() + RestVersion.CD_OESv1.getRestUrlBase() + "other.lds.org/";
        Assert.assertTrue(url.getValue().equals(expected), 
        "policy-service-url should have been '" + expected + "' but was '" + url.getValue() + "'");
    }

    @Test
    public void test_cookie_rewrite_WQuotedString() throws Exception {
        // now connect and verify we get redirected correctly
        String uri = "http://labs-local.lds.org:" + sitePort + "/testQS/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        System.out.println(method.getResponseBodyAsString());
        // sanity check that we got there
        Assert.assertEquals(status, 304, "should have returned http 304 not modified");
        Header cookie = method.getResponseHeader("set-cookie");
        
        System.out.println("Cookie Received: " + cookie.getValue());
        Assert.assertTrue(cookie.getValue().contains("Path=/mls/cr/dir"), 
        "should have been rewritten from '/leader-forms/dir' to '/mls/cr/dir' ");
        Assert.assertTrue(cookie.getValue().contains("ngia path=/leader-forms/dir"), 
        "should NOT have rewritten quote string portion");
    }

    @Test
    public void test_cookie_rewrite_simple() throws Exception {
        // now connect and verify we get redirected correctly
        int sitePort = Config.getInstance().getProxyPort();
        String uri = "http://labs-local.lds.org:" + sitePort + "/testInt/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        // sanity check that we got there
        Assert.assertEquals(status, 304, "should have returned http 304 not modified");
        Header cookie = method.getResponseHeader("set-cookie");
        
        System.out.println("Cookie Received: " + cookie.getValue());
        Assert.assertEquals(cookie.getValue(),"lds-policy=ngia-27;Domain=.lds.org;Path=/mls/cr/dir");
    }
    
    @Test
    public void test_cookie_rewrite_WMultipleCookies_SingleHeader() throws Exception {
        // now start server to spool back a redirect
        
        // now connect and verify we get redirected correctly
        String uri = "http://labs-local.lds.org:" + sitePort + "/testMC/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        System.out.println(method.getResponseBodyAsString());
        // sanity check that we got there
        Assert.assertEquals(status, 304, "should have returned http 304 not modified");
        Header cookie = method.getResponseHeader("set-cookie");
        
        System.out.println("Cookie Received: " + cookie.getValue());
        Assert.assertEquals(cookie.getValue(), "JSESSIONID=D34E5E47A0227DBCFDE5E66884E4C445; Path=/mls/cr,lds-preferred-lang=eng; Expires=Wed, 02-Jun-2010 16:43:27 GMT Path=/", 
        "should have been rewritten from '/leader-forms' to '/mls/cr' ");
    }

    @Test
    public void test_cookie_rewrite_WMultipleCookies_MultipleHeaders() throws Exception {
        // now start server to spool back a redirect
        
        // now connect and verify we get redirected correctly
        String uri = "http://labs-local.lds.org:" + sitePort + "/testMH/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        System.out.println(method.getResponseBodyAsString());
        // sanity check that we got there
        Assert.assertEquals(status, 304, "should have returned http 304 not modified");
        Header[] cookies = method.getResponseHeaders("set-cookie");
        Assert.assertEquals(cookies.length, 2, "should get two cookies back");
        for (Header c : cookies) {
            System.out.println("Cookie Received: " + c.getValue());
            if (c.getValue().startsWith("JSESSIONID=")) {
                Assert.assertEquals(c.getValue(), "JSESSIONID=D34E5E47A0227DBCFDE5E66884E4C445; Path=/mls/cr");
            }
            else if (c.getValue().startsWith("lds-preferred-lang=")) {
                Assert.assertEquals(c.getValue(), "lds-preferred-lang=eng; Expires=Wed, 02-Jun-2010 16:43:27 GMT Path=/");
            }
        }
    }

    @Test
    public void testDoesRewriteRedirect() throws Exception {
        
        // now lets connect and verify we get redirected correctly
        String uri = "http://labs-local.lds.org:" + sitePort + "/testRR/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        System.out.println(method.getResponseBodyAsString());
        Assert.assertEquals(status, 302, "should have returned http 302 for redirect");
        Header loc = method.getResponseHeader("Location");
        System.out.println("Location Received: " + loc.getValue());
        Assert.assertEquals(loc.getValue(), "http://labs-local.lds.org/test/some/resource.html", "should have been rewritten from '.../test-of-redirect...' to '.../test...' ");
    }
    
    @Test
    public void testDoesNotRewriteRedirect() throws Exception {
        
        // now lets connect and verify we get redirected correctly
        String uri = "http://labs-local.lds.org:" + sitePort + "/testNRR/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        System.out.println(method.getResponseBodyAsString());
        Assert.assertEquals(status, 302, "should have returned http 302 for redirect");
        Header loc = method.getResponseHeader("Location");
        System.out.println("Location Received: " + loc.getValue());
        Assert.assertEquals(loc.getValue(), "http://labs-local.lds.org/not-rewritten-path/some/resource.html", "should NOT have been rewritten from '.../not-rewritten-path...'");
    }
}
