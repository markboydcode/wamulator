package org.lds.sso.appwrap.proxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the set of Headers specified in RFC2616 sections 4.5, 5.3, 6.2, and
 * 7.1. Declaration order is as is commonly seen in http traffic rather than in 
 * the order suggested in section 4.2
 * 
 * @author BOYDMR
 * 
 */
public enum HeaderDef implements Comparable<HeaderDef>{
    Host("Host"),
    UserAgent("User-Agent"),
    Accept("Accept"),
    AcceptLanguage("Accept-Language"),
    AcceptEncoding("Accept-Encoding"),
    AcceptCharset("Accept-Charset"),
    KeepAlive("Keep-Alive"),
    Referer("Referer"),

    CacheControl("Cache-Control"),
    Connection("Connection"),
    Date("Date"),
    Pragma("Pragma"),
    Trailer("Trailer"),
    TransferEncoding("Transfer-Encoding"),
    Upgrade("Upgrade"),
    Via("Via"),
    Warning("Warning"),
    
    Authorization("Authorization"),
    Expect("Expect"),
    From("From"),
    IfMatch("If-Match"),
    IfModifiedSince("If-Modified-Since"),
    IfNoneMatch("If-None-Match"),
    IfRange("If-Range"),
    IfUnmodifiedSince("If-Unmodified-Since"),
    MaxForward("Max-Forward"),
    ProxyAuthorization("Proxy-Authorization"),
    Range("Range"),
    TE("TE"),
    Cookie("Cookie"),
    
    AcceptRanges("Accept-Ranges"),
    Age("Age"),
    ETag("ETag"),
    Location("Location"),
    ProxyAuthenticate("Proxy-Authenticate"),
    RetryAfter("Retry-After"),
    Server("Server"),
    Vary("Vary"),
    WwwAuthenticate("WWW-Authenticate"),
    SetCookie("Set-Cookie"),
    
    Allow("Allow"),
    ContentEncoding("Content-Encoding"),
    ContentLanguage("Content-Language"),
    ContentLength("Content-Length"),
    ContentLocation("Content-Location"),
    ContentMD5("Content-MD5"),
    ContentRange("Content-Range"),
    ContentType("Content-Type"),
    Expires("Expires"),
    LastModified("Last-Modified"),
    
    // standard ones not defined in RFC2616
    Extension("");
    
    private String name;
    private static Map<String, HeaderDef> map;

    private HeaderDef(String name) {
        this.name = name;
        addToMap(name.toLowerCase(), this);
    }
    
    private void addToMap(String key, HeaderDef def) {
        if (HeaderDef.map == null) {
            HeaderDef.map = new HashMap<String, HeaderDef>();
        }
        HeaderDef.map.put(name.toLowerCase(), this);
    }

    /**
     * Returns the definition for the passed-in name or Extension if not defined.
     * 
     * @param name
     * @return
     */
    public static HeaderDef getDefByName(String name) {
        HeaderDef def = HeaderDef.map.get(name.toLowerCase());
        
        if (def == null) {
            def = Extension;
        }
        return def;
    }
    
    /**
     * Factory method for creating Headers with the appropriate HeaderDef.
     * 
     * @param name
     * @param value
     * @return
     */
    public static Header createHeader(String name, String value) {
        HeaderDef def = getDefByName(name);
        return new Header(name, value);
    }
    
    /**
     * Factory method for creating Headers with the appropriate HeaderDef.
     * 
     * @param name
     * @param value
     * @return
     */
    public static Header createHeader(HeaderDef def, String value) {
        if (def == HeaderDef.Extension) {
            throw new IllegalArgumentException("Extension header must be defined with a name and value and hence must use the createHeader(String, String) method.");
        }
        return new Header(def, value);
    }
    
    /**
     * Returns the header name as defined in RFC2616.
     *  
     * @return
     */
    public String getName() {
        return name;
    }
    
}
