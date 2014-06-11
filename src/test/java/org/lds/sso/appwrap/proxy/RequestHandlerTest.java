package org.lds.sso.appwrap.proxy;

import org.lds.sso.appwrap.AppEndPoint.Scheme;
import org.lds.sso.appwrap.Config;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

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
        pkg.scheme = Scheme.HTTP;
        pkg.path = "/some/resource";
        StartLine sl = new StartLine("GET /some/resource HTTP1.1");
        pkg.requestLine = sl;
        return pkg;
    }
    
    /**
     * Returns a simulated HttpPackage for an http GET request with values as 
     * needed for test including a query string.
     *
     * @return
     * @throws MalformedURLException
     */
    public HttpPackage getPathAndQueryReq(String query) throws MalformedURLException {
        HttpPackage pkg = new HttpPackage();
        pkg.host = "localhost";
        pkg.port = 10;
        pkg.scheme = Scheme.HTTP;
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
    public void test_ininiteRedirectLoopDetectDirect_ChangingQueryPrevents() throws MalformedURLException {
        Config cfg = new Config(); // clean out impact of other tests
        RequestHandler req = new RequestHandler(null, Config.getInstance(), "test-conn", false);

        for (int i=0; i<=cfg.getMaxRepeatCount()+1; i++) {
            boolean exceeded = req.responseThreasholdExceeded(getPathAndQueryReq("" + i));
            if (exceeded) {
                Assert.fail("shouldn't cause a detection.");
            }
        }
    }
    
}
