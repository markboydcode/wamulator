package org.lds.sso.appwrap;

import org.lds.sso.appwrap.bootstrap.RemoteStartServiceCommand;
import org.lds.sso.appwrap.bootstrap.RemoteStopServiceCommand;
import org.testng.annotations.Test;

public class RemoteStartStopTest {
	private static final String CONFIG =
            "string:<?xml version='1.0' encoding='UTF-8'?>"
            + "<?alias site=labs-local.lds.org?>"
            + "<config console-port='8088' proxy-port='8045'>"
            + "  <conditions>"
            + "   <condition alias='o&apos;hare prj'>"
            + "    <OR site='{{site}}'>"
            + "     <HasLdsApplication value='o&apos;hare prj'/>"
            + "<!-- comment gets dropped -->"
            + "     <HasLdsApplication value='o&quot;hare prj'/>"
            + "    </OR>"
            + "   </condition>"
            + "  </conditions>"
            + " <sso-traffic>"
            + "  <by-site scheme='http' host='local.lds.org' port='45'>"
            + "   <cctx-mapping cctx='/conditional/*' thost='127.0.0.1' tport='1000' tpath='/conditional/*'/>"
            + "   <allow action='GET' cpath='/conditional/*' condition='{{o&apos;hare prj}}'/>"
            + "  </by-site>"
            + " </sso-traffic>"
            + "  <users>"
            + "    <user name='nnn' pwd='pwd'>"
            + "    </user>"
            + "  </users>"
            + "</config>";

	@Test
	public void testRemoteStart() {
		RemoteStartServiceCommand start = new RemoteStartServiceCommand(CONFIG);
		RemoteStopServiceCommand stop = new RemoteStopServiceCommand(CONFIG);
		//Service.invoke(stop).invoke(start).invoke(stop);
		// for the moment...
	}
}
