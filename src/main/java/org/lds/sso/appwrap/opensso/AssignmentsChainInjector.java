package org.lds.sso.appwrap.opensso;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

/**
 * Translates the policy-positions header into the values used by the condition
 * syntax to evaluate access policies. The value of the header can contain 
 * one to many assignment identifiers. Each is delimited by a  pipe character, 
 * '|'. Each assingment has a 'P' followed by the numeric position ID,
 * followed by the unit containment hierarchy of the unit of the position. The
 * following example show s bishop, position id=4, in a ward within a stake
 * within an area. The second represents the stake president of that stake within
 * the same area.
 * 
 * P4:W2300:S345:A228
 * P1:S345:A228 
 * 
 * If a single individual held both positions then the header could look like 
 * either of the following.
 * 
 * P4:W2300:S345:A228|P1:S345:A228 
 * P1:S345:A228|P4:W2300:S345:A228
 * 
 * The form of these assignments used by the conditions has the unit type 
 * characters removed and each is made available in its own cached value along
 * with a cache entry indicating how many there are.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class AssignmentsChainInjector implements HeadersToSSOTokenInjector {

	public void inject(String headerName, String value, SSOToken token) throws SSOException {
		token.setProperty(headerName, value);

        StringBuffer cachedValsIndicator = new StringBuffer();

		String[] assigs = value.split("\\|");
		
		if (assigs.length == 1) {
        	token.setProperty(LegacyPropsInjectorDefs.CACHE_TYPE, LegacyPropsInjectorDefs.SINGLE);
            token.setProperty(LegacyPropsInjectorDefs.CACHED_VALUE + "0", 
            		"P" + UnitsChainInjector.stripOutTypeChars(value));
		}
		else if (assigs.length > 1) {
	        StringBuffer cachedCountIndicator = new StringBuffer();
	        int count = -1;

	        for (String assig:assigs) {
	        	count++;
	            cachedValsIndicator.append(LegacyPropsInjectorDefs.MULTI_CHAR);
	            token.setProperty(LegacyPropsInjectorDefs.CACHED_VALUE + count, 
	            		"P" + UnitsChainInjector.stripOutTypeChars(assig));
	        }
        	token.setProperty(LegacyPropsInjectorDefs.CACHE_TYPE, cachedValsIndicator.toString());
		}
	}

}
