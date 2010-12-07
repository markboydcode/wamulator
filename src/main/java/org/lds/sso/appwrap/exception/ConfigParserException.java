package org.lds.sso.appwrap.exception;

public class ConfigParserException extends RuntimeException {
	public ConfigParserException(String s) {
		super(s);
	}
	public ConfigParserException(String s, Throwable t) {
		super(s, t);
	}
}
