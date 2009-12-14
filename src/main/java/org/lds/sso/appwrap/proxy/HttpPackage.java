package org.lds.sso.appwrap.proxy;

import java.io.ByteArrayOutputStream;

import org.lds.sso.appwrap.SiteMatcher;

public class HttpPackage {
	public static final String COOKIE_HDR = "cookie:";
	public static final String HOST_HDR = "host:";
	public static final String CONTENT_LNG = "content-length:";
	public static final String CONN_ID_HDR = "x-connId: ";
	public static final String SHIM_HANDLED_HDR = "x-shim:";
	public static final String LOCATION_HDR = "location:";
	
	public StringBuffer headerBfr = new StringBuffer("");
	public ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
	public int responseCode = 200;
	public int contentLength = 0;
	public ResponseLine responseLine = null;
	public RequestLine requestLine = null;
	public boolean isResponse = false;
	public HttpPackageType type = null;
	public String cookiesHdr = null;
	public String hostHdr = null;
	public int port = 80;
	public boolean hasNonDefaultPort = false;
	public String host = null;
	public TrafficType trafficType = TrafficType.UNCLASSIFIED;
	public SiteMatcher site = null;
	public boolean redirectLoopDetected = false;
	public boolean rapidRepeatRequestDetected = false;
	public String repeatRequestErrMsg = null;
	public String path = null;
	public String query = null;
	public String scheme = null;
}
