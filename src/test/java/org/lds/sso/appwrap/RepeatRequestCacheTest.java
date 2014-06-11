package org.lds.sso.appwrap;

import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.RequestHandler;
import org.lds.sso.appwrap.proxy.StartLine;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;

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
	public void validateSweepRemoves() throws InterruptedException, MalformedURLException {
		Config c = new Config() {
			@Override
			protected void startRepeatRequestRecordSweeper() {
				setMaxRepeatCount(3);
				setMinReqOccRepeatMillis(100);
				setRepeatRecordSweeperSleepPeriod(100); // <-- this is what we are testing
				super.startRepeatRequestRecordSweeper();
			}
		};
		RequestHandler handler = new RequestHandler(null, c, "id", false);

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
