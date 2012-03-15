package org.lds.sso.appwrap.identity.ldap;

import javax.naming.NamingException;

/**
 * Conveys that a connection to ldap was not possible.
 * 
 * @author BoydMR
 *
 */
public class UnableToConnecToLdap extends Exception {

	/**
	 * Serial Version UID for serialization.
	 */
	private static final long serialVersionUID = 1L;

	public UnableToConnecToLdap(NamingException e) {
		super(e);
	}

}
