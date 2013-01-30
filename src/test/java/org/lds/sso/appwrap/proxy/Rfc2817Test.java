package org.lds.sso.appwrap.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

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


/**
 * Test that sets up canned response http server behind simulator for exposing
 * how browsers implement rfc 2817.
 * 
 * @author BoydMR
 *
 */
public class Rfc2817Test {
    protected static final String MSG4_404_TEST = "don't request this resource. it isn't there.";
    private Service service = null;
    private Thread server = null;
    private int sitePort;
    private int serverPort;

    @BeforeClass
    public void setUpSimulator () throws Exception {
        // first clear any config residue
        Config cfg = new Config();

        // get socket of server emulator
        final ServerSocket sss = new ServerSocket();
        sss.setReuseAddress(true);
        sss.bind(null);
        serverPort = sss.getLocalPort();

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
                        String hostHdrLC = HeaderDef.Host.getLcNameWithColon();

                        if (input.contains("/force/upgrade")) {
                            // implement RFC 2817, section 4.2 mandatory response
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            String html = "<html><head>\r\n" 
                                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>\r\n" 
                                + "<title>426 Upgrade Required</title>\r\n" 
                                + "</head>\r\n" 
                                + "<body><h2>HTTP: 426</h2><pre>Upgrade Required</pre>\r\n" 
                                + "<p><i><small>we really mean it.</small></i></p>\r\n" 
                                + "<br/></body></html>";
                            baos.write(html.getBytes());
                            output = "HTTP/1.1 426 Upgrade Required" + RequestHandler.CRLF
                                + "Upgrade: TLS/1.0, HTTP/1.1" + RequestHandler.CRLF
                                + "Connection: Upgrade" + RequestHandler.CRLF
                                + "Content-Type: text/html; charset=iso-8859-1" + RequestHandler.CRLF
                                + "Server: test-harness" + RequestHandler.CRLF
                                + "Content-Length: " + baos.size()
                                + RequestHandler.CRLF + RequestHandler.CRLF
                                + new String(baos.toByteArray());
                        }

                        if (output == null) {
                            if (answer == null) {
                                answer = "Welcome to Simple-Server.";
                            }
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
        URL filePath = Rfc2817Test.class.getClassLoader().getResource("RFC2817TestConfig.xml");
    	service = Service.getService("string:"
            + "<?file-alias policy-src-xml=\"" + filePath.getPath().substring(1).replace("/", "\\") + "\"?>"
        	+ "<?system-alias usr-src-props=non-existent-sys-prop default="
            + "\"xml="
            + " <users>"
            + "  <user name='ngiwb1' pwd='password1'/>"
            + "  <user name='ngiwb2'>"
            + "   <Att name='apps' value='aaa,bbb,ccc'/>"
            + "  </user>"
            + " </users>"
        	+ "\"?>"
            + "<config console-port='auto' proxy-port='auto'>"
            + " <console-recording sso='true' rest='true' max-entries='100' enable-debug-logging='true' />"
            + " <sso-cookie name='lds-policy' domain='localhost' />"
            + " <proxy-timeout inboundMillis='400000' outboundMillis='400000'/>"
            + " <sso-traffic strip-empty-headers='true'>"
            + "  <by-site scheme='http' host='localhost' port='{{proxy-port}}'>"
            + "    <cctx-mapping thost='127.0.0.1' tport='{{console-port}}'>"
            + "      <policy-source>xml={{policy-src-xml}}</policy-source>"
            + "    </cctx-mapping>"
            + "  </by-site>"
            + " </sso-traffic>"
            + " <user-source type='xml'>{{usr-src-props}}</user-source>"
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

    //@Test
    public void test_force_upgrade() throws HttpException, IOException {
        Config cfg = Config.getInstance();
        cfg.setAllowForwardProxying(true);
        System.out.println("----> test_force_upgrade");
        String uri = "http://local.lds.org:" + sitePort + "/force/upgrade";
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
        Assert.assertEquals(status, 426, "should have returned http 426 not found");
        Assert.assertNotNull(resp, "426 response payload should have been passed-through.");
        Assert.assertTrue(resp.contains("426 Upgrade Required"), "page content should have contained: '426 Upgrade Required'");
        method.releaseConnection();
    }
}
