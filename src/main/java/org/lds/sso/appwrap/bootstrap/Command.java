package org.lds.sso.appwrap.bootstrap;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.exception.ServerFailureException;
import org.lds.sso.appwrap.exception.ServerTimeoutFailureException;
import org.lds.sso.appwrap.io.LogUtils;

import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;

/**
 * Abstract class for runtime start/stop actions and dictates the pattern of
 * parameters used at runtime.
 *
 * <cfg-file> [timeout] [command]
 *
 * @author joshcummings
 *
 */
public abstract class Command {
	private static final Logger cLog = Logger.getLogger(Service.class.getName());

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

	/**
	 * Dictates the pattern of command line parameters passed specified when
	 * running the {@link org.lds.sso.appwrap.Service}. At a minimum a
	 * configuration file must be specified. Therefore, it will always be the
	 * last parameter. Optionally, a command can be specified. If not
	 * specified it defaults to "start". If specified the command is the first
	 * parameter and would be followed by the configuartion file. For all
	 * commands an optional milliseconds timeout can be specified. If included
	 * it must follow the command and precede the configuration file. It
	 * represents the milliseconds allowed for the specified command to execute
	 * before a {@link org.lds.sso.appwrap.exception.ServerTimeoutFailureException} is
	 * thrown indicating that the command could not be executed in the allotted
	 * time.
	 *
	 * @param args
	 * @return
	 */
	public static final Command parseCommand(String... args) {
		String cfgPath = null;
		Integer timeout = 40000;
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
            // indicates what version is running before anything else happens to help with troubleshooting
            // if/when problems arise
            LogUtils.info(cLog, "starting simulator version  : {0}", Config.serverName() );
            return new BlockingServiceCommand(cfgPath, timeout);
		}
	}

	public String getCfgPath() {
		return cfgPath;
	}

	public final void execute() throws ServerFailureException {
		LogUtils.info(cLog, "Preparing to run [{0}] command...", getCommandName());
		Service service = Service.getService(cfgPath);
		Config cfg = Config.getInstance();

		LogUtils.info(cLog, "Running [{0}] command...", getCommandName());
		doExecute(service, cfg);

		LogUtils.info(cLog, "Sucessfully ran [{0}] command...", getCommandName());
	}

	protected void waitForPortsToBecomeAvailable(Config cfg) throws IOException, ServerTimeoutFailureException {
		long startTime = System.currentTimeMillis();
		LogUtils.info(cLog, "Waiting for port {0} and {1} to become available.", String.valueOf(cfg.getConsolePort()), String.valueOf(cfg.getProxyPort()));
		while(!available(cfg.getConsolePort()) && System.currentTimeMillis() - startTime < timeout) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			System.out.print("#");
		}
		while(!available(cfg.getProxyPort()) && System.currentTimeMillis() - startTime < timeout) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			System.out.print("#");
		}
		System.out.print("\n");
		if (!available(cfg.getConsolePort()) || !available(cfg.getProxyPort())) {
			throw new ServerTimeoutFailureException("Failed to stop existing Server.  Required ports "+cfg.getConsolePort()+" and "+cfg.getProxyPort()+" Not available.");
		}
	}

	protected void waitForAppToStart(Config cfg) throws IOException, ServerTimeoutFailureException {
		long startTime = System.currentTimeMillis();
		int responseCode = HttpURLConnection.HTTP_NOT_FOUND;
		LogUtils.info(cLog, "Trying to hit: {0}", getCheckUrl(cfg.getConsolePort()));
		do {
			try {
				URLConnection connection = openConnection(getCheckUrl(cfg.getConsolePort()));
				responseCode = ((HttpURLConnection)connection).getResponseCode();
			} catch(IOException e) {
				responseCode = HttpURLConnection.HTTP_NOT_FOUND;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			System.out.print("#");
		} while ( responseCode != HttpURLConnection.HTTP_OK && System.currentTimeMillis() - startTime < timeout );
		System.out.print("\n");
		if ( responseCode != HttpURLConnection.HTTP_OK ) {
			throw new ServerTimeoutFailureException("Failed to start server within timeout.  Url "+getCheckUrl(cfg.getConsolePort())+" is not working.");
		}
	}

	/**
	 * Checks to see if a specific port is available.
	 *
	 * @param port the port to check for availability
	 */
	public static boolean available(int port) {
	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    } finally {
	        if (ds != null) {
	            ds.close();
	        }

	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {
	                /* should not be thrown */
	            }
	        }
	    }

	    return false;
	}

	protected URLConnection openConnection(URL url) throws IOException {
		try {
			URLConnection connection = url.openConnection();
			connection.setReadTimeout(30000);
			connection.setConnectTimeout(30000);
			connection.connect();
			return connection;
		} catch ( IOException inTheTowel ) {
			throw inTheTowel;
		}
	}

	protected URL getCheckUrl(int port) throws IOException {
		try {
			return new URL("http://localhost:" + port + Config.getInstance().getWamulatorServiceUrlBase() + "/is-alive");
		} catch ( IOException inTheTowel ) {
			throw inTheTowel;
		}
	}
	abstract void doExecute(Service service, Config cfg);
	abstract String getCommandName();
}