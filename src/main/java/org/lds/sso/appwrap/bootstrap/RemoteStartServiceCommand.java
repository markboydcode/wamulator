package org.lds.sso.appwrap.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.exception.ServerFailureException;
import org.lds.sso.appwrap.io.LogUtils;

public class RemoteStartServiceCommand extends Command {
	private static final Logger cLog = Logger.getLogger(RemoteStartServiceCommand.class.getName());
	public static final String JAVA_OPTS_ENVIRONMENT_VARIABLE = "WAM_OPTS";

	private Process process;

	public RemoteStartServiceCommand(String cfgPath, Integer timeout) {
		super(cfgPath, timeout);
	}
	public RemoteStartServiceCommand(String cfgPath) {
		super(cfgPath);
	}

	@Override
	void doExecute(Service service, Config cfg) {
		String classpath = System.getProperty("java.class.path");
		try {
			List<String> command = new ArrayList<String>();
			String env = System.getenv(JAVA_OPTS_ENVIRONMENT_VARIABLE);
			if ( env != null ) {
				command.add("java");
				command.addAll(Arrays.asList(env.split(" ")));
				command.addAll(Arrays.asList("-cp", classpath, Service.class.getName(), "start", cfgPath));
			}
			executeJavaCommand(command.toArray(new String[command.size()]));
		} catch ( IOException e ) {
			throw new ServerFailureException(e);
		}
	}

	protected void executeJavaCommand(String[] args) throws IOException {
		String javaHomeDirectory = System.getProperty("java.home");
		args[0] = javaHomeDirectory + "/bin/" + args[0];
		ProcessBuilder builder = new ProcessBuilder(args);
		LogUtils.info(cLog, "Launching Wam Emulator using the command {0}", StringUtils.join(args, " "));
		builder.environment().put(JAVA_OPTS_ENVIRONMENT_VARIABLE, System.getenv(JAVA_OPTS_ENVIRONMENT_VARIABLE));
		executeProcess(builder, true);
	}

	@Override
	int getTargetResponseCode() {
		return 200;
	}

	private void executeProcess(ProcessBuilder builder, boolean wait) throws IOException {
		builder.redirectErrorStream(true);
		process = builder.start();

		final InputStream is = process.getInputStream();
		new Thread() {
			@Override
			public void run() {
				BufferedReader reader = null;
				try {
					String result = null;
					while(result != null) {
						reader = new BufferedReader(new InputStreamReader(is));
						result = reader.readLine();
						System.out.println("Process: "+result);
					}
				} catch(IOException e) {
					cLog.log(Level.FINE, "Error reading remote process. Terminating Stream.", e);
					return;
				} finally {
					if(reader != null) {
						try {
							reader.close();
						} catch(Exception e) { }
					}
				}
			}
		}.start();
	}

	@Override
	String getCommandName() {
		return "Remote Start Service";
	}

	protected void onTimeout() {
		process.destroy();
	}
}