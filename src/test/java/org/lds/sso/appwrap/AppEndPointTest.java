package org.lds.sso.appwrap;

import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.RequestLine;
import org.lds.sso.appwrap.proxy.StartLine;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AppEndPointTest {
    
    @Test
    public void regtest() {
        String pos = "p4/7u111/5u222/1u333/:p23/7u444/5u555/1u666/1016u777/";
        Assert.assertTrue(pos.matches(".*p4/.*")); // has position 4 
        Assert.assertTrue(pos.matches(".*p23/.*")); // has position 23 
        Assert.assertTrue(pos.matches(".*p4/[^:]*u333/.*")); // has position 4 in unit 333 
        Assert.assertTrue(pos.matches(".*p4/[^:]*u333/.*")); // has position 4 in unit 111 
        Assert.assertFalse(pos.matches(".*p4/[^:]*u444/.*")); // has not position 4 in unit 444 
        Assert.assertFalse(pos.matches(".*p4/[^:]*u777/.*")); // has not position 4 in unit 777 

        Assert.assertTrue(pos.matches(".*p23/[^:]*u777/.*")); // has position 23 in unit 333 
        Assert.assertTrue(pos.matches(".*p23/[^:]*u444/.*")); // has position 23 in unit 111 
    }
    
	@Test
	public void testTransformC2AnoQ() {
		AppEndPoint ep = new AppEndPoint("/mls/mbr", "/mls-membership", "", 80);
		ep.setCanonicalContextRoot("/mls/mbr");
		ep.setApplicationContextRoot("/mls-membership");
		
		StartLine canRl = new StartLine("GET", "/mls/mbr/some/path/to/greatness.jsp", "HTTP/1.1");
		HttpPackage pkg = new HttpPackage();
		pkg.requestLine = canRl;
		RequestLine appRl = ep.getAppRequestUri(pkg);
		
		Assert.assertEquals(appRl.getMethod(), "GET");
		Assert.assertEquals(appRl.getHttpDecl(), "HTTP/1.1");
		Assert.assertEquals(appRl.getUri(), "/mls-membership/some/path/to/greatness.jsp");
	}

	@Test
	public void testTransformC2AwQ() {
		AppEndPoint ep = new AppEndPoint("/mls/mbr", "/mls-membership", "", 80);
		ep.setCanonicalContextRoot("/mls/mbr");
		ep.setApplicationContextRoot("/mls-membership");
		
		StartLine canRl = new StartLine("GET", "/mls/mbr/some/path/to/greatness.jsp?key=value", "HTTP/1.1");
		HttpPackage pkg = new HttpPackage();
		pkg.requestLine = canRl;
		RequestLine appRl = ep.getAppRequestUri(pkg);
		
		Assert.assertEquals(appRl.getMethod(), "GET");
		Assert.assertEquals(appRl.getHttpDecl(), "HTTP/1.1");
		Assert.assertEquals(appRl.getUri(), "/mls-membership/some/path/to/greatness.jsp?key=value");
	}
}
