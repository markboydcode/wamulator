package org.lds.sso.appwrap.ui;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.identity.Session;

/**
 * Object used in jsp:usebean for stuff that I can't do directly in the jsp.
 * Specify application scope since methods are thread safe and a single 
 * instance can be used by all.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class JspUtils {
	
    private BaseMapImpl<String> decoder = null;
    private BaseMapImpl<Collection<Session>> domainSessions = null;
	private BaseMapImpl<String> encoder = null;
    private BaseMapImpl<String> crlfToBr;
    private Pattern crlfPattern = null;
    private BaseMapImpl<Boolean> isSsoDefinedHdr;
	
	/**
	 * Returns a Map implementation whose get takes the passed-in
	 * url and returns it URL decoded for use in jsp pages.
	 *  
	 * @return
	 */
	public BaseMapImpl<String> getDecode() {
		if (decoder == null) {
			decoder = new BaseMapImpl<String>() {
				@Override
				public String get(Object url) {
					try {
						return URLDecoder.decode((String) url, "utf-8");
					}
					catch (UnsupportedEncodingException e) {
						// ignore since we know utf-8 is built in to jvm
					}
					return "incurredUnsupportedEncodingException"; // should never get here
				}
			};
		}
		return decoder; 
	}
	
	/**
	 * Returns a Map implementation whose get takes the passed-in
	 * url and returns it URL encoded for use in jsp pages.
	 *  
	 * @return
	 */
	public BaseMapImpl<String> getEncode() {
		if (encoder == null) {
			encoder = new BaseMapImpl<String>() {
				@Override
				public String get(Object url) {
					try {
						return URLEncoder.encode((String) url, "utf-8");
					}
					catch (UnsupportedEncodingException e) {
						// ignore since we know utf-8 is built in to jvm
					}
					return "incurredUnsupportedEncodingException"; // should never get here
				}
			};
		}
		return encoder; 
	}
    
    /**
     * Returns a Map implementation whose get takes the passed-in String
     * and returns a String with all carriage-return line-feed pairs replaced
     * by an html break "<br/>".
     *  
     * @return
     */
    public BaseMapImpl<String> getCrlfToBr() {
        if (crlfToBr == null) {
            crlfPattern = Pattern.compile("\r\n");

            crlfToBr = new BaseMapImpl<String>() {
                @Override
                public String get(Object key) {
                    String value = (String) key;
                    Matcher m = crlfPattern.matcher((String)key);
                    String resp = m.replaceAll("<br/>");
                        return resp;
                }
            };
        }
        return crlfToBr; 
    }
    
    /**
     * Returns a Map implementation whose get takes the passed-in
     * url and returns it URL decoded for use in jsp pages.
     *  
     * @return
     */
    public BaseMapImpl<Collection<Session>> getDomainSessions() {
        if (domainSessions == null) {
            domainSessions = new BaseMapImpl<Collection<Session>>() {
                @Override
                public Collection<Session> get(Object obj) {
                    String cookieDomain = (String) obj;
                    return Config.getInstance().getSessionManager().getSessions(cookieDomain);
                }
            };
        }
        return domainSessions; 
    }
}
