package org.lds.sso.appwrap.conditions.evaluator;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines constants for the user headers used in the SSO environment.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2010, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class UserHeaderNames {
    /**
     * The prefix of all session attributes injected by this processor.
     */
    public static final String PREFIX = "policy-";
    
    /**
     * Custom properties injected.
     */
    public static final String UNITS = PREFIX + "ldsunits";
    public static final String GENDER = PREFIX + "gender";
    public static final String BIRTH_DATE = PREFIX + "ldsbdate";
    public static final String POSITIONS = PREFIX + "ldspositions";
	public static final String LDS_ACCOUNT_ID = PREFIX + "ldsaccountid";
    public static final String DN = PREFIX + "dn";
    public static final String SN = PREFIX + "sn";
    public static final String CN = PREFIX + "cn";
    public static final String GIVEN_NAME = PREFIX + "givenname";
    public static final String PREFERRED_NAME = PREFIX + "preferredname";
    public static final String PREFERRED_LANG = PREFIX + "preferredlanguage";
    public static final String LDS_MRN = PREFIX + "ldsmrn";
    public static final String EMAIL = PREFIX + "ldsemailaddress";
    public static final String INDIVIDUAL_ID = PREFIX + "ldsindividualid";
    
    public static final String SERVICE_URL = PREFIX + "service-url";

	/**
	 * Prefix of position identifiers for compile time ties to conditions that 
	 * use it.
	 */
    public static final String POSITION_HEADER_PREFIX = "p";
    public static final String POSITION_HEADER_PREFIX_UC = "P";

    /**
     * The character which is used to separate multiple
     * values the position hierarchy in the positions header.
     */
    public static final char MULTI_VALUES_DELIMITER = ':';
    
    /**
     * The character which starts all unit integer identifiers in headers. 
     */
    public static final char UNIT_PREFIX = 'u';
    public static final char UNIT_PREFIX_UC = 'U';

    /**
     * The character which is used to delimite and terminate unit identifiers
     * the both the unit and positions headers. 
     */
    public static final char UNITS_DELIMITER = '/';

    /**
     * The value used for user headers if no value has been specified via an
     * sso-header directive on the user.
     */
	public static final String EMPTY_VALUE_INDICATOR = "";
	
	/**
	 * Formatter for the birthdate header passed to applications. Formats 
	 * dates using a SimpleDateFormat of "yyyy-MM-dd" which produces a String
	 * like "2009-07-04" for the fourth of July in 2009.
	 */
	public static final SimpleDateFormat BIRTHDATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
	
	/**
	 * Set of headers expected to be injected by SSO. 
	 */
    public static Map<String,String> defaultHeaders = new HashMap<String,String>();

    /**
     * Sets up default headers so values are had by the user even if not 
     * specified within config file.
     */
    static {
        Map<String,String> hdr = new HashMap<String,String>(); 
    
        hdr.put(UserHeaderNames.UNITS, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        hdr.put(UserHeaderNames.POSITIONS, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        hdr.put(UserHeaderNames.LDS_ACCOUNT_ID, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        hdr.put(UserHeaderNames.DN, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        hdr.put(UserHeaderNames.EMAIL, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        hdr.put(UserHeaderNames.LDS_MRN, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        hdr.put(UserHeaderNames.SN, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        hdr.put(UserHeaderNames.PREFERRED_NAME, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        hdr.put(UserHeaderNames.INDIVIDUAL_ID, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        hdr.put(UserHeaderNames.GIVEN_NAME, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        hdr.put(UserHeaderNames.GENDER, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        hdr.put(UserHeaderNames.CN, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        hdr.put(UserHeaderNames.BIRTH_DATE, UserHeaderNames.EMPTY_VALUE_INDICATOR);
        
        defaultHeaders = Collections.unmodifiableMap(hdr);
    }
}
