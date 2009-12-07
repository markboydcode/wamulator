package org.lds.sso.appwrap.opensso;

import java.util.HashMap;
import java.util.Map;



import com.sun.identity.shared.debug.IDebug;
import com.sun.identity.shared.debug.IDebugProvider;

/**
 * Custom provider allowing for custom implementation of underlying logging
 * system within opensso's Debug class. This allows for use of Log4j and
 * decoupling from the rest of opensso's infrastructure when using the custom
 * syntax engine.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class MyProvider implements IDebugProvider {
	public static final Map<String, IDebug> inst = new HashMap<String, IDebug>();
	
	public IDebug getInstance(String debugName) {
		IDebug d = inst.get(debugName);
		if (d == null) {
			d = new MyDebug(debugName);
			inst.put(debugName, d);
		}
		return d;
	}
}