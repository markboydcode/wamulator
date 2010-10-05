package org.lds.sso.appwrap.bootstrap;

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
	}

	@Override
	int getTargetResponseCode() {
		return 200;
	}
}