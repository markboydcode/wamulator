package org.lds.sso.appwrap.xml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.lds.sso.appwrap.XmlConfigLoader2;
import org.lds.sso.appwrap.exception.ConfigParserException;

/**
 * A read-only representation of an alias processing instruction.  Each alias has a type--system, classpath, file, or plaintext.
 * Formerly, the type was embedded in the processing instruction data like this:
 * 
 * <?alias alias-name=system:alias-system-property?>
 * 
 * where, in this example, this alias was of type "system".  Presently, the type is now represented in the target like this:
 * 
 * <?system-alias alias-name=alias-system-property?>
 * 
 * This class and its subclasses are the higher-level representation and are meant to represent both instructions.  To go directly from a
 * processing instruction to the appropriate Alias representation, see the fromString method.
 * 
 * The method for resolving alias values in a string can also be found here, having been moved from XmlConfigLoader2.
 * 
 * @author joshcummings
 *
 */
public class Alias {
	public final String name;      // the name of the alias
	public final String value;     // the derived alias value
	public final String original;  // the original alias value
	
	// regex for old-style aliases
	private static final Pattern propertyPattern = Pattern.compile("(((\\w\\s\\w)|([\\w\\-_]))*)\\s*=\\s*\"?(system:|classpath:|file:)?([\\w\\-_\\s\\./\\*]*)\"?\\s*");
	
	// regex for typed aliases
	private static final Pattern typedPropertyPattern = Pattern.compile("([\\w\\-_]*)\\s*=\\s*((\"([^\"]*)\")|([\\w\\-_\\./\\*\\<\\>]*))");
	
	// regex for default property in typed aliases
	private static final Pattern defaultValuePattern = Pattern.compile("default\\s*=\\s*((\"([^\"]*)\")|([\\w\\-_\\./\\*\\<\\>\\{\\}\\']*))");
	
	public Alias(String name, String value) {
		this.name = name;
		this.value = resolveAliases(value);
		this.original = value;
	}
	
	protected Alias(String name, String location, String defaultValue) {
		this.name = name;
		this.value = resolveAliases(deriveFrom(location, defaultValue));
		this.original = location;
	}
	
	/**
	 * Look up the original alias value--location--by the appropriate strategy.  If it cannot be found,
	 * assign the default value.  If that fails as well, then blow up.
	 * 
	 * @param location - the original alias value--from where the actual alias value can be derived
	 * @param defaultValue - the fallback alias value if the actual alias value can't be derived
	 * @return The actual alias value or default alias value if the actual can't be derived
	 */
	protected final String deriveFrom(String location, String defaultValue) {
		String retVal = null;
		try {
			retVal = deriveFrom(location);
		} catch ( Exception e ) {
			if ( defaultValue == null ) {
				throw new ConfigParserException("Problematic alias has no default value", e);
			} else {
				retVal = resolveAliases(defaultValue);
			}
		}
		return retVal;
	}
	
	/**
	 * Class specific way to derive the actual alias value.
	 * 
	 * @param location - the original alias value--from where the actual alias value can be derived
	 * @return
	 */
	protected String deriveFrom(String location) {
		return null;
	}
	
	/**
	 * Given the raw XML processing instruction, create the appropriate Alias representation
	 * 
	 * @param type - if old-style alias syntax, then null, otherwise, the processing instruction target
	 * @param value - the processing instruction data
	 * @return - The appropriate Alias representation
	 */
	public static final Alias fromString(String type, String value) {
		value = value.trim();
		Matcher propertyMatcher = typedPropertyPattern.matcher(value);
		if ( type == null ) { // if the type is null, then we assume old-style aliases where the type must be derived from the data. 
			propertyMatcher = propertyPattern.matcher(value);
		}
		
		if ( propertyMatcher.find() ) {
			String name = propertyMatcher.group(1);
			String location = null;
			if ( type == null ) {
				type = propertyMatcher.group(5);
				location = propertyMatcher.group(6);
			} else {
				location = propertyMatcher.group(4);
				if ( StringUtils.isBlank(location) ) {
					location = propertyMatcher.group(2);
				}
			}
			
			String defaultValue = null;
			Matcher defaultValueMatcher = defaultValuePattern.matcher(value);
			if ( defaultValueMatcher.find() ) {
				defaultValue = defaultValueMatcher.group(1);
			}
			
			if ( XmlConfigLoader2.SRC_CLASSPATH.equals(type) || ClasspathAlias.TAG_NAME.equals(type)) {  // legacy.  It'd be nice not to have both of these
				return new ClasspathAlias(name, location, defaultValue);
			} else if ( XmlConfigLoader2.SRC_FILE.equals(type) || FileAlias.TAG_NAME.equals(type)) {
				return new FileAlias(name, location, defaultValue);
			} else if ( XmlConfigLoader2.SRC_SYSTEM.equals(type) || SystemAlias.TAG_NAME.equals(type)) { 
				return new SystemAlias(name, location, defaultValue);
			} else {
				return new PlaintextAlias(name, location);
			}
		}
		
		throw new ConfigParserException("Couldn't find appropriate representation for alias: " + value);
	}
	
	/**
     * User during config file parsing to resolve references to {{token}} to
     * values stored in the aliases map by "token" key. If not found then an
     * illegal argument exception is thrown.
     * 
     * @param val
     * @return
     */
    public static String resolveAliases(String val) {
        int curIdx = 0;
        int leftCurlys = val.indexOf("{{");
        StringBuffer resolved = new StringBuffer();
        boolean foundEnd = false;

        while (leftCurlys != -1) {
            String text = val.substring(curIdx, leftCurlys);
            resolved.append(text);
            int rightCurlys = val.indexOf("}}", leftCurlys + 2);
            if (rightCurlys == -1) {
                throw new IllegalArgumentException(
                        "Unmatched '}}' for alias in " + val);
            }
            String alias = val.substring(leftCurlys + 2, rightCurlys);
            String value = XmlConfigLoader2.get(XmlConfigLoader2.PARSING_ALIASES).getAliasValue(alias);
            if (value == null) {
                throw new IllegalArgumentException("Can't resolve alias '"
                        + alias + "' in " + val);
            }
            resolved.append(value);
            curIdx = rightCurlys + 2;
            if (curIdx >= val.length()) {
                foundEnd = true;
                break;
            } else {
                leftCurlys = val.indexOf("{{", curIdx);
            }
        }
        if (!foundEnd) {
            resolved.append(val.substring(curIdx));
        }

        return resolved.toString();
    }
	
	public boolean equals(Object obj) {
		if ( obj instanceof Alias ) {
			Alias alias = (Alias)obj;
			return alias.name == null ? this.name == null : alias.name.equals(this.name);
		}
		return false;
	}
}
