package org.lds.sso.appwrap.proxy.header;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lds.sso.appwrap.proxy.RequestHandler;

/**
 * Buffer to hold a list of {@link Header}s in the order added generally and provide 
 * convenience methods for different types of {@link Header}s. For duplicates, 
 * such duplicates will be inserted immediately following any existing ones.
 *  
 * @author BOYDMR
 *
 */
public class HeaderBuffer implements Iterable<Header>{

    private List<Header> headers = new ArrayList<Header>();
    
    /**
     * Appends the passed-in header to the list of like named headers within the
     * full list of Headers so that like named headers will be next to each
     * other.
     * 
     * @param h
     */
    public void append(Header h) {
        for(int i=headers.size()-1; i>=0; i--)
        {
            if (headers.get(i).equals(h)) {
                headers.add(i+1, h);
                return;
            }
        }
        headers.add(h);
    }

    /**
     * Replaces all instances of the passed-in header with this single one and
     * returns the List of the removed ones or null if none were found.
     * 
     * @param h
     * @return
     */
    public List<Header> set(Header h) {
        List<Header> hdrs = removeHeader(h);
        headers.add(h);
        return hdrs;
    }
    
    /**
     * Returns the list of Headers removed of the passed-in HeaderDef type or
     * null if none were found.
     * 
     * @param def
     * @return
     */
    public List<Header> removeHeader(HeaderDef def) {
        if (def == null || def == HeaderDef.Extension) {
            return null;
        }
        Header hdr = new Header(def, "");
        return removeHeader(hdr);
    }
    
    public List<Header> removeHeader(String name) {
		HeaderDef def = HeaderDef.getDefByName(name);

		if (def == HeaderDef.Extension) {
			return _removeExtensionHeader(name);
		}
		else {
			return removeHeader(def);
		}

    }
    
    private List<Header> _removeExtensionHeader(String name) {
        Header hdr = new Header(name, "");
        return removeHeader(hdr);
    }
    
    /**
     * Returns the list of Extension Headers removed having the passed-in 
     * name.
     * 
     * @param def
     * @return
     */
    public List<Header> removeExtensionHeader(String name) {
        if (name == null || HeaderDef.getDefByName(name) != HeaderDef.Extension) {
            return null;
        }
        return _removeExtensionHeader(name);
    }
    
    private List<Header> removeHeader(Header hdr){
        List<Header> hdrs = new ArrayList<Header>();
        for(Iterator<Header> itr = headers.iterator(); itr.hasNext();) {

            Header h = itr.next();
            if (hdr.equals(h)) {
                itr.remove();
                hdrs.add(h);
            }
        }
        if (hdrs.size() == 0) {
            return null;
        }
        return hdrs;
    }

    public List<Header> getExtensionHeaders(String name) {
        if(name == null || HeaderDef.getDefByName(name) != HeaderDef.Extension) {
            return null;
        }
        Header hdr = new Header(name, name);
        return getHeader(hdr);
    }

    public Header getExtensionHeader(String name) {
        if (name == null || HeaderDef.getDefByName(name) != HeaderDef.Extension) {
            return null;
        }
        Header hdr = new Header(name, name);
        List<Header> hdrs = getHeader(hdr);
        if (hdrs == null) {
            return null;
        }
        return hdrs.get(0);
    }
    
    /**
     * Return a List of Headers the are equal to the passed-in Header.
     * 
     * @param hdr
     * @return
     */
    private List<Header> getHeader(Header hdr){
        if (hdr == null) {
            return null;
        }
        List<Header> hdrs = new ArrayList<Header>();
        
        for(Header h : headers) {
            if (hdr.equals(h)) {
                hdrs.add(h);
            }
        }
        if (hdrs.size() == 0) {
            return null;
        }
        return hdrs;
    }
    
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.print(HeaderBuffer.class.getSimpleName());
        pw.print(" [" + RequestHandler.CRLF);
        for(Iterator<Header> itr = headers.iterator(); itr.hasNext();) {
            pw.print(itr.next());
            if (itr.hasNext()) {
                pw.print(",");
            }
            pw.print(RequestHandler.CRLF);
        }
        pw.print("]");
        pw.flush();
        return sw.toString();
    }
    
    /**
     * Returns a List of Header having the same HeaderDef as that passed in. 
     * If none are found then the list will be empty.
     * 
     * @param def
     * @return
     */
    public List<Header> getHeaders(HeaderDef def) {
        if (def == null || def == HeaderDef.Extension) {
            return null;
        }
        Header hdr = new Header(def, "");
        return getHeader(hdr);
    }

    /**
     * Returns the first encountered if multi-valued or the only header of the 
     * type passed in if found. Returns null otherwise.
     * 
     * @param accept
     * @return
     */
    public Header getHeader(HeaderDef def) {
        List<Header> hdrs = getHeaders(def);
        if (hdrs != null) {
            return hdrs.get(0);
        }
        return null;
    }

    public Iterator<Header> iterator() {
        return headers.iterator();
    }
}
