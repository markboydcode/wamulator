package org.lds.sso.appwrap.xml;

import org.lds.sso.appwrap.XmlConfigLoader2;

public class SystemAlias extends Alias {
	
	public static final String TAG_NAME = "system-alias";

	public SystemAlias(String name, String value) {
		super(name, value);
	}

	public SystemAlias(String name, String location, String defaultValue) {
		super(name, location, defaultValue);
	}
	
	@Override
	protected final String deriveFrom(String location) {
        String val = System.getProperty(location);

        if (val == null) {
            throw new IllegalArgumentException(
                    "System alias resource '"
                            + location
                            + "' not found in java.lang.System.");
        }
        
        return val;
	}

	public String toString() {
		return XmlConfigLoader2.SRC_SYSTEM + original;
	}
}
