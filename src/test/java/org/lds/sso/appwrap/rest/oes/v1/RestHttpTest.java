package org.lds.sso.appwrap.rest.oes.v1;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.ServerSocket;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.lds.sso.appwrap.Service;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the Oracle Entitlements Server version 1 rest interface written by the
 * church allowing all technologies to use REST to evaluate authoriation
 * questions.
 * 
 * @author boydmr
 * 
 */
public class RestHttpTest {

	@Test
	public void test_GetCookieName() throws Exception {
		// get available sockets for simulator
		ServerSocket css = new ServerSocket();
		css.setReuseAddress(true);
		css.bind(null);
		int console = css.getLocalPort();
		
		ServerSocket pss = new ServerSocket();
		pss.setReuseAddress(true);
		pss.bind(null);
		int proxy = pss.getLocalPort();
		
		css.close();
		pss.close();
		
		String cookieName = "the-cookie-name";
		
		Service service = new Service("string:"
			+ "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<config console-port='" + console + "' proxy-port='" + proxy + "' " 
			+ " rest-version='CD-OESv1'>"
			+ "<sso-cookie name='" + cookieName + "' domain='.lds.org'/>"
		    + "</config>");
		service.start();
		
		String endpoint = "http://127.0.0.1:" + console + "/rest/oes/1/getCookieName";
		
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        
        Assert.assertEquals(status, 200);
        String content = method.getResponseBodyAsString();
        Assert.assertNotNull(content);
        Assert.assertEquals(content, cookieName);
        service.stop();
	}
	
    @Test
    public void test_AreTokensValid_multiple() throws Exception {
        // get available sockets for simulator
        ServerSocket css = new ServerSocket();
        css.setReuseAddress(true);
        css.bind(null);
        int console = css.getLocalPort();
        
        ServerSocket pss = new ServerSocket();
        pss.setReuseAddress(true);
        pss.bind(null);
        int proxy = pss.getLocalPort();
        
        css.close();
        pss.close();
        
        String cookieName = "the-cookie-name";
        
        Service service = new Service("string:"
            + "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='" + console + "' proxy-port='" + proxy + "' " 
            + " rest-version='CD-OESv1'>"
            + " <sso-cookie name='" + cookieName + "' domain='.lds.org'/>"
            + " <users>"
            + "  <user name='user1' pwd='pwd'/>"
            + "  <user name='user2' pwd='pwd'/>"
            + " </users>"
            + "</config>");
        service.start();
        
        // first initiate two sessions so that we have valid tokens
        String endpoint = "http://127.0.0.1:" + console + "/auth/ui/authenticate?username=user1";
        
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        
        Header ck = method.getResponseHeader("set-cookie");
        Assert.assertNotNull(ck, "auth should have succeeded and set-cookie set for user1");
        String[] tokens = ck.getValue().split("=");
        String cookie = tokens[0];
        String cookieParms = tokens[1];
        String[] parms = cookieParms.split(";");
        String usrToken1 = parms[0];
        Assert.assertEquals(cookie, cookieName);
        
        // authenticate user2
        endpoint = "http://127.0.0.1:" + console + "/auth/ui/authenticate?username=user2";
        
        method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        status = client.executeMethod(method);
        
        ck = method.getResponseHeader("set-cookie");
        Assert.assertNotNull(ck, "auth should have succeeded and set-cookie set for user2");
        tokens = ck.getValue().split("=");
        String cps = tokens[1];
        String[] pms = cookieParms.split(";");
        String usrToken2 = pms[0];

        // next craft request for AreTokensValid
        endpoint = "http://127.0.0.1:" + console + "/rest/oes/1/areTokensValid";
        PostMethod post = new PostMethod(endpoint);
        post.addParameter("token.cnt", "3");
        post.addParameter("token.1", usrToken1);
        post.addParameter("token.2", "invalid-token");
        post.addParameter("token.3", usrToken2);
        post.setFollowRedirects(false);
        status = client.executeMethod(post);
        Assert.assertEquals(status, 200);
        String resp = post.getResponseBodyAsString();
        System.out.println(resp);
        Assert.assertNotNull(resp, "response should not be null");
        StringReader sr = new StringReader(resp);
        BufferedReader br = new BufferedReader(sr);

        boolean token_1 = false;
        boolean token_2 = false;
        boolean token_3 = false;
        
            boolean done = false;
            while(!done) {
                String line = br.readLine();
                
                if (line == null) {
                    done = true;
                    break;
                }
                else {
                    tokens = line.split("=");
                    if (tokens[0].equals(usrToken1)) {
                        token_1 = Boolean.parseBoolean(tokens[1]);
                    }
                    if (tokens[0].equals("invalid-token")) {
                        token_2 = Boolean.parseBoolean(tokens[1]);
                    }
                    if (tokens[0].equals(usrToken2)) {
                        token_3 = Boolean.parseBoolean(tokens[1]);
                    }
                }
            }
        Assert.assertEquals(token_1, true, "user1 token should be valid");
        Assert.assertEquals(token_2, false, "invalid-token token should be invalid");
        Assert.assertEquals(token_3, true, "user2 token should be valid");
        
        service.stop();
    }

