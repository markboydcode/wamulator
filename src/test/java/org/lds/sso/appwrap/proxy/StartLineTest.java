package org.lds.sso.appwrap.proxy;

import java.net.MalformedURLException;

import org.testng.Assert;
import org.testng.annotations.Test;

public class StartLineTest {

	@Test
	public void testRequestLine() throws MalformedURLException {
		StartLine sl = new StartLine("GET /some/url HTTP/1.1");
		RequestLine rl = sl;
		Assert.assertEquals(rl.getMethod(), "GET");
		Assert.assertEquals(rl.getUri(), "/some/url");
		Assert.assertEquals(rl.getHttpDecl(), "HTTP/1.1");
	}

	@Test
	public void testResponseLine() throws MalformedURLException {
		StartLine sl = new StartLine("HTTP/1.1 200 OK");
		ResponseLine rl = sl;
		Assert.assertEquals(rl.getHttpVer(), "HTTP/1.1");
		Assert.assertEquals(rl.getRespCode(), "200");
		Assert.assertEquals(rl.getMsg(), "OK");
	}
}
