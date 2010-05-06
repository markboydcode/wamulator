package org.lds.sso.appwrap.conditions.evaluator;

import java.text.SimpleDateFormat;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.lds.sso.appwrap.conditions.evaluator.syntax.HasAssignment;

/**
 * Implementation of an OpenSSO post authentication processor for injecting
 * legacy attributes into the OpenSSO session for use in configuring policies
 * and passing atributes to applications.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class LegacyPropsInjector {
	/**
	 * OpenSSO headers injected due to config of the ldap data store user atts
	 * and then the access control, authentication, advanced properties to 
	 * inject user atts into the session are all prefixed with this value in
	 * front of the ldap user attribute value. Values injected by this processor
	 * are not prefixed.
	 */
	public static final String OSSO_PREFIX = "am.protected.";
	
    /**
     * The attribute to be looked for in the open sso session whose value is the
     * user's lds member record number if had by the user.
     */
	public static final String cLdsMrnAtt = OSSO_PREFIX + "ldsMRN";
	
	/**
	 * Identifies the lds account id that will be looked for in the session. 
	 * The part after the prefix is what should show in the AccessControl tab,
	 * root real link, Authentication tab, Advanced button for the User 
	 * Attribute Mapping to Session Attribute.
	 */
	public static final String OSSO_LDS_ACCT_ID = OSSO_PREFIX + "ldsAccountId"; 
	public static final String OSSO_DN = OSSO_PREFIX + "entrydn"; 
	public static final String OSSO_EMAIL = OSSO_PREFIX + "ldsEmailAddress"; 
    
    /**
     * The prefix of all session attributes injected by this processor.
     */
    public static final String CP_PREFIX = "policy-";
    
    /**
     * Custom properties injected by this processor.
     */
    public static final String CP_UNITS_SESSION_PROPERTY = CP_PREFIX + "units";
    public static final String CP_GENDER = CP_PREFIX + "gender";
    public static final String CP_BIRTH_DATE = CP_PREFIX + "birthdate";
    public static final String CP_POSITIONS_SESSION_PROPERTY = CP_PREFIX + "positions";
    public static final String CP_STATUS_PROPERTY = CP_PREFIX + "status";
	public static final String CP_LDS_ACCOUNT_ID_PROPERTY = CP_PREFIX + "lds-account-id";
    public static final String CP_DN = CP_PREFIX + "dn";
    public static final String CP_LDS_MRN = CP_PREFIX + "lds-mrn";
    public static final String CP_EMAIL = CP_PREFIX + "email";
    public static final String CP_INDIVIDUAL_ID = CP_PREFIX + "individual-id";

	/**
	 * Prefix of position identifiers for compile time ties to conditions that 
	 * use it.
	 */
	public static final String POSITION_HEADER_PREFIX = "p";

	/**
	 * Prefix of area unit identifiers. The cmis web service in its current form only supports getting a type 
	 * String from unit and parent organization objects and I strip the first
	 * character as an indication of the unit type. Area doesn't have the type
	 * api call so the prefix is hard coded here an used to tie the condition
	 * class here via a compile time dependency. 
	 */
	public static final String AREA_PREFIX = "A";
	
    /**
     * The character which is used by the opensso session to separate multiple
     * values of a property transferred as a single concatenated string causing
     * mis-interpretation of values from ldap that contain this delimiter.
     */
    public static final char OPENSSO_MULTI_VALUES_DELIMITER = ':';
    
    /**
     * The character which starts all unit integer identifiers in headers. 
     */
    public static final char UNIT_PREFIX = 'u';

    /**
     * The character which is used to replace the opensso session property
     * condition default delimiter for ldap values obtained through the session
     * so that SessionPropertyCondition doesn't treat the single value as
     * multiple values. 
     */
    public static final char UNITS_DELIMITER = '/';

    /**
     * Session values injected with an empty string do not get injected as
     * headers by agent configuration. So to ensure that a header will always
     * be injected and thence replace any attempts by rogue clients to spoof
     * the agent passing information, we use this value to ensure that a
     * header is always injected by the agent.
     */
	public static final String EMPTY_VALUE_INDICATOR = "-";

	/**
	 * The key by which assignments are cached in the sso session for use in 
	 * speeding policy evaluation without the need to parse the assignments
	 * string.
	 */
    public static final String CACHED_VALUE = HasAssignment.class.getSimpleName() + "-pos";

    /**
     * A value pushed into the sso session to indicate what kind of assignment
     * list is available in the session: none, single assignment, or multiple.
     * For multiple the number of chars indicates the total number of 
     * assignments.
     */
    public static final String CACHE_TYPE = HasAssignment.class.getSimpleName() + "-type";
	public static final String NONE = "N";
	public static final String SINGLE = "S";
	public static final String MULTI_CHAR = "M"; // warning, must be one char!!
	
	/**
	 * Formatter for the birthdate header passed to applications. Formats 
	 * dates using a SimpleDateFormat of "yyyy-MM-dd" which produces a String
	 * like "2009-07-04" for the fourth of July in 2009.
	 */
	public static final SimpleDateFormat BIRTHDATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
}
