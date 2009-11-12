package org.lds.sso.appwrap.ui;

import java.util.AbstractMap;
import java.util.Set;

/**
 * Implementation of Map that doesn't act as a map for use in jsp bean utils 
 * allowing values to be passed in and manipulated or used to return a value
 * for the functionality being implemented.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public abstract class BaseMapImpl<V> extends AbstractMap<String, V> {

	public abstract V get(Object key);
	
	@Override
	public Set<java.util.Map.Entry<String, V>> entrySet() {
		return null;
	}
}
