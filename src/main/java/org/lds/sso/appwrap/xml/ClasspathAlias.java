package org.lds.sso.appwrap.xml;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.lds.sso.appwrap.XmlConfigLoader2;
import org.lds.sso.appwrap.exception.ConfigParserException;

public class ClasspathAlias extends Alias {
	public static final String TAG_NAME = "classpath-alias";

	public ClasspathAlias(String name, String value) {
		super(name, value);
	}
	
	public ClasspathAlias(String name, String location, String defaultValue) {
		super(name, location, defaultValue);
	}
	
	protected final String deriveFrom(String location) {
		ClassLoader cldr = this.getClass().getClassLoader();
        InputStream src = cldr.getResourceAsStream(location);
        
        if (src == null) {
            throw new ConfigParserException(
                    "Classpath alias resource '" + location
                            + "' not found.");
        } 
        
        try {
        	return StringUtils.trimToEmpty(IOUtils.toString(src));
        } catch ( IOException e ) {
        	throw new ConfigParserException("Unable to load content for alias '"
                    + name
                    + "' from classpath resource '"
                    + location + "'.", e);
        } finally {
        	IOUtils.closeQuietly(src);
        }
	}
	
	public String toString() {
		return XmlConfigLoader2.SRC_CLASSPATH + original;
	}
}
