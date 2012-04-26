package org.lds.sso.appwrap.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.TestUtilities;
import org.lds.sso.appwrap.identity.User;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Cool little unit test that starts a server socket and the simulator and then
 * uses HttpClient to send requests to the simulator which proxies over to the
 * server socket which evaluates whether headers were included or not and
 * sends a response accordingly that is then used in the unit tests to verify
 * that the simulator correctly passes empty headers or strips them out
 * depending on configuration.
 *
 * @author BOYDMR
 *
 */
public class StripEmptyHeadersTest {

	private Service service = null;
    private Thread server = null;
    private int sitePort = -1;
    
    private static final String HDR_NAME="policy-something";

	@Test
	public void test_emptyHeadersPassed_then_emptyHeadersStripped() throws Exception {
	    // first clear any config residue
	    new Config();

        final ServerSocket sss = new ServerSocket();
        sss.setReuseAddress(true);
        sss.bind(null);
        int serverPort = sss.getLocalPort();

        // now start server to spool back a redirect
        server = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Socket sock = sss.accept();
                        InputStream is = sock.getInputStream();
                        byte[] bytes = new byte[1024];
                        is.read(bytes);
                        int endOfReq = -1;
                        for(int i =0; i<1024-8; i++) {
                            if (bytes[i] == 13 && bytes[i+1] == 10 && bytes[i+2] == 13 && bytes[i+3] == 10) {
                                endOfReq = i;
                            }
                        }
                        if (endOfReq == -1) {
                            throw new RuntimeException("Didn't find end of request in " + new String(bytes));
                        }

                        String input = new String(bytes,0,endOfReq);
                        OutputStream out = sock.getOutputStream();
                        String req = null;
                        String output = null;
                        String answer = null;

                        if (input.contains("/verify-empty-hdr-injected/")) {
                            answer = HDR_NAME + ": not found";
                            int idx = input.indexOf(HDR_NAME + ":");

                            if (idx != -1) {
                                int col = input.indexOf(":", idx+1);
                                int cr = input.indexOf(RequestHandler.CRLF, col+1);
                                String val = input.substring(col+1, cr).trim();
                                answer = HDR_NAME + ": found";
                            }
                            req = "verify-empty-hdr-injected";
                        }
                        else if (input.contains("/verify-stripped/")) {
                            answer = HDR_NAME + ": cleared";

                            if (input.contains(HDR_NAME + ":")) {
                                answer = HDR_NAME + ": seen";
                            }
                            req = "stripped-test";
                        }
                        else if (input.contains("/verify-not-stripped/")) {
                            answer = HttpPackage.SHIM_STRIPPED_HEADERS
                                + ": not seen";

                            if (input.contains(HttpPackage.SHIM_STRIPPED_HEADERS + ":")) {
                                answer = HttpPackage.SHIM_STRIPPED_HEADERS
                                + ": seen";
                            }
                            req = "not-stripped-test";
                        }
                        else {
                            req = "UNEXPECTED";
                            output =
                                "HTTP/1.1 500 Internal Server Error" + RequestHandler.CRLF;
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

        // now set up the shim to verify empty headers are injected
    	System.getProperties().remove("non-existent-sys-prop");
        service = Service.getService("string:"
            + "<?xml version='1.0' encoding='UTF-8'?>"
        	+ "<?system-alias usr-src-props=non-existent-sys-prop default="
            + "\"xml="
            + " <users>"
            + "  <user name='ngiwb1' pwd='password1'>"
            + "   <Att name='non-existent-att' value=''/>"
            + "  </user>"
            + " </users>"
        	+ "\"?>"
            + "<config console-port='auto' proxy-port='auto'>"
            + " <console-recording sso='true' rest='true' max-entries='100' enable-debug-logging='true' />"
            + " <sso-cookie name='lds-policy' domain='.lds.org' />"
            + " <sso-traffic strip-empty-headers='true'>"
            + "  <by-site scheme='http' host='local.lds.org' port='{{proxy-port}}'>"
            + "   <cctx-mapping cctx='/test/*' thost='127.0.0.1' tport='" + serverPort + "' tpath='/test/*'>"
            + "   </cctx-mapping>"
            + "   <unenforced cpath='/test/*'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + " <user-source type='xml'>{{usr-src-props}}</user-source>"
            + "</config>");
        service.start();
        sitePort = Config.getInstance().getProxyPort();
        System.out.println(); // to leave a gap before test output.


        // ------------ TEST ONE: stip headers loaded correctly

        // verify that strip-empty-headers is 'on'
        Config cfg = Config.getInstance();
        Assert.assertEquals(cfg.getStripEmptyHeaders(), true);

        // now set to false and verify that empty user headers are injected
        cfg.setStripEmptyHeaders(false);

        // get the user cookie
        String token = TestUtilities.authenticateUser("ngiwb1", cfg.getConsolePort(), "local.lds.org");

        // ------------ TEST TWO: empty headers make it through

        // now connect and verify that empty headers get passed through.
        String uri = "http://local.lds.org:" + sitePort + "/test/verify-empty-hdr-injected/";
        HttpClient client = new HttpClient();

        HostConfiguration hcfg = new HostConfiguration();
        hcfg.setProxy("127.0.0.1", sitePort);
        client.setHostConfiguration(hcfg);

        HttpMethod method = new GetMethod(uri);
        
        method.addRequestHeader(new Header("Cookie", cfg.getCookieName() + "=" + token));
        method.addRequestHeader(new Header("policy-something", ""));

        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        String response = method.getResponseBodyAsString().trim();
        System.out.println(response);
        // sanity check that we got there
        Assert.assertEquals(status, 200, "should have returned http 200 OK");
        Assert.assertEquals(response, HDR_NAME + ": found");
        method.releaseConnection();

        // ------------ TEST THREE: empty headers get stripped

        // now set to true and verify that empty user headers are stripped
        cfg.setStripEmptyHeaders(true);

        // now connect and verify we get empty headers injected
        String u2 = "http://local.lds.org:" + sitePort + "/test/verify-stripped/";
        HttpClient c2 = new HttpClient();

        HostConfiguration hcfg2 = new HostConfiguration();
        hcfg2.setProxy("127.0.0.1", sitePort);
        c2.setHostConfiguration(hcfg2);

        HttpMethod m2 = new GetMethod(u2);
        m2.addRequestHeader(new Header("Cookie", cfg.getCookieName() + "=" + token));
        m2.setFollowRedirects(false);
        int s2 = c2.executeMethod(m2);
        String r2 = m2.getResponseBodyAsString().trim();
        System.out.println(r2);
        // sanity check that we got there
        Assert.assertEquals(s2, 200, "should have returned http 200 OK");
        Assert.assertEquals(r2, HDR_NAME + ": cleared");

        // ------------ TEST FOUR: empty headers stripped indicator not seen if none stripped

        String u3 = "http://local.lds.org:" + sitePort + "/test/verify-not-stripped/";
        HttpClient c3 = new HttpClient();

        HostConfiguration hcfg3 = new HostConfiguration();
        hcfg3.setProxy("127.0.0.1", sitePort);
        c3.setHostConfiguration(hcfg3);

        HttpMethod m3 = new GetMethod(u3);
        m3.addRequestHeader(new Header("Cookie", cfg.getCookieName() + "=" + token));

        // add all user header values for ngiwb1 so none will be empty
        User usr = cfg.getUserManager().getUser("ngiwb1");
        usr.addAttributeValues("non-existent-att", new String[] {"not-empty"});
        m3.setFollowRedirects(false);
        int s3 = c3.executeMethod(m3);
        String r3 = m3.getResponseBodyAsString().trim();
        System.out.println(r3);
        // sanity check that we got there
        Assert.assertEquals(s3, 200, "should have returned http 200 OK");
        Assert.assertEquals(r3, HttpPackage.SHIM_STRIPPED_HEADERS + ": not seen");

        service.stop();
        server.interrupt();
	}
}
