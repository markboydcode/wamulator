package org.lds.sso.appwrap.bootstrap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.exception.ServerFailureException;

public class RemoteStartServiceCommand extends Command {
	private static final Logger cLog = Logger.getLogger(RemoteStartServiceCommand.class);
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
		
		byte[] buffer = new byte[512];
		int read = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream is = process.getInputStream();
		while (is.available() > 0 && (read = is.read(buffer)) != -1) {
			baos.write(buffer, 0, read);
		}
		System.out.println(baos.toString());		
	}
	
	@Override
	String getCommandName() {
		return "Remote Start Service";
	}
	
	protected void onTimeout() {
		process.destroy();
	}
}