    @Test
    public void test_AreTokensValid_single() throws Exception {
        // get available sockets for simulator
        ServerSocket css = new ServerSocket();
        css.setReuseAddress(true);
        css.bind(null);
        int console = css.getLocalPort();
        
        ServerSocket pss = new ServerSocket();
        pss.setReuseAddress(true);
        pss.bind(null);
        int proxy = pss.getLocalPort();
        
        css.close();
        pss.close();
        
        Service service = new Service("string:"
            + "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='" + console + "' proxy-port='" + proxy + "' " 
            + " rest-version='CD-OESv1'>"
            + " <sso-cookie name='the-cookie-name' domain='.lds.org'/>"
            + " <users>"
            + "  <user name='user1' pwd='pwd'/>"
            + "  <user name='user2' pwd='pwd'/>"
            + " </users>"
            + "</config>");
        service.start();
        
        // first initiate two sessions so that we have valid tokens
        String endpoint = "http://127.0.0.1:" + console + "/auth/ui/authenticate?username=user1";
        
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        
        Header ck = method.getResponseHeader("set-cookie");
        Assert.assertNotNull(ck, "auth should have succeeded and set-cookie set for user1");
        String[] tokens = ck.getValue().split("=");
        String cookie = tokens[0];
        String cookieParms = tokens[1];
        String[] parms = cookieParms.split(";");
        String usrToken1 = parms[0];
        
        // next craft request for AreTokensValid
        endpoint = "http://127.0.0.1:" + console + "/rest/oes/1/areTokensValid";
        PostMethod post = new PostMethod(endpoint);
        post.addParameter("token.cnt", "1");
        post.addParameter("token.1", usrToken1);
        post.setFollowRedirects(false);
        status = client.executeMethod(post);
        Assert.assertEquals(status, 200);
        String resp = post.getResponseBodyAsString();
        System.out.println(resp);
        Assert.assertNotNull(resp, "response should not be null");
        StringReader sr = new StringReader(resp);
        BufferedReader br = new BufferedReader(sr);

        boolean token_1 = false;
        String line = br.readLine();
        Assert.assertTrue(line != null, "should be a line in response");        
        tokens = line.split("=");
        Assert.assertEquals(tokens[0], usrToken1);
        Assert.assertEquals(tokens[1], "true");
        
        service.stop();
    }

    @Test
    public void test_AreTokensValid_BadTokenCnt() throws Exception {
        // get available sockets for simulator
        ServerSocket css = new ServerSocket();
        css.setReuseAddress(true);
        css.bind(null);
        int console = css.getLocalPort();
        
        ServerSocket pss = new ServerSocket();
        pss.setReuseAddress(true);
        pss.bind(null);
        int proxy = pss.getLocalPort();
        
        css.close();
        pss.close();
        
        Service service = new Service("string:"
            + "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='" + console + "' proxy-port='" + proxy + "' " 
            + " rest-version='CD-OESv1'>"
            + " <sso-cookie name='the-cookie-name' domain='.lds.org'/>"
            + " <users>"
            + "  <user name='user1' pwd='pwd'/>"
            + "  <user name='user2' pwd='pwd'/>"
            + " </users>"
            + "</config>");
        service.start();
        
        // craft request for AreTokensValid
        String endpoint = "http://127.0.0.1:" + console + "/rest/oes/1/areTokensValid";
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(endpoint);
        post.addParameter("token.cnt...", "1");
        post.setFollowRedirects(false);
        int status = client.executeMethod(post);
        Assert.assertEquals(status, 400);
        String resp = post.getResponseBodyAsString();
        Assert.assertTrue(resp.contains("no token.cnt"), "response should contain message 'no token.cnt'");
        
        service.stop();
    }

    @Test
    public void test_AreTokensValid_TokenCntNotInt() throws Exception {
        // get available sockets for simulator
        ServerSocket css = new ServerSocket();
        css.setReuseAddress(true);
        css.bind(null);
        int console = css.getLocalPort();
        
        ServerSocket pss = new ServerSocket();
        pss.setReuseAddress(true);
        pss.bind(null);
        int proxy = pss.getLocalPort();
        
        css.close();
        pss.close();
        
        Service service = new Service("string:"
            + "<?xml version='1.0' encoding='UTF-8'?>"
            + "<config console-port='" + console + "' proxy-port='" + proxy + "' " 
            + " rest-version='CD-OESv1'>"
            + " <sso-cookie name='the-cookie-name' domain='.lds.org'/>"
            + " <users>"
            + "  <user name='user1' pwd='pwd'/>"
            + "  <user name='user2' pwd='pwd'/>"
            + " </users>"
            + "</config>");
        service.start();
        
        // craft request for AreTokensValid
        String endpoint = "http://127.0.0.1:" + console + "/rest/oes/1/areTokensValid";
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(endpoint);
        post.addParameter("token.cnt", "sss");
        post.setFollowRedirects(false);
        int status = client.executeMethod(post);
        Assert.assertEquals(status, 400);
        String resp = post.getResponseBodyAsString();
        Assert.assertTrue(resp.contains("not an integer"), "response should contain message 'not an integer'");
        
        service.stop();
    }

}
