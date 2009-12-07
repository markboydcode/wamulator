package org.lds.sso.appwrap.opensso;

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
	public void inject(String headerName, String value, SSOToken token) throws SSOException;
}
