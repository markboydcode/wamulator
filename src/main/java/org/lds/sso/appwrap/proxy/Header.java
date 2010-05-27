package org.lds.sso.appwrap.proxy;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Holder of a header value with characteristics allowing two headers though
 * different in value yet the same by name to be equal and have the same
 * hashcode to help ensure that only one is in the map at a time and is updated
 * with additional values as needed.
 * 
 * @author BOYDMR
 * 
 */
public class Header implements Comparable<Header>{

    private String value;
    private String name;
    private HeaderDef def;

    /**
     * Constructs a non-Extension Header instance.
     * 
     * @param def
     * @param value
     */
    public Header(HeaderDef def, String value) {
        if (def == null) {
            throw new IllegalArgumentException("HeaderDef can not be null.");
        }
        if (def == HeaderDef.Extension) {
            throw new IllegalArgumentException("HeaderDef can not be HeaderDef.Extension. Use other constructor for extension headers.");
        }
        this.def = def;
        this.name = "";
        this.value = value;
    }

    /**
     * Constructs a regular or Extension Header instance.
     * 
     * @param def
     * @param value
     */
    public Header(String name, String value) {
        if (name == null || "".equals(name)) {
            throw new IllegalArgumentException("Header name must be non-null and a non-empty string.");
        }
        if (value == null) {
            throw new IllegalArgumentException("Header value must be non-null.");
        }
        
        this.def = HeaderDef.getDefByName(name);
        this.name = name;
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
    /**
     * Replaces the existing value with the passed-in value.
     * @param value
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getName() {
        if (def == HeaderDef.Extension) {
            return name.toLowerCase();
        }
        return def.getName();
    }
    
    /**
     * Sort order is based upon declaration within {@link HeaderDef} except for 
     * {@link HeaderDef#Extension} which should all fall after all other 
     * {@link HeaderDef}s and sorted by lower case name.
     */
    public int compareTo(Header o) {
        // YES the following line will throw NPE if o is null and it _should_
        // according to the Comparable javadocs.
        if (o == null) {
            throw new NullPointerException();
        }
        
        if (def == o.def && def == HeaderDef.Extension) {
            // extension headers sort by ext name
            return name.compareTo(o.name);  
        }
        return def.compareTo(o.def); // sort by declaration order in headerdef
    }

    /**
     * For Headers having a non-{@link HeaderDef#Extension} type I want equality
     * to be based upon HeaderDef to identify if an instance of a Header already
     * exists while for the {@link HeaderDef#Extension} type I want the name of
     * the Header to define equality independent of case. 
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (! (obj instanceof Header)) {
            return false;
        }
        Header o = (Header) obj;
        if (def != o.def) {
            return false;
        }
        if (def == HeaderDef.Extension) {
            return name.equals(o.name);
        }
        return def.equals(o.def);
    }

    /**
     * Define hashcode to be the hashcode of its {@link HeaderDef} unless that
     * is {@link HeaderDef#Extension} in which case it should be the hashcode of
     * their name value in lower case. 
     */
    @Override
    public int hashCode() {
        if (def == HeaderDef.Extension) {
            return name.toLowerCase().hashCode();
        }
        return def.hashCode();
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        
        if (def == HeaderDef.Extension) {
            buf.append(name);
        }
        else {
            buf.append(def.getName());
        }
        
        buf.append(": ").append(value);
        return buf.toString();
    }
    
    /**
     * Writes this header to the outputstream terminated with a carriage return
     * and then linefeed character.
     * 
     * @param out
     * @throws IOException
     */
    public void writeTo(OutputStream out) throws IOException {
        out.write(toString().getBytes());
        out.write(RequestHandler.CRLF.getBytes());
    }
}
