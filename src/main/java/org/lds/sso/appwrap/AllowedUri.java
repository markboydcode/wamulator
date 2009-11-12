package org.lds.sso.appwrap;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class AllowedUri extends UnenforcedUri {

	protected String[] actions = null;
	
	public AllowedUri(String host, int port, String path) {
		super(host, port, path);
		throw new UnsupportedOperationException("Use four parameter constructor.");
	}
	
	public AllowedUri(String host, int port, String path, String[] actions) {
		super(host, port, path);
		this.actions = actions;
		Set<String> sorted = new TreeSet<String>(Arrays.asList(actions));
		this.id = host + ":" + port + path + sorted.toString();
	}
	
	public boolean allowed(String action) {
		for(String a : actions) {
			if (a.equals(action)) {
				return true;
			}
		}
		return false;
	}
}
