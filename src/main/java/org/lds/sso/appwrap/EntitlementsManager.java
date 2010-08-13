package org.lds.sso.appwrap;

import java.util.HashMap; 
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationContext;
import org.lds.sso.appwrap.conditions.evaluator.EvaluationException;
import org.lds.sso.appwrap.conditions.evaluator.IEvaluator;
import org.lds.sso.appwrap.conditions.evaluator.LogicalSyntaxEvaluationEngine;

/**
 * Manager of entitlements.
 * 
 * @author boydmr
 *
 */
public class EntitlementsManager {
    private static final Logger cLog = Logger.getLogger(EntitlementsManager.class);
    private LogicalSyntaxEvaluationEngine cEngine = null;
    protected Map<String, Entitlement> entitlements = new HashMap<String, Entitlement>();

    public EntitlementsManager(LogicalSyntaxEvaluationEngine eng) {
        this.cEngine = eng;
    }

    public void addEntitlement(Entitlement ent) {
        for(String action : ent.actions) {
            String key = action + ":" + ent.getUrn();
            this.entitlements.put(key, ent);
        }
    }
    
    /**
     * Implements Oracle Entitlements Server hierarchical approach to entitlement
     * evaluation. It walks down through the hierarchy looking for conditions 
     * and as soon as it finds one that grants access it returns true. This has 
     * the effect of granting all URNs that happen to reside below that level to
     * that user. For example, suppose I had the following entitlement:
     *
     *  <allow action='GET' urn='/root/ward/stake' condition='{{stake-pres}}'/>" 
     *  <allow action='GET' urn='/root/ward' condition='{{bishop}}'/>"
     *  <allow action='GET' urn='/' condition='{{super-user}}'/>" 
     * 
     * Evaluation would proceed as follows:
     * 
     *  / = match found but super-user condition not met
     *  /root = no match found hence no condition
     *  /root/ward = match found and condition satisfied, return true for /root/ward/stake.
     *
     * From the configured entitlements such a setup would appear to be incorrectly
     * implementing the intent since the bishop would also get what erroneously
     * was meant only for the stake president. To do this correctly both
     * conditions would have to be at the same level of the hierarchy like so:
     * 
     *  <allow action='GET' urn='/root/page/stake' condition='{{stake-pres}}'/>" 
     *  <allow action='GET' urn='/root/page/ward' condition='{{bishop}}'/>"
     *  <allow action='GET' urn='/' condition='{{super-user}}'/>" 
     * 
     * And the URNs passed in accordingly to protect the stake and ward pages
     * distinctly.
     * 
     * @param action
     * @param urn
     * @param user
     * @param ctx
     * @return
     */
    public boolean isAllowed(String policyDomain, String action, String urn, User user, Map<String, String> ctx) {
        if (user == null) {
            return false; // must be authenticated to get entitlements
        }
        if (urn.length() > 1 && urn.endsWith("/")) {
            urn = urn.substring(0, urn.length()-1);
        }
        String[] toks = urn.split("/");
        StringBuffer bfr = new StringBuffer(action).append(":").append(policyDomain);
        boolean tokenWasAppended = false;
        boolean rootBeingProcessed = true;
        
        for (String token : toks) {
            if (rootBeingProcessed) {
                bfr.append("/");
                rootBeingProcessed = false;
            }
            else {
                if (tokenWasAppended) {
                    bfr.append("/");
                }
                bfr.append(token);
                tokenWasAppended = true;
            }
            String evalUrn = bfr.toString();
            Entitlement ent = entitlements.get(evalUrn);
            
            if (ent != null) {
                if (ent.getConditionId() == null) {
                    return true; // only requires authentication
                } else { // see if they meet conditions for access
                    IEvaluator evaluator = null;
                    try {
                        evaluator = cEngine.getEvaluator(ent.conditionSyntax);
                    } catch (EvaluationException e1) {
                        cLog.error("Unable to evaluate condition for entitlement "
                                + ent.getUrn()
                                + ". Unable to obtain evaluator for condition alias "
                                + ent.getConditionId() + " with syntax "
                                + ent.getConditionSyntax() 
                                + ". ", e1);
                        continue; // go to next level
                    }
                    EvaluationContext evaluationContext = new EvaluationContext(user, ctx);
                    try {
                        if (evaluator.isConditionSatisfied(evaluationContext)) {
                            return true;
                        }
                    } catch (EvaluationException e) {
                        cLog.error("Error occurred for entitlement " + ent.getUrn()
                                + " for condition alias " + ent.getConditionId()
                                + " when evaluating for user " + user.getUsername()
                                + ".", e);
                    }
                }
            }
        }
        // if didn't meet any conditions for any levels of the matching 
        // hierarchy then they are denied access.
        return false;
    }


}
