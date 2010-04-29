package org.lds.sso.appwrap.rest.oes.v1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.ServerSocket;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.TestUtilities;
import org.lds.sso.appwrap.TestUtilities.Ports;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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

    private Service service = null;
    private String cookieName = "the-cookie-name";
    private Ports ports = null;
    
    @BeforeClass
    public void setUpSimulator() throws Exception {
        System.out.println("setting up simulator");
        ports = TestUtilities.getAvailableSimulatorPorts();

        StringBuffer config = new StringBuffer("string:")
        .append("<?xml version='1.0' encoding='UTF-8'?>")
        .append("<config console-port='" + ports.console + "' proxy-port='" + ports.proxy + "' ")
        .append(" rest-version='CD-OESv1'>")
        .append(" <console-recording sso='true' rest='true' max-entries='100' enable-debug-logging='false'/>")
        .append(" <sso-cookie name='" + cookieName + "' domain='.lds.org'/>")
        .append(" <sso-traffic>")
        .append("  <by-resource uri='app://some-resource' allow='GET'/>")
        .append(" </sso-traffic>")
        .append(" <sso-entitlements policy-domain='lds.org'>")
        .append("  <allow action='WAVE,SHOVE,PUSH' urn='/some/resource'/>")
        .append("  <allow action='SMILE,GET,DROP' urn='/some/resource'/>")
        .append(" </sso-entitlements>")
        .append(" <users>")
        .append("  <user name='user1' pwd='pwd'/>")
        .append("  <user name='user2' pwd='pwd'/>")
        .append(" </users>")
        .append("</config>");

        service = new Service(config.toString());
        service.start();
}
    
    @AfterClass
    public void cleanUpSimulator() throws Exception {
        System.out.println("tearing down simulator on admin-rest port: " 
                + ports.console + " and http-proxy port: " + ports.proxy);
        service.stop();
        service = null;
        ports = null;
    }
    
	@Test
	public void test_GetCookieName() throws Exception {
		String endpoint = "http://127.0.0.1:" + ports.console + "/rest/oes/1/getCookieName";
		
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        
        Assert.assertEquals(status, 200);
        String content = method.getResponseBodyAsString();
        Assert.assertNotNull(content);
        Assert.assertEquals(content, cookieName);
	}
	
    @Test
    public void test_AreTokensValid_multiple() throws Exception {
        // first initiate two sessions so that we have valid tokens
        String usrToken1 = TestUtilities.authenticateUser("user1", ports.console);
        String usrToken2 =  TestUtilities.authenticateUser("user2", ports.console);

        // next craft request for AreTokensValid
        String endpoint = "http://127.0.0.1:" + ports.console + "/rest/oes/1/areTokensValid";
        PostMethod post = new PostMethod(endpoint);
        post.addParameter("token.cnt", "3");
        post.addParameter("token.1", usrToken1);
        post.addParameter("token.2", "invalid-token");
        post.addParameter("token.3", usrToken2);
        post.setFollowRedirects(false);
        HttpClient client = new HttpClient();
        int status = client.executeMethod(post);
        Assert.assertEquals(status, 200);
        String resp = post.getResponseBodyAsString();
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
                    String[] tokens = line.split("=");
                    if (tokens[0].equals("token.1")) {
                        token_1 = Boolean.parseBoolean(tokens[1]);
                    }
                    if (tokens[0].equals("invalid-token")) {
                        token_2 = Boolean.parseBoolean(tokens[1]);
                    }
                    if (tokens[0].equals("token.3")) {
                        token_3 = Boolean.parseBoolean(tokens[1]);
                    }
                }
            }
        Assert.assertEquals(token_1, true, "user1 token should be valid");
        Assert.assertEquals(token_2, false, "invalid-token token should be invalid");
        Assert.assertEquals(token_3, true, "user2 token should be valid");
    }

    @Test
    public void test_AreTokensValid_single() throws Exception {
        // first initiate session so that we have valid token
        String endpoint = "http://127.0.0.1:" + ports.console + "/auth/ui/authenticate?username=user1";
        
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
        endpoint = "http://127.0.0.1:" + ports.console + "/rest/oes/1/areTokensValid";
        PostMethod post = new PostMethod(endpoint);
        post.addParameter("token.cnt", "1");
        post.addParameter("token.1", usrToken1);
        post.setFollowRedirects(false);
        status = client.executeMethod(post);
        Assert.assertEquals(status, 200);
        String resp = post.getResponseBodyAsString();
        Assert.assertNotNull(resp, "response should not be null");
        StringReader sr = new StringReader(resp);
        BufferedReader br = new BufferedReader(sr);

        boolean token_1 = false;
        String line = br.readLine();
        Assert.assertTrue(line != null, "should be a line in response");        
        tokens = line.split("=");
        Assert.assertEquals(tokens[0], "token.1");
        Assert.assertEquals(tokens[1], "true");
    }

    @Test
    public void test_AreTokensValid_BadTokenCnt() throws Exception {
        // craft request for AreTokensValid
        String endpoint = "http://127.0.0.1:" + ports.console + "/rest/oes/1/areTokensValid";
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(endpoint);
        post.addParameter("token.cnt...", "1");
        post.setFollowRedirects(false);
        int status = client.executeMethod(post);
        Assert.assertEquals(status, 400);
        String resp = post.getResponseBodyAsString();
        Assert.assertTrue(resp.contains("no token.cnt"), "response should contain message 'no token.cnt'");
    }

    @Test
    public void test_AreTokensValid_TokenCntNotInt() throws Exception {
        // craft request for AreTokensValid
        String endpoint = "http://127.0.0.1:" + ports.console + "/rest/oes/1/areTokensValid";
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(endpoint);
        post.addParameter("token.cnt", "sss");
        post.setFollowRedirects(false);
        int status = client.executeMethod(post);
        Assert.assertEquals(status, 400);
        String resp = post.getResponseBodyAsString();
        Assert.assertTrue(resp.contains("not an integer"), "response should contain message 'not an integer'");
    }

    @Test
    public void test_ArePermitted_NoToken() throws Exception {
        // craft request for AreTokensValid
        String endpoint = "http://127.0.0.1:" + ports.console + "/rest/oes/1/arePermitted";
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(endpoint);
        // leave off token param intentionally
        post.setFollowRedirects(false);
        int status = client.executeMethod(post);
        Assert.assertEquals(status, 400);
        String resp = post.getResponseBodyAsString();
        Assert.assertTrue(resp.contains("no token specified"), "response should contain message 'no token specified'");
    }

    @Test
    public void test_ArePermitted_NoResCnt() throws Exception {
        // craft request for AreTokensValid
        String endpoint = "http://127.0.0.1:" + ports.console + "/rest/oes/1/arePermitted";
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(endpoint);
        // leave off token param intentionally
        post.setFollowRedirects(false);
        post.addParameter("token", "invalid-token");
        // leave off res.cnt intentionally
        int status = client.executeMethod(post);
        Assert.assertEquals(status, 400);
        String resp = post.getResponseBodyAsString();
        Assert.assertTrue(resp.contains("no res.cnt specified"), "response should contain message 'no res.cnt specified'");
    }

    @Test
    public void test_ArePermitted_ResCntNotInt() throws Exception {
        // craft request for AreTokensValid
        String endpoint = "http://127.0.0.1:" + ports.console + "/rest/oes/1/arePermitted";
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(endpoint);
        post.addParameter("token", "invalid-token");
        post.addParameter("res.cnt", "sss");
        post.setFollowRedirects(false);
        int status = client.executeMethod(post);
        Assert.assertEquals(status, 400);
        String resp = post.getResponseBodyAsString();
        Assert.assertTrue(resp.contains("not an integer"), "response should contain message 'not and integer'");
    }

    private void injectSampleResourcesForAreValidCall(PostMethod post) {
        post.addParameter("res.cnt", "3");
        post.addParameter("res.1","lds.org/some/resource");
        post.addParameter("act.1","GET");
        post.addParameter("ctx.1.cnt","2");
        post.addParameter("ctx.1.1.key","unit");
        post.addParameter("ctx.1.1.val","222");
        post.addParameter("ctx.1.2.key","color");
        post.addParameter("ctx.1.2.val","blue");
        post.addParameter("res.2","lds.org/some/resource");
        post.addParameter("act.2","POST");
        post.addParameter("res.3","lds.org/another/resource");
        post.addParameter("act.3","DELETE");
    }
    
    @Test
    public void test_ArePermitted_ExpiredToken() throws Exception {
        // craft request for AreTokensValid
        String endpoint = "http://127.0.0.1:" + ports.console + "/rest/oes/1/arePermitted";
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(endpoint);
        post.addParameter("token", "invalid-token");
        injectSampleResourcesForAreValidCall(post);
        post.setFollowRedirects(false);
        int status = client.executeMethod(post);
        Assert.assertEquals(status, 200);
        String resp = post.getResponseBodyAsString();
        StringReader sr = new StringReader(resp);
        BufferedReader br = new BufferedReader(sr);

        boolean res_1 = false;
        boolean res_2 = false;
        boolean res_3 = false;
        
            boolean done = false;
            while(!done) {
                String line = br.readLine();
                
                if (line == null) {
                    done = true;
                    break;
                }
                else {
                    String[] tokens = line.split("=");
                    if (tokens[0].equals("res.1")) {
                        res_1 = Boolean.parseBoolean(tokens[1]);
                    }
                    if (tokens[0].equals("res.2")) {
                        res_2 = Boolean.parseBoolean(tokens[1]);
                    }
                    if (tokens[0].equals("res.3")) {
                        res_3 = Boolean.parseBoolean(tokens[1]);
                    }
                }
            }
        Assert.assertEquals(res_1, false, "res.1 should not be permitted for invalid token");
        Assert.assertEquals(res_2, false, "res.2 should not be permitted for invalid token");
        Assert.assertEquals(res_3, false, "res.3 should not be permitted for invalid token");
    }

    @Test
    public void test_ArePermitted_ValidToken() throws Exception {
        // first initiate session so that we have valid token
        String usrToken =  TestUtilities.authenticateUser("user1", ports.console);
        
        // craft request for AreTokensValid
        String endpoint = "http://127.0.0.1:" + ports.console + "/rest/oes/1/arePermitted";
        PostMethod post = new PostMethod(endpoint);
        post.addParameter("token", usrToken);
        injectSampleResourcesForAreValidCall(post);
        post.setFollowRedirects(false);
        HttpClient client = new HttpClient();
        int status = client.executeMethod(post);
        Assert.assertEquals(status, 200);
        String resp = post.getResponseBodyAsString();
        StringReader sr = new StringReader(resp);
        BufferedReader br = new BufferedReader(sr);

        boolean res_1 = false;
        boolean res_2 = false;
        boolean res_3 = false;
        
        boolean done = false;
        while(!done) {
            String line = br.readLine();
                
            if (line == null) {
                done = true;
                break;
            }
            else {
                String[] tokens = line.split("=");
                if (tokens[0].equals("res.1")) {
                    res_1 = Boolean.parseBoolean(tokens[1]);
                }
                if (tokens[0].equals("res.2")) {
                    res_2 = Boolean.parseBoolean(tokens[1]);
                }
                if (tokens[0].equals("res.3")) {
                    res_3 = Boolean.parseBoolean(tokens[1]);
                }
            }
        }
        Assert.assertEquals(res_1, true, "res.1 should be permitted for GET");
        Assert.assertEquals(res_2, false, "res.2 should not be permitted for POST");
        Assert.assertEquals(res_3, false, "res.3 should not be permitted since not defined");
    }
}
