package org.lds.sso.appwrap.proxy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Buffer to hold a list of {@link Header}s in the order added and provide 
 * convenience methods for different types of {@link Header}s.
 *  
 * @author BOYDMR
 *
 */
public class HeaderBuffer {

    private List<Header> headers = new ArrayList<Header>();
    
    private int find(Header h) {
        int idx = 0;
        for (Iterator<Header> itr = headers.iterator(); itr.hasNext();) {
            if (itr.next().equals(h)) {
                return idx;
            }
            idx++;
        }
        return -1;
    }
    public void append(Header h) {
        int idx = find(h);
        
        if (idx == -1) {
            headers.add(h);
        }
        else {
            Header existing = headers.get(idx);
            existing.addValue(h.getValue());
        }
    }

    public Header set(Header h) {
        int idx = find(h);
        
        if (idx == -1) {
            headers.add(h);
            return null;
        }
        else {
            Header ret = headers.remove(idx);
            headers.add(idx, h);
            return ret;
        }
    }
    
    public Header removeHeader(HeaderDef def) {
        if (def == null || def == HeaderDef.Extension) {
            return null;
        }
        Header h = new Header(def, "");
        int idx = find(h);
        
        if (idx != -1) {
            return headers.remove(idx);
        }
        return null;
    }
    
    public Header removeExtensionHeader(String name) {
        Header h = new Header(name, "");
        int idx = find(h);
        
        if (idx != -1) {
            return headers.remove(idx);
        }
        return null;
    }
    
    public Header getHeader(HeaderDef def) {
        if (def == null || def == HeaderDef.Extension) {
            return null;
        }
        Header h = new Header(def, "");
        int idx = find(h);
        
        if (idx != -1) {
            return headers.get(idx);
        }
        return null;
    }
    
    public Header getExtensionHeader(String name) {
        Header h = new Header(name, name);
        int idx = find(h);
        
        if (idx != -1) {
            return headers.get(idx);
        }
        return null;
    }
    
    public Iterator<Header> getIterator() {
        return headers.iterator();
    }
    
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.print(HeaderBuffer.class.getSimpleName());
        pw.println(" [");
        for(Iterator<Header> itr = headers.iterator(); itr.hasNext();) {
            pw.print(itr.next());
            if (itr.hasNext()) {
                pw.print(",");
            }
            pw.println();
        }
        pw.print("]");
        pw.flush();
        return sw.toString();
    }
}
