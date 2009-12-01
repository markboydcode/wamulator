package org.lds.sso.appwrap;

import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.RequestHandler;
import org.lds.sso.appwrap.proxy.StartLine;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RepeatRequestCacheTest {
	
	@Test
	public void testRepeatDetected() {
		Config c = new Config();
		c.setMaxRepeatCount(3);
		c.setMinReqOccRepeatMillis(1000);
		RequestHandler handler = new RequestHandler(null, c, "id");

		HttpPackage pkg = new HttpPackage();
		StartLine sl = new StartLine("GET", "path", "HTTP1.1");
		pkg.requestLine = sl;
		pkg.host = "host";
		pkg.port = 100;
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertTrue(handler.responseThreasholdExceeded(pkg));
		c.stopRepeatRequestRecordSweeper();
	}
	@Test
	public void testRepeatNotDetected() throws InterruptedException {
		Config c = new Config();
		c.setMaxRepeatCount(3);
		c.setMinReqOccRepeatMillis(500);
		RequestHandler handler = new RequestHandler(null, c, "id");

		HttpPackage pkg = new HttpPackage();
		StartLine sl = new StartLine("GET", "path", "HTTP1.1");
		pkg.requestLine = sl;
		pkg.host = "host";
		pkg.port = 100;
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Thread.sleep(500);
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertTrue(handler.responseThreasholdExceeded(pkg));
	}
}
