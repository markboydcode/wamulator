package org.lds.sso.appwrap;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.lds.sso.appwrap.conditions.evaluator.UserHeaderNames;
import org.lds.sso.appwrap.proxy.header.Header;
import org.lds.sso.appwrap.proxy.header.HeaderBuffer;

public class User {
	public static final String LDSAPPS_ATT = "ldsApplications";
	
	protected String password = null;
	protected String username = null;
	private Map<String, String> headers = new TreeMap<String, String>(UserHeaderNames.defaultHeaders);
	Principal principal = null;
    private Set<NvPair> atts = new TreeSet<NvPair>();

	public User(String username, String pwd) {
		this.password = pwd;
		this.username = username;
        this.headers.put(UserHeaderNames.CN, username);
		this.principal = new Principal() {
			
			private String name = "sso.appwrap.user." + User.this.username;
			
			public String getName() {
				return this.name;
			}
		};
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
		this.headers.put(UserHeaderNames.CN, username);
	}

	public void addHeader(String name, String value) {
		this.headers.remove(name);
		this.headers.put(name, value);
	}

	public void injectUserHeaders(HeaderBuffer headersBfr) {
		for(Entry<String, String> e : headers.entrySet()) {
			headersBfr.append(new Header(e.getKey(), e.getValue()));
		}
	}

    public NvPair[] getHeaders() {
        return getAsNvPairArry(headers);
    }

    public NvPair[] getAttributes() {
        return atts.toArray(new NvPair[] {});
    }

    private NvPair[] getAsNvPairArry(Map<String,String> map) {
        NvPair[] arr = null;
        
        if (map.size() > 0) {
            arr = new NvPair[map.size()];
            int idx = 0;
            for(Entry<String, String> e : map.entrySet()) {
                arr[idx++] = new NvPair(e.getKey(), e.getValue());
            }
            
        }
        return arr;
    }

    public String getProperty(String name) {
        return headers.get(name);
    }

    public boolean hasAttributeValue(String name, String value) {
        return atts.contains(new NvPair(name, value));
    }

    /**
     * Returns true if an attribute exists with this name.
     * 
     * @param name
     * @return
     */
    public boolean hasAttribute(String name) {
        for(NvPair pair : atts) {
            if ((name == null && pair.getName() == null) ||
                    (name != null && name.equals(pair.getName()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an array of NvPairs all having the same name.
     * 
     * @return
     */
    public NvPair[] getAttribute(String name) {
        List<NvPair> pairs = new ArrayList<NvPair>();
        for(NvPair pair : atts) {
            if ((name == null && pair.getName() == null) ||
                    (name != null && name.equals(pair.getName()))) {
                pairs.add(pair);
            }
        }
            
        return pairs.toArray(new NvPair[] {});
    }

    public void addAttribute(String name, String value) {
        atts.add(new NvPair(name, value));
    }
    
    public String toString() {
        return "user: " + username;
    }
}
