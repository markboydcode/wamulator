package org.lds.sso.appwrap;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents an entitlement consisting of one or more actions, a URN, and 
 * optional condition.  
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class Entitlement implements Comparable<Entitlement>{

	protected String[] actions = null;
	   protected boolean usePathPrefixMatching = false;
	    protected String pathPrefix = null;
	    protected String pathMatch = null;
	    protected String id;

    public Entitlement(String[] actions, String urn) {
        this.pathMatch = urn;
        this.actions = actions;
        Set<String> sorted = new TreeSet<String>(Arrays.asList(actions));
        this.id = urn + sorted.toString();
        
        if (urn.startsWith("*")) {
            pathPrefix = "";
            usePathPrefixMatching = true;
        } else if (urn.endsWith("*")) {
            pathPrefix = urn.substring(0, urn.length() - 2);
            usePathPrefixMatching = true;
        }
    }
	
    public boolean matches(String urn) {
        if (usePathPrefixMatching) {
            if (urn.startsWith(pathPrefix)) {
                return true;
            }
        } else {
            if (urn.equals(pathMatch)) {
                return true;
            }
        }
        return false;
    }

	public boolean allowed(String action) {
		for(String a : actions) {
			if (a.equals(action)) {
				return true;
			}
		}
		return false;
	}

    public int compareTo(Entitlement o) {
        return id.compareTo(o.id);
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == null || ! (arg0 instanceof Entitlement)) {
            return false;
        }
        Entitlement ent = (Entitlement) arg0;
        return this.id.equals(ent.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
