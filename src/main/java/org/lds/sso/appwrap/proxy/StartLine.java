package org.lds.sso.appwrap.proxy;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Object modeling an the start line of http requests and responses.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class StartLine implements RequestLine, ResponseLine{
	private String token1 = null;
	private String token2 = null;
	private String token3 = null;
	private String rawLine = null;
	private String httpVersion = null;
	
	private String absReqUri_scheme = null;
	private String absReqUri_host = null;
	private int absReqUri_port = -1;
    private boolean absReqUriUsesDefaultPort = false;
    
	private String reqPath = null;
	private String reqQuery = null;
	private String reqFragment = null;
    
	private boolean isAbsReqURI = false;
	private boolean isBadLine = false;
	private boolean isResponseLine = false;
	private boolean isRequestLine = false;
	
    /**
	 * Constructor that parses the start line into its three tokens and then
	 * into request and response specific values as appropriate.
	 * 
	 * @param line
	 * @throws MalformedURLException 
	 */
	public StartLine(String line) throws MalformedURLException {
		String cleanLn = line.trim();
		int sIdx = cleanLn.indexOf(RequestHandler.SP);
		int eIdx = cleanLn.indexOf(RequestHandler.SP, sIdx+1);

		if (sIdx == -1 || eIdx == -1) {
		    rawLine = line;
		    token1 = line;
		    token2 = line;
		    token3 = line;
		    isBadLine = true;
		    return;
        }
		token1 = cleanLn.substring(0, sIdx);
		token2 = cleanLn.substring(sIdx + 1, eIdx);
		token3 = cleanLn.substring(eIdx + 1);
		
		interpretRequestAndResponseValues();
	}
	
	/**
	 * Determines suitable values as appropriate for request and response 
	 * start line formats.
	 * 
	 * @throws MalformedURLException
	 */
	private void interpretRequestAndResponseValues() throws MalformedURLException {
        if (token1.toLowerCase().startsWith("http")) {
            isResponseLine = true;
            httpVersion = token1.substring("http/".length());
        }
        else {
            isRequestLine = true;
            
            URL url = null;

            if (token2.startsWith("http://") || token2.startsWith("https://")) {
                isAbsReqURI = true;
                url = new URL(token2);
                absReqUri_scheme = url.getProtocol();
                absReqUri_host = url.getHost();
                absReqUri_port = (url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
                absReqUriUsesDefaultPort = (url.getPort() == -1);
            }
            else {
                isAbsReqURI = false;
                URL ctx = new URL("http://host");
                url = new URL(ctx, token2);
            }
            reqPath = url.getPath();
            reqQuery = url.getQuery();
            reqFragment = url.getRef();
            httpVersion = token3.substring("http/".length());
            /*
             * Need to revisit host this is used since we are overriding the
             * real token2 value of a start line. perhaps a better way is to
             * leave as-is but expose needed pieces and then consume those
             * where this is being used.
             */
            token2 = url.getPath() 
            + (url.getQuery() != null ? ("?" + url.getQuery()) : "")
            + (url.getRef() != null ? ("#" + url.getRef()) : "");
        }
    }

    /**
     * Constructor that sets the individual tokens directly and then evaluates
     * request and response specific interpretations as appropriate.
	 * 
	 * @param tk1
	 * @param tk2
	 * @param tk3
     * @throws MalformedURLException 
	 */
	public StartLine(String tk1, String tk2, String tk3) throws MalformedURLException {
		this.token1 = tk1;
		this.token2 = tk2;
		this.token3 = tk3;
		
		interpretRequestAndResponseValues();
	}
	
	public String toString() {
	    if (isBadLine) {
	        return rawLine;
	    }
		return token1 + RequestHandler.SP + token2 + RequestHandler.SP + token3;
	}

	// general start line methods
    public boolean isAbsReqURI() {
        return isAbsReqURI;
    }

    public boolean isBadLine() {
        return isBadLine;
    }

    public boolean isResponseLine() {
        return isResponseLine;
    }

    public boolean isRequestLine() {
        return isRequestLine;
    }

	////////////// RequestLine methods
	
    public boolean getAbsReqUriUsesDefaultPort() {
        return absReqUriUsesDefaultPort;
    }

	public String getMethod() {
		return token1;
	}

	public String getUri() {
		return token2;
	}

    public String getAbsReqUri_scheme() {
        return absReqUri_scheme;
    }

    public String getAbsReqUri_host() {
        return absReqUri_host;
    }

    public int getAbsReqUri_port() {
        return absReqUri_port;
    }

    public String getReqPath() {
        return reqPath;
    }

    public String getReqQuery() {
        return reqQuery;
    }

    public String getReqFragment() {
        return reqFragment;
    }

    ////////////// ResponseLine methods
	
	public String getMsg() {
		return token3;
	}

	public String getRespCode() {
		return token2;
	}
	
	////////////// Shared RequestLine and ResponseLine methods
    
    /**
     * Returns a request's last start line token or a response's first start
     * line token both of which have the form "http/x.y" for the version of 
     * http represented by the package. 
     */
    public String getHttpDecl() {
        if (isRequestLine) {
            return token3;
        }
        else {
            return token1;
        }
    }

    /**
     * Returns the version of http to which the request or response line 
     * package conforms. This is just the "x.y" portion of the start line's 
     * http declaration having the form "http/x.y". For Request lines this is
     * found in the start line's last token. For responses it is found in the
     * start line's first token.
     */
    public String getHttpVer() {
        return httpVersion;
    }
}
