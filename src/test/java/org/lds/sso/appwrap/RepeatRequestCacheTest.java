package org.lds.sso.appwrap;

import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.RequestHandler;
import org.lds.sso.appwrap.proxy.StartLine;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the repeat-request-cache for proper behavior and mark and sweep of
 * old records.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class RepeatRequestCacheTest {
	
	@Test
	public void testRepeatDetected() {
		Config c = new Config() {
			@Override
			protected void startRepeatRequestRecordSweeper() {
				setMaxRepeatCount(3);
				setMinReqOccRepeatMillis(1000);
				setRepeatRecordSweeperSleepPeriod(3000); // <-- this is what we are testing
				super.startRepeatRequestRecordSweeper();
			}
		};
		c.setMaxRepeatCount(3);
		c.setMinReqOccRepeatMillis(1000);
		RequestHandler handler = new RequestHandler(null, c, "id");

		HttpPackage pkg = new HttpPackage();
		StartLine sl = new StartLine("GET", "path", "HTTP1.1");
		pkg.requestLine = sl;
		pkg.host = "host";
		pkg.port = 100;
		pkg.path = sl.getUri();
		
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertTrue(handler.responseThreasholdExceeded(pkg));
		c.stopRepeatRequestRecordSweeper();
	}
	
	@Test
	public void testRepeatNotDetected() throws InterruptedException {
		Config c = new Config() {
			@Override
			protected void startRepeatRequestRecordSweeper() {
				setMaxRepeatCount(3);
				setMinReqOccRepeatMillis(300);
				super.startRepeatRequestRecordSweeper();
			}
		};
		RequestHandler handler = new RequestHandler(null, c, "id");

		HttpPackage pkg = new HttpPackage();
		StartLine sl = new StartLine("GET", "path", "HTTP1.1");
		pkg.requestLine = sl;
		pkg.host = "host";
		pkg.port = 100;
		pkg.path = sl.getUri();

		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Thread.sleep(500); // since exceeds watch-window count should be reset
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		// now threshold should show as exceeded.
		Assert.assertTrue(handler.responseThreasholdExceeded(pkg));

		c.stopRepeatRequestRecordSweeper();
	}

	@Test
	public void validateSweepRemoves() throws InterruptedException {
		Config c = new Config() {
			@Override
			protected void startRepeatRequestRecordSweeper() {
				setMaxRepeatCount(3);
				setMinReqOccRepeatMillis(100);
				setRepeatRecordSweeperSleepPeriod(100); // <-- this is what we are testing
				super.startRepeatRequestRecordSweeper();
			}
		};
		RequestHandler handler = new RequestHandler(null, c, "id");

		HttpPackage pkg = new HttpPackage();
		StartLine sl = new StartLine("GET", "path", "HTTP1.1");
		pkg.requestLine = sl;
		pkg.host = "host";
		pkg.port = 100;
		pkg.path = sl.getUri();

		// get a record in there
		Assert.assertFalse(handler.responseThreasholdExceeded(pkg));
		Thread.sleep(1000); // since exceeds watch-window count should be reset
		Assert.assertTrue(c.requestOccurrence.size() == 0, "occurrence records should have been cleaned out.");
		c.stopRepeatRequestRecordSweeper();
	}
}
