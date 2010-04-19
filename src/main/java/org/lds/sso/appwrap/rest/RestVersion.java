package org.lds.sso.appwrap.rest;

import java.util.Map;
import java.util.TreeMap;

/**
 * Defines the allowed values of the configuration file's rest-version attribute
 * declared in the config file.
 * 
 * @author boydmr
 *
 */
public enum RestVersion {
	/**
	 * Church Defined Oracle Entitlements Server version 1 rest interface.
	 */
	CD_OESv1("CD-OESv1"), 
	/**
	 * OpenSSO rest interface defined by OpenSSO 8. 
	 */
	OPENSSO("openSSO");
	
	private final String vId;
	
	private static final Map<String, RestVersion> VERSIONS = new TreeMap<String, RestVersion>();
	
	static {
		for(RestVersion ver : values()) {
			VERSIONS.put(ver.getVersionId(), ver);
		}
	}
	
	/**
	 * Returns the RestVersion that represents the version identifier used in 
	 * the config file for specifying the version of the rest interface that 
	 * should be exposed or null if no defined value is found.
	 * 
	 * @param vId
	 * @return
	 */
	public static RestVersion findRestVersionById(String vId) {
		return VERSIONS.get(vId);
	}
	
	/**
	 * Construct a RestVersion for the version identifier that will be used in 
	 * the configuration file.
	 * 
	 * @param vId
	 */
	private RestVersion(String vId) {
		this.vId = vId;
	}
	
	/**
	 * Returns the identifier that should be used in the configuration file
	 * for specifying a RestVersion to be exposed.
	 * @return
	 */
	public String getVersionId() {
		return vId;
	}

	/**
	 * Returns the set of registered version identifiers comma and space separated.
	 * 
	 * @return
	 */
	public static String getValidIdentifiers() {
		StringBuffer bfr = new StringBuffer();
		boolean first = true;
		bfr.append("[ ");
		for (String vid : VERSIONS.keySet()) {
			if (first) {
				first = false;
			}
			else {
				bfr.append(", ");
			}
			bfr.append(vid);
		}
		bfr.append(" ]");
		return bfr.toString();
	}
}
