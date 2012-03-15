package org.lds.sso.appwrap.conditions.evaluator;

import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.StartLine;

/**
 * Provides funcationality related to detecting signin and signout request 
 * signals which are query parameters that can be added to any protected URL
 * by an application to force the action.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2010, The Church of Jesus Christ of Latter Day Saints
 * 
 */
public class GlobalHeaderNames {
    /**
     * The prefix of all session attributes injected by this processor.
     */
    public static final String PREFIX = "policy-";

    /**
     * Name of the header that contains the URL pointing to the oes rest service.
     */
    public static final String SERVICE_URL = PREFIX + "service-url";

    /**
     * global properties injected by this processor.
     */
    public static final String SIGNIN = PREFIX + "signin";
    public static final String SIGNIN_VALUE = "signmein";
    public static final String SIGNOUT = PREFIX + "signout";
    public static final String SIGNOUT_VALUE = "signmeout";
    
    
    /**
     * A regex pattern and the corresponding replacement string that should be 
     * used when running the replaceall method of a matcher for the pattern 
     * against a specific String.
     *  
     * @author BOYDMR
     *
     */
    public static final class PRP {
        public Pattern pattern = null;
        public String replacement = null;
        
        public PRP(String pattern, String replacement) {
            this.pattern = Pattern.compile(pattern);
            this.replacement = replacement;
        }
    }
    
    public static final PRP[] SIGN_IN_SET = new PRP[] {
        new PRP("^(.*)\\?signmein=?$", "$1"),          // only parm
        new PRP("^(.*\\?)signmein=?&(.*)$", "$1$2"),   // first parm
        new PRP("^(.*\\?.*)&signmein=?$", "$1"),       // last parm
        new PRP("^(.*\\?.*)&signmein=?(&.*)$", "$1$2") // middle parm
    };
    
    public static final PRP[] SIGN_OUT_SET = new PRP[] {
        new PRP("^(.*)\\?signmeout=?$", "$1"),
        new PRP("^(.*\\?)signmeout=?&(.*)$", "$1$2"),
        new PRP("^(.*\\?.*)&signmeout=?$", "$1"),
        new PRP("^(.*\\?.*)&signmeout=?(&.*)$", "$1$2"),
    };
    
    /**
     * Detects signmein query parameter. Could be one of the following patterns:
     * not found, no parms: /path 
     * not found, with parms: /path?a=b&c=d
     * only parm: /path?signmein 
     * only parm: /path?signmein=
     * first of several: /path?signmein&...
     * first of several: /path?signmein=&...
     * last parm: /path?...&signmein
     * last parm: /path?...&signmein=
     * embedded: /path?...&signmein&...
     * embedded: /path?...&signmein=&...
     * 
     * @param pkg the httpPackage being checked
     * @throws MalformedURLException 
     */
    public static boolean detectedAndStrippedSignMeIn(HttpPackage pkg) throws MalformedURLException {
        return detectedAndStrippedSignal(pkg, SIGN_IN_SET);
    }
    
    /**
     * See comments for {@link #detectSignMeIn(String)}.
     * 
     * @param pkg the httpPackage being checked
     * @return
     * @throws MalformedURLException 
     */
    public static boolean detectedAndStrippedSignMeOut(HttpPackage pkg) throws MalformedURLException {
        return detectedAndStrippedSignal(pkg, SIGN_OUT_SET);
    }

    
    /**
     * See comments for {@link #detectSignMeIn(String)}.
     * 
     * @param pkg the httpPackage being checked
     * @param set the set of regex/replacement strings to use for evaluation
     * @return
     * @throws MalformedURLException 
     */
    public static boolean detectedAndStrippedSignal(HttpPackage pkg, PRP[] set) throws MalformedURLException {
        if (pkg == null || pkg.requestLine == null) {
            return false;
        }
        String raw = pkg.requestLine.getUri();
        for (PRP prp : set) {
            Matcher m = prp.pattern.matcher(raw);
            String res = m.replaceAll(prp.replacement);
            
            if (! raw.equals(res)) {
                // changed! signal detected, replace url with signal removed
                pkg.requestLine = new StartLine(pkg.requestLine.getMethod(), 
                        res, pkg.requestLine.getHttpDecl());
                return true;
            }
        }
        return false;
    }

}
