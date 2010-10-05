package org.lds.sso.appwrap.bootstrap;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.exception.ServerFailureException;

public class BlockingServiceCommand extends StartingServiceCommand {
	public BlockingServiceCommand(String cfgPath, Integer timeout) {
		super(cfgPath, timeout);
	}
	public BlockingServiceCommand(String cfgPath) {
		super(cfgPath);
	}

	@Override
	void doExecute(Service service, Config cfg) {
		try {
			service.startAndBlock();
		} catch ( Exception e ) {
			throw new ServerFailureException(e);
		}
	}
}