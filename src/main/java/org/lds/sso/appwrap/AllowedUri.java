package org.lds.sso.appwrap;

import org.lds.sso.appwrap.AppEndPoint.InboundScheme;

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
public class AllowedUri extends OrderedUri {

	private static final String ALLOW_ALL_ACTIONS_PATTERN = "*";
    protected String[] actions = null;
    private boolean allow_all;
	
	public AllowedUri(InboundScheme scheme, String host, int port, String path, String query, String[] actions, String cpathDeclaration) {
		super(scheme, host, port, path, query, cpathDeclaration);
		this.actions = actions;
		for(String action : actions) {
		    if (action.equals(ALLOW_ALL_ACTIONS_PATTERN)) {
		        allow_all = true;
		        break;
		    }
		}
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

    /**
     * Returns true if all actions are allowed or if the given action is included in the allowed set of actions.
     *
     * @param action
     * @return
     */
    public boolean allowed(String action) {
        if (allow_all) {
            return true;
        }
		for(String a : actions) {
			if (a.equals(action)) {
				return true;
			}
		}
		return false;
	}
}

