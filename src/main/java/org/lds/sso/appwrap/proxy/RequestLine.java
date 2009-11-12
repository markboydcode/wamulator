package org.lds.sso.appwrap.proxy;

public interface RequestLine {
	public String getMethod();
	public String getUri();
	public String getHttpDecl();
}
