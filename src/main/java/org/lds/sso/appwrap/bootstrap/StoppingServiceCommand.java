package org.lds.sso.appwrap.bootstrap;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.exception.ServerFailureException;

public class StoppingServiceCommand extends Command {
	public StoppingServiceCommand(String cfgPath, Integer timeout) {
		super(cfgPath, timeout);
	}
	public StoppingServiceCommand(String cfgPath) {
		super(cfgPath);
	}
	
	@Override
	int getTargetResponseCode() {
		return 404;
	}

	@Override
	void doExecute(Service service, Config cfg) {
		try {
			service.stop();
		} catch ( Exception e ) {
			throw new ServerFailureException(e);
		}
	}

	@Override
	String getCommandName() {
		return "Stop Service";
	}

}
