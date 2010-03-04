package org.lds.sso.appwrap.opensso;

import java.util.Map;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

/**
 * Simple interface representing transformation of lds user attribute values to 
 * values used in SSOToken by custom condition evaluators.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public interface HeadersToSSOTokenInjector {
	/**
	 * Should clear out anything injected via {@link #inject(String, String, SSOToken)}
	 * from the passed-in map that must be the Map backing the SSOToken.
	 */
	public void clearOld(String headerName, Map<String, String> tokenValues);
	
	/**
	 * Injects the headerName and value into the SSOToken along with any other
	 * versions needed by the custom syntax evaluators. 
	 * 
	 * @param headerName
	 * @param value
	 * @param token
	 * @throws SSOException
	 */
	public void inject(String headerName, String value, SSOToken token) throws SSOException;
}
