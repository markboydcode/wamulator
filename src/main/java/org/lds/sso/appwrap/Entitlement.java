package org.lds.sso.appwrap;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents an entitlement consisting of one or more actions, a URN, and 
 * optional condition representative of OES entitlements.  
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class Entitlement implements Comparable<Entitlement>{

    protected String[] actions = null;
    protected String id;
    protected String conditionSyntax = null;
    protected String conditionId = null;
    private String urn;
    

    public String getConditionSyntax() {
        return conditionSyntax;
    }

    public String getConditionId() {
        return conditionId;
    }

    public String getUrn() {
        return urn;
    }

    public Entitlement(String policyDomain, String urn, String[] actions, String conditionId, String conditionSyntax) {
        // strip off any trailing slashes
        if (urn.length() > 1 && urn.endsWith("/")) {
            urn = urn.substring(0, urn.length()-1);
        }

        this.urn = policyDomain + urn;
        this.actions = actions;
        this.conditionId = conditionId;
        this.conditionSyntax = conditionSyntax;
        Set<String> sorted = new TreeSet<String>(Arrays.asList(actions));
        this.id = urn + ":" + sorted.toString() + ":" + conditionId;
    }
    
    public String toString() {
        return id;
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
