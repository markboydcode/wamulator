package org.lds.sso.appwrap.ui;

import org.lds.sso.appwrap.Config;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URISyntaxException;

/**
 * Object used in jsp:usebean for stuff that I can't do directly in the jsp.
 * Specify application scope since methods are thread safe and a single 
 * instance can be used by all.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class JspUtils {
	
	private BaseMapImpl<String> decoder = null;
	private BaseMapImpl<Boolean> isUnenforced = null;
	private BaseMapImpl<String> encoder = null;
	
	/**
	 * Returns a Map implementation whose get takes the passed-in
	 * url and returns it URL decoded for use in jsp pages.
	 *  
	 * @return
	 */
	public BaseMapImpl<String> getDecode() {
		if (decoder == null) {
			decoder = new BaseMapImpl<String>() {
				@Override
				public String get(Object url) {
					try {
						return URLDecoder.decode((String) url, "utf-8");
					}
					catch (UnsupportedEncodingException e) {
						// ignore since we know utf-8 is built in to jvm
					}
					return "incurredUnsupportedEncodingException"; // should never get here
				}
			};
		}
		return decoder; 
	}
	
	/**
	 * Returns a Map implementation whose get takes the passed-in
	 * url and returns it URL encoded for use in jsp pages.
	 *  
	 * @return
	 */
	public BaseMapImpl<String> getEncode() {
		if (encoder == null) {
			encoder = new BaseMapImpl<String>() {
				@Override
				public String get(Object url) {
					try {
						return URLEncoder.encode((String) url, "utf-8");
					}
					catch (UnsupportedEncodingException e) {
						// ignore since we know utf-8 is built in to jvm
					}
					return "incurredUnsupportedEncodingException"; // should never get here
				}
			};
		}
		return encoder; 
	}
	
	/**
	 * Returns a Map implementation whose get takes the passed-in
	 * url and returns a Boolean true if it is already in the unenforced urls
	 * list for use in jsp pages.
	 *  
	 * @return
	 */
	public BaseMapImpl<Boolean> getIsUnenforced() {
		if (isUnenforced == null) {
			isUnenforced = new BaseMapImpl<Boolean>() {
				@Override
				public Boolean get(Object key) {
					try {
						return Boolean.valueOf(Config.getInstance().getTrafficManager().isUnenforced((String) key));
					}
					catch (URISyntaxException e) {
						throw new RuntimeException("problem converting " + key + " to URI instance.", e);
					}
				}
			};
		}
		return isUnenforced; 
	}
	
	
}
