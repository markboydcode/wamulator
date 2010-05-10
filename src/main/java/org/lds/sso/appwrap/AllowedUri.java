package org.lds.sso.appwrap;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Adds semantics of actions on the URI for restricting access. 
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class AllowedUri extends UnenforcedUri {

	protected String[] actions = null;
	
	public AllowedUri(String scheme, String host, int port, String path, String query) {
		super(scheme, host, port, path, query);
		throw new UnsupportedOperationException("Use other constructor.");
	}
	
	public AllowedUri(String scheme, String host, int port, String path, String query, String[] actions) {
		super(scheme, host, port, path, query);
		this.actions = actions;
		updateId();
	}
	
	@Override
    protected void updateId() {
        super.updateId();
        // if actions are null then the id as specified by parent is done
        if (actions != null) {
            Set<String> sorted = new TreeSet<String>(Arrays.asList(actions));
            this.id = this.id + sorted.toString();
        }
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
