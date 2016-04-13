package org.lds.sso.appwrap;

import org.lds.sso.appwrap.AppEndPoint.InboundScheme;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;


public class DeniedUri extends OrderedUri {

	private static final String DENY_ALL_ACTIONS_PATTERN = "*";
    protected String[] actions = null;
    private boolean deny_all;
	
	public DeniedUri(InboundScheme scheme, String host, int port, String path, String query, String[] actions, String cpathDeclaration) {
		super(scheme, host, port, path, query, cpathDeclaration);
		this.actions = actions;
		for(String action : actions) {
		    if (action.equals(DENY_ALL_ACTIONS_PATTERN)) {
		        deny_all = true;
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
     * Returns true if all actions are denied or if the specified action is in the list of actions for this Uri.
     * @param action
     * @return
     */
    public boolean denied(String action) {
        if (deny_all) {
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

