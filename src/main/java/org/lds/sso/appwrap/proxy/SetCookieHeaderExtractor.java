package org.lds.sso.appwrap.proxy;

import org.mortbay.io.Buffer;
import org.mortbay.jetty.HttpFields;

/**
 * Wraps header formatting in jetty allowing us to acquire the generated 
 * header value for the set-cookie header.
 * 
 * @author boydmr
 *
 */
public class SetCookieHeaderExtractor extends HttpFields {

	private String headerValue;

	@Override
	public void add(Buffer name, Buffer value) throws IllegalArgumentException {
		String headerName = name.toString();
    	this.headerValue = value.toString();
	}

	public String getHeaderName() {
		return this.headerValue;
	}

	public String getHeaderValue() {
		return this.headerValue;
	}
}
