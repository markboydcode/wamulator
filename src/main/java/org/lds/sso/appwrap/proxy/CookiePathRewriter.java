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
	 * The attribute name of the expires attribute.
	 */
	private static final String EXPIRES = "Expires";

	/**
	 * The incoming, original, untouched, header value being parsed and rewritten.
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
	 * the same as src which is the raw, untouched set-cookie header value 
	 * passed in for parsing.
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
	 * The various parsing states of the automaton.
	 * 
	 * @author BoydMR
	 *
	 */
	private static enum States {
		/**
		 * Represents being at the starting point of reading a name-value pair both
		 * for the cookie name and value and for all av-pairs.
		 */
		START, 
		/**
		 * Represents reading the name portion of a name-value pair both
		 * for the cookie name and value and for all av-pairs.
		 */
		NAME,
		/**
		 * Represents reading the "=" sitting between the name and value of a 
		 * name-value pair both
		 * for the cookie name and value and for all av-pairs.
		 */
		EQLS,
		/**
		 * Represents reading a token value a name-value pair both
		 * for the cookie name and value and for all av-pairs.
		 */
		TOKEN,
		/**
		 * Represents reading a quoted-string value for a name-value pair both
		 * for the cookie name and value and for all av-pairs.
		 */
		QSTR,
		/**
		 * Represents having just read a backslash character while in the quoted-string state.
		 */
		BSL,
		/**
		 * Represents having just read a double quote character while in the
		 * quoted-string state and hence transitioning out of that state with a
		 * completed value.
		 */
		DQUT,
		/**
		 * Indicates that the end of the header string has been encounted and 
		 * parsing is complete.
		 */
		END,
		/**
		 * Indicates that we are within an Expires attribute value.
		 */
		EXPR,
		/**
		 * Indicates that we have just read the 'G' of the terminating 'GMT'
		 * sequence of the Expires header.
		 */
		EXPRG,
		/**
		 * Indicates that we have just read the 'M' of the terminating 'GMT'
		 * sequence of the Expires header.
		 */
		EXPRM,
		/**
		 * Indicates that we have just read the 'T' of the terminating 'GMT'
		 * sequence of the Expires header.
		 */
		EXPRT;
	}

	/**
	 * Creates an instance, parse the header, performs rewrites, and has resulting
	 * rewritten header available upon returning.
	 * 
	 * @param hdr the full, untouched, incoming set-cookie header 
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
	 * basis of cookie format declarations. State transitions 7.1 to 7.8 are
	 * based upon section 10.1.2 however format of the expires header in 
	 * practice appears to also have four character years. So termination of 
	 * the Expires header value is determined by watching for the characters
	 * 'GMT'. Special states for the Expires headers are required since that
	 * heder has spaces and a comma character. Hence leaving its contents 
	 * opaque to the automaton save for the terminating 'GMT' sequence.
	 * 
	 * State transitions and resulting actions are as follows:
	 * <pre>
	 * 
	 * [1]    START --> START : if char is LWS, append to hbf.
	 * [2]    START --> NAME  : if char is not LWS, append to nbf and hbf.
	 * [3]    START --> END   : if end of string (EOS)
	 * [4]    NAME  --> NAME  : if char is not "=", append to nbf and hbf.
	 * [5]    NAME  --> EQLS  : if char is "=", append to hbf.
	 * [6]    NAME  --> END   : if EOS 
	 * [7]    NAME  --> START : if ";" or ",", clear vbf, scbf, and nbf.
	 * [7.1]  NAME  --> EXPR  : if char is "=" and nbf.toLowerCase() = 'expires',
	 *                            append to hbf
	 * [7.2]  EXPR  --> EXPR  : if char is not 'G', append to hbf
	 * [7.3]  EXPR  --> END   : if EOS
	 * [7.4]  EXPR  --> EXPRG : if char is 'G', append to hbf
	 * [7.5]  EXPRG --> EXPRM : if char is 'M', append to hbf
	 * [7.6]  EXPRM --> EXPRT : if char is 'T', append to hbf
	 * [7.7]  EXPRT --> END   : if EOS
	 * [7.8]  EXPRT --> START : if ";" or ",", append to hbf, clear vbf, scbf, and nbf.
	 * [7.9]  EXPRG --> EXPR  : if char is not 'M', append to hbf
	 * [7.10] EXPRM --> EXPR  : if char is not 'T', append to hbf
	 * [7.11] EXPRT --> EXPR  : if char is not ';' and not ',', append to hbf
	 * [8]    EQLS  --> END   : if EOS
	 * [9]    EQLS  --> QSTR  : if <">, append to hbf.
	 * [10]   EQLS  --> TOKEN : if not <">, append to vbf.
	 * [11]   TOKEN --> TOKEN : if not "," or ";", append to vbf.
	 * [12]   TOKEN --> END   : if EOS, process-path-nscbf.
	 * [13]   TOKEN --> START : if "," or ";", append to scbf, process-path, clear vbf, scbf, nbf.
	 * [14]   QSTR  --> QSTR  : if not "\", append to vbf.
	 * [15]   QSTR  --> BSL   : if "\", append to vbf.
	 * [16]   QSTR  --> END   : if EOS, append vbf to hbf.
	 * [17]   QSTR  --> DQUT  : if <">, append to scbf, process-path.
	 * [18]   BSL   --> QSTR  : any char, append to vbf.
	 * [19]   BSL   --> END   : if EOS, append vbf to hbf
	 * [20]   DQUT  --> START : if ";" or ",", append to hbf, clear vbf, scbf, and nbf.
	 * [21]   DQUT  --> END   : if EOS
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
		States state = States.START;
		
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
					state = States.NAME;
				}
				break;
			case NAME:
				if (c == ';' || c == ',') {
					clearBuffers();
					state = States.START;
				}
				else if (c == '=') {
					hbf.append(c);
					if (nbf.toString().equalsIgnoreCase(EXPIRES)) { // [7.1]
						state = States.EXPR;
					}
					else { // [5]
						state = States.EQLS;
					}
				}
				else { // [4]
					nbf.append(c);
					hbf.append(c);
				}
				break;
			case EXPR:
				hbf.append(c);
				
				if (c == 'G') { // [7.4]
					state = States.EXPRG; 
				}
				else {
					// [7.2]
				}
				break;
			case EXPRG:
				hbf.append(c);
				
				if (c == 'M') { //[7.5]
					state = States.EXPRM;
				}
				else { // [7.9]
					state = States.EXPR;
				}
				break;
			case EXPRM:
				hbf.append(c);

				if (c == 'T') { // [7.6]
					state = States.EXPRT;
				}
				else { // [7.10]
					state = States.EXPR;
				}
				break;
			case EXPRT:
				hbf.append(c);

				if (c == ';' || c == ',') { // [7.8]
					clearBuffers();
					state = States.START;
				}
				else { // [7.11]
					state = States.EXPR;
				}
				break;
			case EQLS:
				if (c == '"') {
					hbf.append(c);
					state = States.QSTR;
				}
				else {
					vbf.append(c);
					state = States.TOKEN;
				}
				break;
			case TOKEN:
				if (c == ';' || c == ',') {
					scbf.append(c);
					processPath(true);
					clearBuffers();
					state = States.START;
				}
				else {
					vbf.append(c);
				}
				break;
			case QSTR:
				if (c == '\\') {
					vbf.append(c);
					state = States.BSL;
				}
			    else if (c =='"') {
					scbf.append(c);
					processPath(true);
					state = States.DQUT;
				}
			    else {
			    	vbf.append(c);
			    }
				break;
			case BSL:
				vbf.append(c);
				state = States.QSTR;
				break;
			case DQUT:
				if (c == ';' || c == ',') {
					hbf.append(c);
					clearBuffers();
					state = States.START;
				}
				break;
			}
		}
		// dropped out of character scan loop. therefore EOS was reached
		// handle EOS occurring in specific states that need special handling
		switch (state) {
		case TOKEN:
			processPath(false); // see [12]
			break;
		case QSTR: // fall-through intentional: see [16] and [19] and [7.7]
		case EXPRT:
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
