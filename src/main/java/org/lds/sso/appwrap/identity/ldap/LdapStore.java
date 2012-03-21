package org.lds.sso.appwrap.identity.ldap;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.lds.sso.appwrap.proxy.tls.TrustAllManager;

/**
 * Centralizes all LDAP support for searching for users, binding as that user,
 * and returning attributes for that user.
 * 
 * @author BoydMR
 *
 */
public class LdapStore {
	private static Logger cLog = Logger.getLogger(LdapStore.class.getName());
	private static final String SEARCH_BASE_DN = "search-base-dn";
	private static final String ATTRIBUTES_TO_OBTAIN = "atts-to-obtain";
	private static Hashtable<String, Object> srchEnv;

	/**
	 * Singleton one-shot loader of SSL default context for SSL connections to LDAP store.
	 * 
	 * @author BoydMR
	 *
	 */
	private static class SslTrustAllOnDemandLoader {
		private static final boolean installed = installSslTrustAllContext();
		
		/**
		 * Must be called only once.
		 */
		private static boolean installSslTrustAllContext() {
			try {
				SSLContext ctx = SSLContext.getInstance("TLS");
				ctx.init(null, new TrustManager[] {new TrustAllManager()}, null);
	            SSLContext.setDefault(ctx);
	            cLog.info("SSL Trust-All Manager loaded");
	            return true;
			}
			catch (Exception e) {
				cLog.log(Level.SEVERE, "Unable to load SSL Trust-All Manager. "
						+ "SSL LDAP connections will fail without locally "
						+ "loading server certificates.", e);
			}
			return false;
		}
	}

	/**
	 * Searches for the user by common name, authenticates via an ldap compare,
	 * then loads and returns the list of attributes. Such attributes must be 
	 * visible to that user to be extracted here not to the user used for 
	 * performing the search.
	 * 
	 * @param cn
	 * @return
	 * @throws UnableToBindEndUser 
	 * @throws UnableToBindSearchUser 
	 * @throws UnableToConnecToLdap 
	 * @throws UnableToFindUser 
	 * @throws UnableToLoadUserAttributes 
	 * @throws UnableToSearchForUser 
	 * @throws UserNotFound 
	 * @throws UnableToGetUserAttributes 
	 * @throws IllegalStateException if the LDAP environment has not yet been set 
	 */
	public static Map<String, List<String>> getUserAttributes(String cn, String pwd)
			throws UnableToConnecToLdap, UnableToBindSearchUser, UserNotFound,
			UnableToSearchForUser, UnableToBindEndUser, UnableToGetUserAttributes,
			UnableToLoadUserAttributes {
		Map<String, List<String>> atts = new HashMap<String, List<String>> ();

		if (srchEnv == null) {
			throw new IllegalStateException("LDAP Environment not yet set. No attributes can be obtained for " + cn);
		}
		
		DirContext ctx = bindForSearch(srchEnv);
		String dn = findUserDn(ctx, cn);
		cLog.info("Found " + cn + " at " + dn + ", closing search ctx");
		try {
			ctx.close();
		}
		catch (NamingException e) {
			cLog.log(Level.WARNING, "Unable to close ldap search context. Ignoring.", e);
		}
		cLog.info("done");
		
		// now bind as that user
		ctx = bindUser(dn, pwd);
		cLog.info("Authenticated " + dn);
			
		// finally load their atts
		Attributes sres = loadUserAtts(ctx, dn);
		cLog.info("Loaded user with atts");
		convertAttsToMap(sres, cn, atts);
		cLog.info("closing user ctx");
		try {
			ctx.close();
		}
		catch (NamingException e) {
			cLog.log(Level.WARNING, "Unable to close ldap user context. Ignoring.", e);
		}
		cLog.info("done");
		return atts;
	}

	/**
	 * Binds to ldap with the given context returning the directory context 
	 * object.
	 * 
	 * @throws IllegalArgumentException if can't bind
	 * @throws IllegalStateException if unable to set up the context.
	 * 
	 * @param srchEnv2
	 * @return
	 * @throws UnableToConnecToLdap if an NamingException occurs
	 * @throws UnableToBindSearchUser if an AuthenticationException occurs when asSearchUser is true.
	 * @throws UnableToBindEndUser if an AuthenticationException occurs when asSearchUser is false. 
	 */
	private static DirContext _bind(Hashtable<String, Object> srchEnv2, boolean asSearchUser) throws UnableToBindSearchUser, UnableToBindEndUser, UnableToConnecToLdap {
		
        DirContext srchContext = null;

        try {
        	cLog.info("call LDAP.InitialDirContext");
            srchContext = new InitialDirContext(srchEnv2);
            cLog.info("done LDAP.InitialDirContext");
        }
        catch(AuthenticationException ae) {
   			cLog.info("done LDAP.InitialDirContext");
        	cLog.log(Level.SEVERE, "Unable BIND to ldap as '" 
            		+ srchEnv2.get(Context.SECURITY_PRINCIPAL) + "'", ae);
        	if (asSearchUser) {
            	throw new UnableToBindSearchUser();
        	}
        	else {
            	throw new UnableToBindEndUser();
        	}
        }
        catch (NamingException e) {
        	cLog.info("done LDAP.InitialDirContext");
        	cLog.log(Level.SEVERE, "Unable to converse with ldap service since unable to set up ldap context.", e);
        	throw new UnableToConnecToLdap(e);
        }
        
        return srchContext;
	}
	

