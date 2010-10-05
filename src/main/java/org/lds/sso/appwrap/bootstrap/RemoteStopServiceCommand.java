package org.lds.sso.appwrap.bootstrap;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;

public class RemoteStopServiceCommand extends Command {
	public RemoteStopServiceCommand(String cfgPath) {
		super(cfgPath);
	}
	public RemoteStopServiceCommand(String cfgPath, Integer timeout) {
		super(cfgPath, timeout);
	}

	@Override
	void doExecute(Service service, Config cfg) {
		try {
			URL url = new URL("http://localhost:" + cfg.getConsolePort() + "/admin/shutdown");
			URLConnection connection = url.openConnection();
			connection.connect();
			int responseCode = ((HttpURLConnection)connection).getResponseCode();
		} catch ( IOException e ) {
			// ignore... it just means we've stopped
		}
	}

	@Override
	int getTargetResponseCode() {
		return 404;
	}
}