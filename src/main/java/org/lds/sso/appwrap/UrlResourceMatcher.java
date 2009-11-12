package org.lds.sso.appwrap;

public interface UrlResourceMatcher {

	public boolean matches(String host, int port, String relUrl);
	public boolean isUnenforced(String relUrl);
	public boolean isAllowed(String action, String relUrl);
}
