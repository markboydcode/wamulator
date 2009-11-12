package org.lds.sso.appwrap;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.testng.annotations.Test;

public class TestTimestamp {

	@Test
	public void test() {
		// need "Mon, 14 Sep 2009 04:56:45 GMT" for http date header
		SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		Date theDate = new Date();
		long theTime = theDate.getTime();
		System.out.println(f.format(theDate));
		TimeZone z = f.getTimeZone();
		System.out.println(z.toString());
		int offMillis = z.getOffset(theTime);
		System.out.println("current offset from GMT: " + offMillis + " = " + ((offMillis /3600000)));
		long gmtTime = theTime - offMillis;
		f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ");
		System.out.println(f.format(new Date(gmtTime)) + "GMT");
		//System.out.println(f.format(new Date()));
	}
}
