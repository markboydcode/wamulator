package org.lds.sso.appwrap.bootstrap;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
			URLConnection connection = openConnection(getShutdownURL(cfg.getConsolePort()));
			((HttpURLConnection)connection).getResponseCode();
		} catch ( IOException e ) {
			// ignore... it just means we've stopped
		}
	}

	protected URL getShutdownURL(int port) throws MalformedURLException {
		return new URL("http://localhost:" + port + "/admin/shutdown");
	}

	@Override
	int getTargetResponseCode() {
		return 404;
	}

	@Override
	String getCommandName() {
		return "Remote Stop Service";
	}
}