package org.lds.sso.appwrap.proxy;

import java.util.Map;

/**
 * Implements an automaton that parses the set-cookie header according to rfc2109
 * and rewrites any cookie path whose value starts with one of the keys passed
 * in via a map. The path is rewritten by replacing the matching portion of the
 * path with the value for that key from the map.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class CookiePathRewriter {

	/**
	 * The header value being parsed and rewritten.
	 */
	private String src;
	
	/**
	 * Map of path prefix keys and the values that should replace them if they 
	 * are found starting a path value. These are case sensitive.
	 */
	private Map<String, String> rewrites;
	
	/**
	 * The header buffer (hbf) holding the results of parsing and rewriting if
	 * any rewriting took place. If none took place hbf contents should be 
	 * the same as hdr.
	 */
	private StringBuffer hbf = new StringBuffer();
	
	/**
	 * Name BuFfer (nbf) for accumulating the contents of an rfc2109 attr either
	 * the name of the cookie or the name of an av-pair. See section 4.1 and 4.2.2.
	 */
	private StringBuffer nbf = new StringBuffer();
	
	/**
	 * Value BuFfer (vbf) for accumulating the contents of an rfc2109 "value"
	 * either the VALUE of the cookie or the optional value of an av-pair. 
	 * See section 4.1.
	 */
	private StringBuffer vbf = new StringBuffer();
	
	/**
	 * Single Character BuFfer (scbf) for holding the single character that 
	 * terminated the QSTR or TOKEN states.
	 */
	private StringBuffer scbf = new StringBuffer();

	/**
	 * Represents being at the starting point of reading a name-value pair both
	 * for the cookie name and value and for all av-pairs.
	 */
	private static final int START = 0; 
	/**
	 * Represents reading the name portion of a name-value pair both
	 * for the cookie name and value and for all av-pairs.
	 */
	private static final int NAME = 1;
	/**
	 * Represents reading the "=" sitting between the name and value of a 
	 * name-value pair both
	 * for the cookie name and value and for all av-pairs.
	 */
	private static final int EQLS = 2;
	/**
	 * Represents reading a token value a name-value pair both
	 * for the cookie name and value and for all av-pairs.
	 */
	private static final int TOKEN = 3;
	/**
	 * Represents reading a quoted-string value for a name-value pair both
	 * for the cookie name and value and for all av-pairs.
	 */
	private static final int QSTR = 4;
	/**
	 * Represents having just read a backslash character while in the quoted-string state.
	 */
	private static final int BSL = 5;
	/**
	 * Represents having just read a double quote character while in the
	 * quoted-string state and hence transitioning out of that state with a
	 * completed value.
	 */
	private static final int DQUT = 6;
	/**
	 * Indicates that the end of the header string has been encounted and 
	 * parsing is complete.
	 */
	private static final int END = 7;

	/**
	 * Creates an instance, parse the header, performs rewrites, and has resulting
	 * rewritten header available upon returning.
	 * 
	 * @param hdr
	 * @param rewrites
	 */
	public CookiePathRewriter(String hdr, Map<String,String>rewrites) {
		this.src = hdr;
		this.rewrites = rewrites;
		parse();
	}
	
	/**
	 * Parses according to the follow flow which looks better as a flow diagram
	 * on paper but alas can't be done here. We start in the START state. Note
	 * that this approach assumes that the cookie header passed in here has
	 * already been unfolded as outlined in rfc2616
	 * section 2.2 whose BNF definitions are incorporated into rfc2109 as the 
	 * basis of cookie format declarations. State transitions and resulting 
	 * actions are as follows:
	 * <pre>
	 * 
	 * [1]  START --> START : if char is LWS, append to hbf.
	 * [2]  START --> NAME  : if char is not LWS, append to nbf and hbf.
	 * [3]  START --> END   : if end of string (EOS)
	 * [4]  NAME  --> NAME  : if char is not "=", append to nbf and hbf.
	 * [5]  NAME  --> EQLS  : if char is "=", append to hbf.
	 * [6]  NAME  --> END   : if EOS 
	 * [7]  NAME  --> START : if ";" or ",", clear vbf, scbf, and nbf.
	 * [8]  EQLS  --> END   : if EOS
	 * [9]  EQLS  --> QSTR  : if <">, append to hbf.
	 * [10] EQLS  --> TOKEN : if not <">, append to vbf.
	 * [11] TOKEN --> TOKEN : if not "," or ";", append to vbf.
	 * [12] TOKEN --> END   : if EOS, process-path-nscbf.
	 * [13] TOKEN --> START : if "," or ";", append to scbf, process-path, clear vbf, scbf, nbf.
	 * [14] QSTR  --> QSTR  : if not "\", append to vbf.
	 * [15] QSTR  --> BSL   : if "\", append to vbf.
	 * [16] QSTR  --> END   : if EOS, append vbf to hbf.
	 * [17] QSTR  --> DQUT  : if <">, append to scbf, process-path.
	 * [18] BSL   --> QSTR  : any char, append to vbf.
	 * [19] BSL   --> END   : if EOS, append vbf to hbf
	 * [20] DQUT  --> START : if ";" or ",", append to hbf, clear vbf, scbf, and nbf.
	 * [21] DQUT  --> END   : if EOS
	 * 
	 *  
	 *  process-path : looks to see if nbf starts with any key in the map. if so
	 *    then that matching portion is replaced with the value for that key from 
	 *    the map and the result is placed in vbf. Regardless of match or not
	 *    vbf is then appended to hbf followed by scbf.
	 *    
	 *  process-path-nscbf : the same as process-path except that the scbf does
	 *    not get appended to the hbf.
	 *    
	 * </pre>   
	 */
	private void parse() {
		int state = START;
		
		for(int i=0; i<src.length(); i++) {
			char c = src.charAt(i);
			switch(state) {
			case START:
				if (c == RequestHandler.SP || c == RequestHandler.HT) {
					hbf.append(c);
				}
				else {
					nbf.append(c);
					hbf.append(c);
					state = NAME;
				}
				break;
			case NAME:
				if (c == ';' || c == ',') {
					clearBuffers();
					state = START;
				}
				else if (c == '=') {
					hbf.append(c);
					state = EQLS;
				}
				else {
					nbf.append(c);
					hbf.append(c);
				}
				break;
			case EQLS:
				if (c == '"') {
					hbf.append(c);
					state = QSTR;
				}
				else {
					vbf.append(c);
					state = TOKEN;
				}
				break;
			case TOKEN:
				if (c == ';' || c == ',') {
					scbf.append(c);
					processPath(true);
					clearBuffers();
					state = START;
				}
				else {
					vbf.append(c);
				}
				break;
			case QSTR:
				if (c == '\\') {
					vbf.append(c);
					state = BSL;
				}
			    else if (c =='"') {
					scbf.append(c);
					processPath(true);
					state = DQUT;
				}
			    else {
			    	vbf.append(c);
			    }
				break;
			case BSL:
				vbf.append(c);
				state = QSTR;
				break;
			case DQUT:
				if (c == ';' || c == ',') {
					hbf.append(c);
					clearBuffers();
					state = START;
				}
				break;
			}
		}
		// handle EOS occurring in specific states that need special handling
		switch (state) {
		case TOKEN:
			processPath(false); // see [12]
			break;
		case QSTR: // fall-through intentional: see [16] and [19]
		case BSL:
			hbf.append(vbf);
			break;
		}
	}

	/**
	 * Handles rewriting paths of they are prefixed with any configured path
	 * keys.
	 * 
	 * @param appendScbf
	 */
	private void processPath(boolean appendScbf) {
		String name = nbf.toString().toLowerCase();
		String value = vbf.toString();
		if (name.equals("path")) {
			for (Map.Entry<String, String>ent : rewrites.entrySet()) {
				if (value.startsWith(ent.getKey())) {
					value = ent.getValue() + value.substring(ent.getKey().length());
					break;
				}
			}
		}
		hbf.append(value);
		if (appendScbf) {
			hbf.append(scbf.charAt(0));
		}
	}

	/**
	 * Clears the value buffer, scbf, and name buffer.
	 */
	private void clearBuffers() {
		vbf.setLength(0);
		scbf.setLength(0);
		nbf.setLength(0);
	}
	
	public String getHeader() {
		return hbf.toString();
	}
}
