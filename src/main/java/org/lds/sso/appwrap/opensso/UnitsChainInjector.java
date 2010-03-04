package org.lds.sso.appwrap.opensso;

import java.util.Map;

import org.lds.sso.plugins.authz.LegacyPropsInjector;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

/**
 * Injects the header as-is then also translates to the cached value used by the
 * custom syntax and injects it. The unit chain headera have the following form
 * including unit type characters prefixing the unit identifiers:
 * 
 * L##:M##:N##
 * 
 * The conditions use the same chain but without the type characters:
 * 
 * ##:##:##
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class UnitsChainInjector implements HeadersToSSOTokenInjector {

	public void inject(String headerName, String value, SSOToken token) throws SSOException {
		token.setProperty(LegacyPropsInjector.CP_UNITS_SESSION_PROPERTY, value);
		
		// now add the values used by the conditions
		token.setProperty(LegacyPropsInjector.CP_UNITS_SESSION_PROPERTY_FOR_POLICY_EVAL, 
				stripOutTypeChars(value));
	}
	
	/**
	 * Strips out the unit type characters making a unit chain like this:
	 * 
	 * L##:M##:N##
	 * 
	 * become this:
	 * 
	 * ##:##:##
	 * 
	 * @param unitsChain
	 * @return
	 */
	public static final String stripOutTypeChars(String unitsChain) {
		String[] tokens = unitsChain.split("" + LegacyPropsInjector.UNITS_DELIMITER);
		StringBuffer ret = new StringBuffer();
		
		for(String tok : tokens) {
			ret.append(LegacyPropsInjector.UNITS_DELIMITER);
			ret.append(tok.substring(1));
		}
		return ret.toString().substring(1);
	}

	public void clearOld(String headerName, Map<String, String> tokenValues) {
		tokenValues.remove(LegacyPropsInjector.CP_UNITS_SESSION_PROPERTY);
		tokenValues.remove(LegacyPropsInjector.CP_UNITS_SESSION_PROPERTY_FOR_POLICY_EVAL);
	}
}
