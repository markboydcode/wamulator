package org.lds.sso.appwrap.proxy;

import java.net.MalformedURLException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RequestHandlerTest {
    /**
     * Returns a simulated HttpPackage for an http GET request with values as 
     * needed for test.
     * 
     * @return
     * @throws MalformedURLException
     */
    public HttpPackage getPathOnlyReq() throws MalformedURLException {
        HttpPackage pkg = new HttpPackage();
        pkg.host = "localhost";
        pkg.port = 10;
        pkg.scheme = "http";
        pkg.path = "/some/resource";
        StartLine sl = new StartLine("GET /some/resource HTTP1.1");
        pkg.requestLine = sl;
        return pkg;
    }
    
    /**
     * Returns a simulated HttpPackage for an http GET request with values as 
     * needed for test including a query string.
     * @param string 
     * 
     * @return
     * @throws MalformedURLException
     */
    public HttpPackage getPathAndQueryReq(String query) throws MalformedURLException {
        HttpPackage pkg = new HttpPackage();
        pkg.host = "localhost";
        pkg.port = 10;
        pkg.scheme = "http";
        pkg.path = "/some/resource";
        if (query != null) {
            pkg.query = "q=" + query;
        }
        else {
            pkg.query = "a=b&c=d";
        }
        StartLine sl = new StartLine("GET /some/resource?" + pkg.query + " HTTP1.1");
        pkg.requestLine = sl;
        return pkg;
    }
    
    @Test
    public void test_ininiteRedirectLoopDetectDirect_PathOnly() throws MalformedURLException {
        Config cfg = new Config(); // clean out impact of other tests
        RequestHandler req = new RequestHandler(null, Config.getInstance(), "test-conn");
        int loops = -1;
        for (int i=0; i<=cfg.getMaxRepeatCount(); i++) {
            boolean exceeded = req.responseThreasholdExceeded(getPathOnlyReq());
            if (exceeded) {
                loops = i;
                break;
            }
        }
        Assert.assertEquals(loops, cfg.getMaxRepeatCount());
    }
    
    
    @Test
    public void test_ininiteRedirectLoopDetectDirect_PathAndQuery() throws MalformedURLException {
        Config cfg = new Config(); // clean out impact of other tests
        RequestHandler req = new RequestHandler(null, Config.getInstance(), "test-conn");
        int loops = -1;
        for (int i=0; i<=cfg.getMaxRepeatCount(); i++) {
            boolean exceeded = req.responseThreasholdExceeded(getPathAndQueryReq(null));
            if (exceeded) {
                loops = i;
                break;
            }
        }
        Assert.assertEquals(loops, cfg.getMaxRepeatCount());
    }
    
    @Test
    public void test_ininiteRedirectLoopDetectDirect_ChangingQueryPrevents() throws MalformedURLException {
        Config cfg = new Config(); // clean out impact of other tests
        RequestHandler req = new RequestHandler(null, Config.getInstance(), "test-conn");

        for (int i=0; i<=cfg.getMaxRepeatCount()+1; i++) {
            boolean exceeded = req.responseThreasholdExceeded(getPathAndQueryReq("" + i));
            if (exceeded) {
                Assert.fail("shouldn't cause a detection.");
            }
        }
    }
    
    
    /**
     * Attempt to tests infinite loop detect through proxy but is susceptible to
     * speed of server so save for reference but don't use. Rather, test via
     * the other method directly.
     * @throws Exception 
     */
    // @ Test
    public void test_inifiniteRedirectLoopDetect() throws Exception {
        RequestHandler rh = null;
        
        String xml = "string:" + 
        		"<config proxy-port='auto' console-port='auto' rest-version='CD-OESv1'>" + 
        		"    <console-recording sso='false' rest='false' max-entries='100' enable-debug-logging='false'/>" + 
        		"    <sso-traffic>" + 
        		"        <by-site host='localhost' port='{{proxy-port}}'>" + 
        		"            <cctx-mapping cctx='/public/*' thost='127.0.0.1' tport='{{console-port}}' tpath='/admin/*'/>" + 
        		"            <unenforced cpath='/public/debug.jsp'/>" + 
        		"        </by-site>" + 
        		"    </sso-traffic>" + 
        		"</config>" + 
        		"";
        Service service = new Service(xml);
        service.start();
        
        Config cfg = Config.getInstance();
        long start = System.currentTimeMillis();
        HttpClient client = new HttpClient();
        HttpMethod get = new GetMethod("http://localhost:" + cfg.getProxyPort()
                + "/public/debug.jsp");
        int code = client.executeMethod(get); //1
        if (code != 200) {
            System.out.println("time to incurr loop detect: " + (System.currentTimeMillis() - start));
            System.out.println(get.getResponseBodyAsString());
        }
        Assert.assertEquals(code, 200);
        code = client.executeMethod(get); //2
        if (code != 200) {
            System.out.println("time to incurr loop detect: " + (System.currentTimeMillis() - start));
            System.out.println(get.getResponseBodyAsString());
        }
        Assert.assertEquals(code, 200);
        code = client.executeMethod(get); //3
        if (code != 200) {
            System.out.println("time to incurr loop detect: " + (System.currentTimeMillis() - start));
            System.out.println(get.getResponseBodyAsString());
        }
        Assert.assertEquals(code, 200);
        code = client.executeMethod(get); //4
        if (code != 200) {
            System.out.println("time to incurr loop detect: " + (System.currentTimeMillis() - start));
            System.out.println(get.getResponseBodyAsString());
        }
        Assert.assertEquals(code, 200);
        code = client.executeMethod(get); //5
        if (code != 200) {
            System.out.println("time to incurr loop detect: " + (System.currentTimeMillis() - start));
            System.out.println(get.getResponseBodyAsString());
        }
        Assert.assertEquals(code, 200);
        code = client.executeMethod(get); //6
        if (code != 200) {
            System.out.println("time to incurr loop detect: " + (System.currentTimeMillis() - start));
            System.out.println(get.getResponseBodyAsString());
        }
        Assert.assertEquals(code, 200); 
        code = client.executeMethod(get); //7
        if (code != 200) {
            System.out.println("time to incurr loop detect: " + (System.currentTimeMillis() - start));
            System.out.println(get.getResponseBodyAsString());
        }
        Assert.assertEquals(code, 200);
        code = client.executeMethod(get); //8
        if (code != 200) {
            System.out.println("time to incurr loop detect: " + (System.currentTimeMillis() - start));
            System.out.println(get.getResponseBodyAsString());
        }
        Assert.assertEquals(code, 200);
    }
}
