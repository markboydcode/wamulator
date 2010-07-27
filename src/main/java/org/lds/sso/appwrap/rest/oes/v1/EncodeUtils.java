package org.lds.sso.appwrap.rest.oes.v1;

/**
 * Created by IntelliJ IDEA.
 * User: NeweyCR
 * Date: Jul 21, 2010
 * Time: 11:25:33 AM
 */
public class EncodeUtils {
	public static String encode(String str) {
		if (null != str) {
			return str.replaceAll("[^a-zA-Z0-9\\.]","_");
		}
		return str;
	}
	public static final String HTTP_SCHEME         = "http://";
	public static final String HTTPS_SCHEME     	= "https://";
	
	public static String clean(String str) {
		if (null != str) {
			if (str.startsWith(HTTP_SCHEME)) {
				str = str.substring(HTTP_SCHEME.length());
			}
			if (str.startsWith(HTTPS_SCHEME)) {
				str = str.substring(HTTPS_SCHEME.length());
			}
			int idx = str.lastIndexOf('?');
			if (idx >= 0) {
				str = str.substring(0, idx);
			}
		}
		return str;
	}

	public static void main(String[] args) {
		// Test replacement of special characters
		System.out.println("Encoded string should be: ab12.__xyz___ and is: " + encode("ab12.\\,xyz!*&"));
		System.out.println("Encoded string should be: ab12_xyz and is: " + encode("ab12_xyz"));
		System.out.println("Encoded string should be: ab12xyz and is: " + encode("ab12xyz"));
	}
}
