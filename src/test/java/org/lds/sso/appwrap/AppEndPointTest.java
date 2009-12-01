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
	public void uriTest() throws URISyntaxException {
		showUri(new URI("app://labs-local.lds.org/some/path"));
		showUri(new URI("http://boydmr:password@labs-local.lds.org:80/some/path?var=val#anchor-a"));
		showUri(new URI("labs-local.lds.org/some/path"));
		showUri(new URI("/some/path"));
		showUri(new URI("labs-local.lds.org/some:/path"));
		showUri(new URI("labs-local.lds.org:this/some:/path"));
	}
	
	private void showUri(URI uri) {
		System.out.println();
		System.out.println("URI : " + uri.toString());
		System.out.println("    scheme             : " + uri.getScheme());
		System.out.println("raw-schemeSpecificPart : " + uri.getRawSchemeSpecificPart());
		System.out.println("    schemeSpecificPart : " + uri.getSchemeSpecificPart());
		System.out.println("raw-userInfo           : " + uri.getRawUserInfo());
		System.out.println("    userInfo           : " + uri.getUserInfo());
		System.out.println("raw-authority          : " + uri.getRawAuthority());
		System.out.println("    authority          : " + uri.getAuthority());
		System.out.println("    host               : " + uri.getHost());
		System.out.println("    port               : " + uri.getPort());
		System.out.println("raw-path               : " + uri.getRawPath());
		System.out.println("    path               : " + uri.getPath());
		System.out.println("raw-query              : " + uri.getRawQuery());
		System.out.println("    query              : " + uri.getQuery());
		System.out.println("raw-fragment           : " + uri.getRawFragment());
		System.out.println("    fragment           : " + uri.getFragment());
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
