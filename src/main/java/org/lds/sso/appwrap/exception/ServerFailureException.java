package org.lds.sso.appwrap.exception;

public class ServerFailureException extends RuntimeException {
	public ServerFailureException(Throwable t) {
		super(t);
	}

	public ServerFailureException(String message) {
		super(message);
	}
}
