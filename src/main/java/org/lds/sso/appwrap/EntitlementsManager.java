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
    private LogicalSyntaxEvaluationEngine cEngine;
    protected Map<String, String> cSynMap;
    protected Map<Entitlement, String> conditionsMap = new HashMap<Entitlement, String>();
    private Set<Entitlement> entitlements = new TreeSet<Entitlement>();

    public EntitlementsManager(LogicalSyntaxEvaluationEngine eng, Map<String, String> syntax) {
        this.cEngine = eng;
        this.cSynMap = syntax;
    }
    public void setConditionEnv(LogicalSyntaxEvaluationEngine engine, Map<String,String>syntaxMap) {
        cEngine = engine;
        cSynMap = syntaxMap;
    }

    public void addEntitlement(Entitlement ent, String condId, String condSyntax) {
        this.entitlements.add(ent);

        if (condId != null && condSyntax != null) {
            cSynMap.put(condId, condSyntax);
            conditionsMap.put(ent, condId);
        }
    }
    
    public boolean isAllowed(String action, String urn, User user) {
        for (Entitlement ent : entitlements) {
            if (ent.matches(urn) && ent.allowed(action)) {
                String condId = conditionsMap.get(ent);
                if (condId == null) { // no condition needs to be met
                    return true;
                } else { // must further meet conditions for access
                    String syntax = cSynMap.get(condId);
                    IEvaluator evaluator = null;
                    try {
                        evaluator = cEngine.getEvaluator(syntax);
                    } catch (EvaluationException e1) {
                        cLog
                                .error(
                                        "Disallowing entitlement "
                                                + ent
                                                + " since unable to obtain evaluator for condition alias "
                                                + condId + " with syntax "
                                                + syntax + ". ", e1);
                        return false;
                    }
                    EvaluationContext ctx = new EvaluationContext(user,
                            new HashMap<String, String>());
                    try {
                        return evaluator.isConditionSatisfied(ctx);
                    } catch (EvaluationException e) {
                        cLog.error("Error occurred for entitlement " + ent
                                + " for user " + user.getUsername()
                                + " denying.", e);
                    }
                }
                return true;
            }
        }
        return false;
    }


}
