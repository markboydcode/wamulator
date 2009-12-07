package org.lds.sso.appwrap;

import java.net.URI;
import java.net.URISyntaxException;

import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.RequestLine;
import org.lds.sso.appwrap.proxy.StartLine;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AppEndPointTest {
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
