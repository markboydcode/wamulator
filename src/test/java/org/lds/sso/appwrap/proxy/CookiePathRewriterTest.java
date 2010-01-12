package org.lds.sso.appwrap.proxy;

import java.io.IOException;
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
import org.lds.sso.appwrap.Service;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CookiePathRewriterTest {
	
	@Test
	public void parseSimple() {
		String cookieHdr = "c=one; path=/leader-form; domain=.lds.org";
		
		Map<String,String> prw = new HashMap<String,String>();
		prw.put("/leader-form", "/mls/cr");
		prw.put("/mls-membership", "/mls/mbr");
		
		CookiePathRewriter cpr = new CookiePathRewriter(cookieHdr, prw);
		
		String newHdr = cpr.getHeader();
		Assert.assertEquals(newHdr, "c=one; path=/mls/cr; domain=.lds.org");
	}

	@Test
	public void parseQuotedString() {
		String cookieHdr = "c=\"path=/leader-form\"; path=/leader-form; domain=.lds.org";
		
		Map<String,String> prw = new HashMap<String,String>();
		prw.put("/leader-form", "/mls/cr");
		prw.put("/mls-membership", "/mls/mbr");
		
		CookiePathRewriter cpr = new CookiePathRewriter(cookieHdr, prw);
		
		String newHdr = cpr.getHeader();
		// should be no change
		Assert.assertEquals(newHdr, "c=\"path=/leader-form\"; path=/mls/cr; domain=.lds.org");
	}
	

	@Test
	public void parseWithMultipleComplexCookiesQuotedString() {
		String cookieHdr = "c=\"path=/leader-form\"; path=/leader-form; domain=.lds.org";
		
		Map<String,String> prw = new HashMap<String,String>();
		prw.put("/leader-form", "/mls/cr");
		prw.put("/mls-membership", "/mls/mbr");
		
		CookiePathRewriter cpr = new CookiePathRewriter(cookieHdr, prw);
		
		String newHdr = cpr.getHeader();
		// should be no change
		Assert.assertEquals(newHdr, "c=\"path=/leader-form\"; path=/mls/cr; domain=.lds.org");
	}
	
	@Test
	public void testViaIntegration() throws Exception {
		// find some free socket ports
		ServerSocket css = new ServerSocket();
		css.setReuseAddress(true);
		css.bind(null);
		int console = css.getLocalPort();
		
		ServerSocket pss = new ServerSocket();
		pss.setReuseAddress(true);
		pss.bind(null);
		int proxy = pss.getLocalPort();
		
		ServerSocket sss = new ServerSocket();
		sss.setReuseAddress(true);
		sss.bind(null);
		final int server = sss.getLocalPort();
		
		css.close();
		pss.close();
		sss.close();
		
		// now set up the shim
		Service service = new Service("string:"
			+ "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='" + console + "' proxy-port='" + proxy + "'>"
			+ " <sso-traffic>"
	        + "  <by-site scheme='http' host='labs-local.lds.org' port='" + proxy + "'>"
            + "   <cctx-mapping cctx='/test/*' thost='127.0.0.1' tport='" + server + "' tpath='/test/*'/>"
            + "   <unenforced cpath='/test/*'/>"
            + "  </by-site>"
            + "  <rewrite-cookie from-path='/leader-forms' to-path='/mls/cr' />"
			+ " </sso-traffic>"
		    + "</config>");
		service.start();
		
		// now start server to spool back a redirect
		Thread serverT = new Thread() {
			@Override
			public void run() {
				ServerSocket sss = null;
				try {
					sss = new ServerSocket();
					sss.setReuseAddress(true);
					sss.bind(new InetSocketAddress("127.0.0.1", server));
					Socket sock = sss.accept();
					OutputStream out = sock.getOutputStream();
					out.write(("HTTP/1.1 304 Not Modified" + RequestHandler.CRLF
							+ "Set-Cookie: lds-policy=ngia-27;Domain=.lds.org;Path=/leader-forms/dir" + RequestHandler.CRLF
							+ RequestHandler.CRLF // end of headers
							+ RequestHandler.CRLF // end of body
							).getBytes());
					out.flush();
				}
				catch (Exception e) {
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
		serverT.start();
		
		// now connect and verify we get redirected correctly
        String uri = "http://labs-local.lds.org:" + proxy + "/test/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", proxy);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        // sanity check that we got there
        Assert.assertEquals(status, 304, "should have returned http 304 not modified");
        Header cookie = method.getResponseHeader("set-cookie");
        
        Assert.assertEquals(cookie.getValue(),"lds-policy=ngia-27;Domain=.lds.org;Path=/mls/cr/dir");
        service.stop();
	}
	
	@Test
	public void testViaIntegrationWQuotedString() throws Exception {
		// find some free socket ports
		ServerSocket css = new ServerSocket();
		css.setReuseAddress(true);
		css.bind(null);
		int console = css.getLocalPort();
		
		ServerSocket pss = new ServerSocket();
		pss.setReuseAddress(true);
		pss.bind(null);
		int proxy = pss.getLocalPort();
		
		ServerSocket sss = new ServerSocket();
		sss.setReuseAddress(true);
		sss.bind(null);
		final int server = sss.getLocalPort();
		
		css.close();
		pss.close();
		sss.close();
		
		// now set up the shim
		Service service = new Service("string:"
			+ "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='" + console + "' proxy-port='" + proxy + "'>"
			+ " <sso-traffic>"
	        + "  <by-site scheme='http' host='labs-local.lds.org' port='" + proxy + "'>"
            + "   <cctx-mapping cctx='/test/*' thost='127.0.0.1' tport='" + server + "' tpath='/test/*'/>"
            + "   <unenforced cpath='/test/*'/>"
            + "  </by-site>"
            + "  <rewrite-cookie from-path='/leader-forms' to-path='/mls/cr' />"
			+ " </sso-traffic>"
		    + "</config>");
		service.start();
		
		// now start server to spool back a redirect
		Thread serverT = new Thread() {
			@Override
			public void run() {
				ServerSocket sss = null;
				try {
					sss = new ServerSocket();
					sss.setReuseAddress(true);
					sss.bind(new InetSocketAddress("127.0.0.1", server));
					Socket sock = sss.accept();
					OutputStream out = sock.getOutputStream();
					out.write(("HTTP/1.1 304 Not Modified" + RequestHandler.CRLF
							+ "Set-Cookie: lds-policy=\"ngia path=/leader-forms/dir\";Domain=.lds.org;Path=/leader-forms/dir" + RequestHandler.CRLF
							+ RequestHandler.CRLF // end of headers
							+ RequestHandler.CRLF // end of body
							).getBytes());
					out.flush();
				}
				catch (Exception e) {
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
		serverT.start();
		
		// now connect and verify we get redirected correctly
        String uri = "http://labs-local.lds.org:" + proxy + "/test/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", proxy);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        System.out.println(method.getResponseBodyAsString());
        // sanity check that we got there
        Assert.assertEquals(status, 304, "should have returned http 304 not modified");
        Header cookie = method.getResponseHeader("set-cookie");
        
        Assert.assertTrue(cookie.getValue().contains("Path=/mls/cr/dir"), 
		"should have been rewritten from '/leader-forms/dir' to '/mls/cr/dir' ");
        Assert.assertTrue(cookie.getValue().contains("ngia path=/leader-forms/dir"), 
		"should NOT have rewritten quote string portion");
        service.stop();
	}
}
