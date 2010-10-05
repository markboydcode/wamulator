package org.lds.sso.appwrap.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * A convenience template for doing GETs and POSTs.
 *
 * @author joshcummings
 *
 */
public class URLConnectionTemplate<T> {
	public final T doGet(URL url) throws IOException {
		return connect(url, Method.GET);
	}

	public final T doPost(URL url) throws IOException {
		return connect(url, Method.POST);
	}

	public final T connect(URL url, Method method) throws IOException {
		OutputStream os = null;
		InputStream is = null;
		URLConnection connection = null;
		try {
			connection = url.openConnection();
			if ( method == Method.POST ) {
				connection.setDoOutput(true);
				os = connection.getOutputStream();
				doWrite(os);
				os.flush();
			}
		} finally {
			if ( os != null ) {
				os.close();
			}
		}

		try {
			is = connection.getInputStream();
			return doRead(is);
		} finally {
			if ( is != null ) {
				is.close();
			}
		}
	}

	protected void doWrite(OutputStream os) throws IOException {}
	protected T doRead(InputStream is) throws IOException { return null; }

	public enum Method {
		GET, POST;
	}
}