	/**
	 * Binds to ldap with the given context returning the directory context 
	 * object.
	 * 
	 * @throws IllegalArgumentException if can't bind
	 * @throws IllegalStateException if unable to set up the context.
	 * 
	 * @param srchEnv2
	 * @return
	 * @throws UnableToConnecToLdap if an NamingException occurs
	 * @throws UnableToBindSearchUser if an AuthenticationException occurs when asSearchUser is true.
	 * @throws UnableToBindEndUser if an AuthenticationException occurs when asSearchUser is false. 
	 */
	static DirContext bindForSearch(Hashtable<String, Object> srchEnv2) throws UnableToConnecToLdap, UnableToBindSearchUser {
		try {
			return _bind(srchEnv2, true);
		}
		catch(UnableToBindEndUser e) {
			// will never happen based upon _bind implementation
		}
		return null; // will never get here
	}
	
	/**
	 * Binds to ldap with the given context returning the directory context 
	 * object.
	 * 
	 * @throws IllegalArgumentException if can't bind
	 * @throws IllegalStateException if unable to set up the context.
	 * 
	 * @param env
	 * @return
	 * @throws UnableToConnecToLdap if an NamingException occurs
	 * @throws UnableToBindSearchUser if an AuthenticationException occurs when asSearchUser is true.
	 * @throws UnableToBindEndUser if an AuthenticationException occurs when asSearchUser is false. 
	 */
	static DirContext bindUser(String userDn, String password) throws UnableToBindEndUser, UnableToConnecToLdap {
		@SuppressWarnings("unchecked")
		Hashtable<String, Object> env = (Hashtable<String, Object>) srchEnv.clone();
		env.put(Context.SECURITY_PRINCIPAL, userDn);
		if (password != null) {
			env.put(Context.SECURITY_CREDENTIALS, password);
		}
		try {
			return _bind(env, false);
		}
		catch (UnableToBindSearchUser e) {
			// will never happen
		}
		return null; // will never happen
	}
	
	/**
	 * Finds the DN representing an ldap user entity.
	 * 
	 * @param ctx
	 * @param userCn
	 * @return user's DN
	 * @throws UnableToFindUser thrown if the user can't be found 
	 * @throws UserNotFound 
	 * @throws UnableToSearchForUser 
	 */
	static String findUserDn(DirContext ctx, String userCn) throws UserNotFound, UnableToSearchForUser {
        SearchControls srchControls = new SearchControls();

        srchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        srchControls.setCountLimit(1);

        String srchFilter = "(cn=" + userCn + ")";

        try {
    		cLog.info("call LDAP.search");
            NamingEnumeration<SearchResult> srchResponse = ctx.search((String)srchEnv.get(SEARCH_BASE_DN),
                    srchFilter, srchControls);
            cLog.info("done LDAP.search");

            if (srchResponse != null && srchResponse.hasMoreElements()) {
                return srchResponse.nextElement().getNameInNamespace();
            }
            else {
            	throw new UserNotFound();
            }
        }
        catch (NamingException e) {
        	cLog.info("done LDAP.search");
        	throw new UnableToSearchForUser(e);
        }
	}
	
	
	/**
	 * Returns for the user represented by the passed-in distinguished name, the
	 * set of {@link Attributes} specified via
	 * {@link #setUserAttributesToObtain(String[])} or if not specified all
	 * available attributes for the user.
	 * 
	 * @param ctx
	 * @param userDn
	 * @return
	 * @throws UnableToFindUser thrown if the user can't be found
	 * @throws UserNotFound
	 * @throws UnableToSearchForUser
	 * @throws UnableToGetUserAttributes
	 */
	static Attributes loadUserAtts(DirContext ctx, String userDn) throws UnableToGetUserAttributes {
		Attributes atts = null;
        try {
    		cLog.info("call LDAP.getAttributes for user");
    		@SuppressWarnings("unchecked")
			Hashtable<String, Object> env = (Hashtable<String, Object>) ctx.getEnvironment();
    		String[] attsToLoad = (String[]) env.get(ATTRIBUTES_TO_OBTAIN);
    		
    		if (attsToLoad == null) {
        		atts = ctx.getAttributes(userDn);
    		}
    		else {
        		atts = ctx.getAttributes(userDn, attsToLoad);
    		}
    		cLog.info("done LDAP.getAttributes for user [" + atts.size() + "]" );
        }
        catch (NamingException e) {
    		cLog.info("done LDAP.getAttributes for user");
        	throw new UnableToGetUserAttributes(e);
        }
        return atts;
	}
	
