package org.lds.sso.appwrap.identity.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;


/**
 * An exception occurred while extracting attribute values from the 
 * {@link Attribute}s object.
 *  
 * @author BoydMR
 *
 */
public class UnableToLoadUserAttributes extends Exception {

	/**
	 * Serial Version UID for serialization.
	 */
	private static final long serialVersionUID = 1L;

	public UnableToLoadUserAttributes(NamingException e) {
		super(e);
	}
}
