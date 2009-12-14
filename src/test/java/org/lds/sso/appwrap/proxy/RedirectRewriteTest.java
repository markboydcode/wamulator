package org.lds.sso.appwrap.proxy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.lds.sso.appwrap.Service;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RedirectRewriteTest {
	
	@Test
	public void testDoesRewrite() throws Exception {
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
		
		Service service = new Service("string:"
			+ "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='" + console + "' proxy-port='" + proxy + "'>"
			+ " <sso-traffic>"
	        + "  <by-site scheme='http' host='labs-local.lds.org' port='" + proxy + "'>"
            + "   <cctx-mapping cctx='/test/*' thost='127.0.0.1' tport='" + server + "' tpath='/test/*'/>"
            + "   <unenforced cpath='/test/*'/>"
            + "  </by-site>"
            + "  <rewrite-redirect " 
            + "    from='http://labs-local.lds.org/test-of-redirect' "
            + "    to='http://labs-local.lds.org/test' />"
			+ " </sso-traffic>"
		    + "</config>");
		service.start();
		
		// now start listener to spool back redirect
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
					out.write(("HTTP/1.1 302 Found" + RequestHandler.CRLF
							+ "Location: http://labs-local.lds.org/test-of-redirect/some/resource.html" + RequestHandler.CRLF
							+ RequestHandler.CRLF // end of headers
							+ RequestHandler.CRLF // end of body
							).getBytes());
				}
				catch (IOException e) {
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
		
		// now lets connect and verify we get redirected correctly
        String uri = "http://labs-local.lds.org:" + proxy + "/test/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", proxy);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        System.out.println(method.getResponseBodyAsString());
        Assert.assertEquals(status, 302, "should have returned http 302 for redirect");
        Header loc = method.getResponseHeader("Location");
        Assert.assertEquals(loc.getValue(), "http://labs-local.lds.org/test/some/resource.html", "should have been rewritten from '.../test-of-redirect...' to '.../test...' ");
        service.stop();
	}
	
	@Test
	public void testDoesNotRewrite() throws Exception {
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
		
		Service service = new Service("string:"
			+ "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='" + console + "' proxy-port='" + proxy + "'>"
			+ " <sso-traffic>"
	        + "  <by-site scheme='http' host='labs-local.lds.org' port='" + proxy + "'>"
            + "   <cctx-mapping cctx='/test/*' thost='127.0.0.1' tport='" + server + "' tpath='/test/*'/>"
            + "   <unenforced cpath='/test/*'/>"
            + "  </by-site>"
            + "  <rewrite-redirect " 
            + "    from='http://labs-local.lds.org/test-of-redirect' "
            + "    to='http://labs-local.lds.org/test' />"
			+ " </sso-traffic>"
		    + "</config>");
		service.start();
		
		// now start listener to spool back redirect
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
					out.write(("HTTP/1.1 302 Found" + RequestHandler.CRLF
							+ "Location: http://labs-local.lds.org/not-rewritten-path/some/resource.html" + RequestHandler.CRLF
							+ RequestHandler.CRLF // end of headers
							+ RequestHandler.CRLF // end of body
							).getBytes());
				}
				catch (IOException e) {
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
		
		// now lets connect and verify we get redirected correctly
        String uri = "http://labs-local.lds.org:" + proxy + "/test/a/path";
        HttpClient client = new HttpClient();
        HostConfiguration cfg = new HostConfiguration();
        cfg.setProxy("127.0.0.1", proxy);
        client.setHostConfiguration(cfg);
        HttpMethod method = new GetMethod(uri);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        System.out.println(method.getResponseBodyAsString());
        Assert.assertEquals(status, 302, "should have returned http 302 for redirect");
        Header loc = method.getResponseHeader("Location");
        Assert.assertEquals(loc.getValue(), "http://labs-local.lds.org/not-rewritten-path/some/resource.html", "should NOT have been rewritten from '.../not-rewritten-path...'");
        service.stop();
	}
}
