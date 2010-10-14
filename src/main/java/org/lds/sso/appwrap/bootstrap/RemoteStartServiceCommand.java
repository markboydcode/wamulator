package org.lds.sso.appwrap.bootstrap;

import java.io.IOException;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.exception.ServerFailureException;

public class RemoteStartServiceCommand extends Command {
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
			String[] args = { "java", "-cp", classpath, Service.class.getName(), "start", cfgPath };
			executeJavaCommand(args);
		} catch ( IOException e ) {
			throw new ServerFailureException(e);
		}
	}

	protected void executeJavaCommand(String... args) throws IOException {
		String javaHomeDirectory = System.getProperty("java.home");
		args[0] = javaHomeDirectory + "/bin/" + args[0];
		Process p = Runtime.getRuntime().exec(args);
	}

	@Override
	int getTargetResponseCode() {
		return 200;
	}
}