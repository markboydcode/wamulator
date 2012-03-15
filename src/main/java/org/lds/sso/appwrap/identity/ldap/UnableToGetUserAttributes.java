package org.lds.sso.appwrap.identity.ldap;

import javax.naming.NamingException;

/**
 * Indicates inability to call DirContext.getAttributes() for a user bound
 * context.
 * 
 * @author BoydMR
 *
 */
public class UnableToGetUserAttributes extends Exception {

	/**
	 * Serial Version UID for serialization.
	 */
	private static final long serialVersionUID = 1L;

	public UnableToGetUserAttributes(NamingException e) {
		super(e);
	}

}
