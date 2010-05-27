package org.lds.sso.appwrap.conditions.evaluator;

/**
 * Defines global constants used in the SSO environment.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2010, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class GlobalHeaderNames {
    /**
     * The prefix of all session attributes injected by this processor.
     */
    public static final String PREFIX = UserHeaderNames.PREFIX;
    
    /**
     * global properties injected by this processor.
     */
    public static final String SIGNIN = PREFIX + "signin";
    public static final String SIGNIN_VALUE = "signmein";
    public static final String SIGNOUT = PREFIX + "signout";
    public static final String SIGNOUT_VALUE = "signmeout";
    
    /**
     * Detects signmein query parameter. Could be one of the following patterns:
     * only parm: signmein --> equals
     * first of several: signmein&... --> startswith
     * last parm: ...&signmein --> endswith
     * embedded: ...&signmein&... --> contains
     */
    public static boolean detectSignMeIn(String q) {
        if (q == null) {
            return false;
        }
        if (q.equals(SIGNIN_VALUE) || 
                q.startsWith(SIGNIN_VALUE + "&") ||
                q.endsWith("&" + SIGNIN_VALUE) ||
                q.contains("&" + SIGNIN_VALUE + "&")) {
            return true;
        }
        return false;
    }
    
    /**
     * See comments for {@link #detectSignMeIn(String)}.
     * 
     * @param q
     * @return
     */
    public static boolean detectSignMeOut(String q) {
        if (q == null) {
            return false;
        }
        if (q.equals(SIGNOUT_VALUE) || 
                q.startsWith(SIGNOUT_VALUE + "&") ||
                q.endsWith("&" + SIGNOUT_VALUE) ||
                q.contains("&" + SIGNOUT_VALUE + "&")) {
            return true;
        }
        return false;
    }

}
