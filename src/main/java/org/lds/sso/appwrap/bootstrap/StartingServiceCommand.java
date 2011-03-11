package org.lds.sso.appwrap.bootstrap;

import java.io.IOException;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.exception.ServerFailureException;

public class StartingServiceCommand extends Command {
	public StartingServiceCommand(String cfgPath, Integer timeout) {
		super(cfgPath, timeout);
	}
	public StartingServiceCommand(String cfgPath) {
		super(cfgPath);
	}

	@Override
	void doExecute(Service service, Config cfg) {
		try {
			service.start();
		} catch ( Exception e ) {
			throw new ServerFailureException(e);
		}
		try {
			waitForAppToStart(cfg);
		} catch ( IOException e ) {
			throw new ServerFailureException(e);
		}
	}

	@Override
	String getCommandName() {
		return "Start Service";
	}
}