	/**
	 * Fills the passed-in list of attributes for the user with
	 * the attribute values found in the passed-in SearchResult.
	 * 
	 * @param atts
	 * @param userCn
	 * @return
	 * @throws UnableToLoadUserAttributes 
	 */
	static void convertAttsToMap(Attributes atts, String userCn, Map<String, List<String>> attributes) throws UnableToLoadUserAttributes {
        NamingEnumeration<? extends Attribute> attEnum = atts.getAll();
        
        try {
            while (attEnum.hasMore()) {
                Attribute att = attEnum.next();
                String name = att.getID();
                List<String> vals = attributes.get(name);
                
                if (vals == null) {
                	vals = new ArrayList<String>();
                	attributes.put(name, vals);
                }
                
            	// handle single value versus multiple values
                if (att.size() <= 1) {
                	vals.add(att.get().toString());
                }
                else {
                    for(int i=0; i<att.size(); i++) {
                        vals.add(att.get(i).toString());
                    }
                }
            }
        }
        catch (NamingException e) {
        	cLog.log(Level.SEVERE, "Unable to extract attributes for user '"
        			+ userCn + "'.", e);
        	throw new UnableToLoadUserAttributes(e);
        }
	}

	/**
	 * Passes a new environment for connecting to ldap and performs validation
	 * and tests binding.
	 * 
	 * @param searchBase
	 * @param bindDn
	 * @param bindPwd
	 * @param url
	 * @param useTlsExtension indicates that the connection should leverate the
	 * TLS extension to LDAP for the duration of connections.
	 * @param atts
	 */
	public static void setEnv(String searchBase, String bindDn, String bindPwd,
			String url, boolean useTlsExtension, String[] atts) {
		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(SEARCH_BASE_DN, searchBase);
		if (atts != null) {
			env.put(ATTRIBUTES_TO_OBTAIN, atts);
		}
		env.put(Context.SECURITY_PRINCIPAL, bindDn);
		env.put(Context.SECURITY_CREDENTIALS, bindPwd);
		env.put(Context.PROVIDER_URL, url);
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put("java.naming.ldap.version", "3");

		if (useTlsExtension) {
	        env.put(Context.SECURITY_PROTOCOL, "ssl");
		}
        
        // ensure that we trust certs from ldap server in thread safe manner
        if (! SslTrustAllOnDemandLoader.installed) {
        	cLog.log(Level.SEVERE, "SSL Trust-All Manager not installed. " 
        			+ "Must install SSL certificates for server '" 
        			+ env.get(Context.PROVIDER_URL) + "'.");
        }
		
		try {
			dumpEnv(env);
			cLog.info("Testing ldap env...");
			DirContext ctx = bindForSearch(env);

			// if everything passes then set this as our current env
			srchEnv = env;
			
			cLog.info("Successfully connected to ldap.");
			ctx.close();
		}
		catch(Exception e) {
			throw new IllegalStateException("Attempt to load ldap configuration " 
					+ "failed.", e);
		}
	}
	
	private static void dumpEnv(Hashtable<String, Object> env) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println();
		pw.println("Loaded LDAP Environment Values");
		pw.println("-----------------------------------");
		
		for (Iterator<String> itr = env.keySet().iterator(); itr.hasNext();) {
			String key = itr.next();
			if (key.endsWith(ATTRIBUTES_TO_OBTAIN)) {
				String[] atts = (String[]) env.get(key);
				if (atts == null) {
					pw.println(" " + key + " = <all>");
				}
				else {
					StringBuffer buf = new StringBuffer();
					for(String att:atts) {
						buf.append(", ").append(att);
					}
					pw.println(" " + key + " = " + buf.substring(2));
				}
			}
			else {
				String val = (String) env.get(key);

				// make sure we don't log the password in clear text
				if (key.equals(Context.SECURITY_CREDENTIALS)) {
					val = "***";
				}
				pw.println(" " + key + " = '" + val + "'");
			}
		}
		pw.flush();
		cLog.info(sw.toString());
	}
}
