package org.lds.sso.appwrap.identity.ldap;

import javax.naming.NamingException;

/**
 * Thrown if a {@link NamingException} occurred while performing a search for a user.
 * 
 * @author BoydMR
 *
 */
public class UnableToSearchForUser extends Exception {

	/**
	 * Serial Version UID for serialization.
	 */
	private static final long serialVersionUID = 1L;

	public UnableToSearchForUser(NamingException e) {
		super(e);
	}

}
