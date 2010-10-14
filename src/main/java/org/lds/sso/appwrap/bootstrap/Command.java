package org.lds.sso.appwrap.bootstrap;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.exception.ServerFailureException;
import org.lds.sso.appwrap.exception.ServerTimeoutFailureException;

public abstract class Command {
	private static final Logger cLog = Logger.getLogger(Service.class);

	protected Integer timeout = 40000;
	protected final String cfgPath;

	public Command(String cfgPath) {
		this(cfgPath, null);
	}
	public Command(String cfgPath, Integer timeout) {
		this.cfgPath = cfgPath;
		if ( timeout != null ) {
			this.timeout = timeout;
		}
	}

	public static final Command parseCommand(String... args) {
		String cfgPath = null;
		Integer timeout = 5000;
		String command = "start";
		if ( args.length > 2 ) {
			cfgPath = args[2];
			timeout = Integer.parseInt(args[1]);
			command = args[0];
		} else if ( args.length > 1 ) {
			cfgPath = args[1];
			command = args[0];
		} else {
			cfgPath = args[0];
		}

		if ( "run".equals(command) ) {
			return new RemoteStartServiceCommand(cfgPath, timeout);
		} else if ( "stop".equals(command) ) {
			return new RemoteStopServiceCommand(cfgPath, timeout);
		} else {
			return new BlockingServiceCommand(cfgPath, timeout);
		}
	}

	public String getCfgPath() {
		return cfgPath;
	}

	public void execute() throws ServerFailureException {
		Service service = Service.getService(cfgPath);
		Config cfg = Config.getInstance();
		doExecute(service, cfg);

		try {
			waitFor(cfg);
		} catch ( IOException e ) {
			throw new ServerFailureException(e);
		}
	}

	void waitFor(Config cfg) throws IOException, ServerTimeoutFailureException {
		long startTime = System.currentTimeMillis();
		int responseCode = getTargetResponseCode();
		do {
			cLog.info("Trying to hit: " + getCheckUrl(cfg.getConsolePort()));
			try {
				URLConnection connection = openConnection(getCheckUrl(cfg.getConsolePort()));
				responseCode = ((HttpURLConnection)connection).getResponseCode();
			} catch ( IOException e ) {
				responseCode = 404;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			cLog.info("Responded with: " + responseCode);
		} while ( responseCode != getTargetResponseCode() && System.currentTimeMillis() - startTime < timeout );

		if ( responseCode != getTargetResponseCode() ) {
			throw new ServerTimeoutFailureException("Server failed to perform the operation in time");
		}
	}

	protected URLConnection openConnection(URL url) throws IOException {
		try {
			URLConnection connection = url.openConnection();
			connection.connect();
			return connection;
		} catch ( IOException inTheTowel ) {
			throw inTheTowel;
		}
	}

	protected URL getCheckUrl(int port) throws IOException {
		try {
			return new URL("http://localhost:" + port + "/is-alive");
		} catch ( IOException inTheTowel ) {
			throw inTheTowel;
		}
	}
	abstract int getTargetResponseCode();
	abstract void doExecute(Service service, Config cfg);
}