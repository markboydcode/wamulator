package org.lds.sso.appwrap.opensso;

import org.lds.sso.plugins.policy.conditions.evaluator.syntax.HasAssignment;

/**
 * Holds constants from the LegacyPropsInjector so that we don't get all of its
 * dependencies injected into this project due to its import declarations. This
 * may cause breakage if those values ever change but that would occur anyway
 * since the compile takes static finals and hard-codes them into the compiled
 * classes.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class LegacyPropsInjectorDefs {
    public static final String CP_PREFIX = "policy-";
    public static final String CP_POSITIONS_SESSION_PROPERTY = CP_PREFIX + "positions";
	public static final String EMPTY_VALUE_INDICATOR = "-";
    public static final String CACHED_VALUE = HasAssignment.class.getSimpleName() + "-pos";
    public static final String CACHE_TYPE = HasAssignment.class.getSimpleName() + "-type";
	public static final String NONE = "N";
	public static final String SINGLE = "S";
	public static final String MULTI_CHAR = "M"; // warning, must be one char!!
    public static final String CP_UNITS_SESSION_PROPERTY = CP_PREFIX + "units";
    public static final String CP_GENDER = CP_PREFIX + "gender";
    public static final String CP_BIRTH_DATE = CP_PREFIX + "birthdate";
    public static final String CP_UNITS_SESSION_PROPERTY_FOR_POLICY_EVAL = CP_PREFIX + "units-for-eval";
    public static final String CP_STATUS_PROPERTY = CP_PREFIX + "status";
	public static final String CP_LDS_ACCOUNT_ID_PROPERTY = CP_PREFIX + "lds-account-id";
    public static final String CP_DN = CP_PREFIX + "dn";
    public static final String CP_LDS_MRN = CP_PREFIX + "lds-mrn";
    public static final String CP_EMAIL = CP_PREFIX + "email";

}
