package org.lds.sso.appwrap;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * Keeps track of url hits and outcomes.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class TrafficRecorder {
	private static final Logger cLog = Logger.getLogger(TrafficRecorder.class);
	
	private SortedSet<Hit> hits = new TreeSet<Hit>();
	private boolean recordTraffic = false;
	
	private int rHitCount = 0;
	private SortedSet<RestHit> rhits = new TreeSet<RestHit>();
	private boolean recordRestTraffic = false;
	
	public synchronized void recordHit(long time, String connId, String username, int respCode, boolean isProxyRes, String method, String uri) {
		if (cLog.isInfoEnabled()) {
			cLog.info(
					connId 
					+ " " + username 
					+ " " + (isProxyRes ? 'P' : '-') 
					+ " " + respCode 
					+ " " + method
					+ " " + uri);
		}
		if (recordTraffic) {
			Hit hit = new Hit(time, connId, username, respCode, method, uri, isProxyRes);
			/*
			 * The following two lines are subtle but needed. Hits implement
			 * comparable and base equality on the connection id. In the event 
			 * that an exception occurs in RequestHandler that exception
			 * catch block at the end logs the hit as a 500 using the same
			 * connection id so that if a hit for the connection was already
			 * recorded prior to the exception that hit will be replaced here
			 * with the one from the catch block. Only calling add leaves the 
			 * existing one in the set and ignores the new one being added. 
			 */
			hits.remove(hit);
			hits.add(hit);
		}
	}
	
	public synchronized void recordRestHit(String path, int code, String response, Map<String,String> props) {
		rHitCount++;
		if (cLog.isInfoEnabled()) {
			cLog.info(
					path 
					+ " " + response 
					+ " " + props);
		}
		if (recordRestTraffic) {
			RestHit rhit = new RestHit(rHitCount, path, code, response, props);
			/*
			 * The following two lines are subtle but needed. Hits implement
			 * comparable and base equality on the connection id. In the event 
			 * that an exception occurs in RequestHandler that exception
			 * catch block at the end logs the hit as a 500 using the same
			 * connection id so that if a hit for the connection was already
			 * recorded prior to the exception that hit will be replaced here
			 * with the one from the catch block. Only calling add leaves the 
			 * existing one in the set and ignores the new one being added. 
			 */
			rhits.remove(rhit);
			rhits.add(rhit);
		}
	}
	
	public Set<Hit> getHits() {
		return hits;
	}
	
	public Set<RestHit> getRestHits() {
		return rhits;
	}
	
	public static class Hit implements Comparable<Hit>{
		private int code;
		private String username;
		private String method;
		private String uri;
		private boolean isProxyCode;
		private String connId;
		private int conn = -1;
		private long time;
		
		private static final SimpleDateFormat fmtr = new SimpleDateFormat("HH:mm:ss.SSS");

		public Hit(long time, String connId, String username, int code, String method, String uri, boolean proxyResponse) {
			this.username = username;
			this.time = time;
			this.connId = connId;
			String cid = connId.substring("C-".length());
			this.conn = Integer.parseInt(cid);
			
			this.code = code;
			this.method = method;
			this.uri = uri;
			this.isProxyCode = proxyResponse;
		}
		public String getConnId() {
			return connId;
		}
		public int getCode() {
			return code;
		}
		public String getUsername() {
			return username;
		}
		public String getMethod() {
			return method;
		}
		public String getUri() {
			return uri;
		}
		public boolean isProxyCode() {
			return isProxyCode;
		}
		public boolean getIsProxyCode() {
			return isProxyCode;
		}
		public int compareTo(Hit o) {
			return Long.signum(conn - o.conn);
		}
		public String getTimestamp() {
			return fmtr.format(time);
		}
	}

	public static class RestHit implements Comparable<RestHit>{
		private String path;
		private String response;
		private Map<String, String> props;
		private long time;
		private int code;
		
		private static final SimpleDateFormat fmtr = new SimpleDateFormat("HH:mm:ss.SSS");

		public RestHit(long time, String path, int code, String response, Map<String,String> props) {
			this.path = path;
			this.code = code;
			this.response = response;
			this.props = props;
			this.time = time;
		}
		public int getCode() {
			return code;
		}
		public String getPath() {
			return path;
		}
		public String getResponse() {
			return response;
		}
		public Map<String,String> getProperties() {
			return props;
		}
		public int compareTo(RestHit o) {
			return Long.signum(time - o.time);
		}
		public String getTimestamp() {
			return fmtr.format(time);
		}
	}

	/**
	 * Indicates if recording of SSO connections is active.
	 * @return
	 */
	public boolean isRecording() {
		return recordTraffic;
	}

	/**
	 * Indicates if recording of rest calls is active.
	 * @return
	 */
	public boolean isRecordingRest() {
		return recordRestTraffic;
	}

	/**
	 * Turns on recording of SSO connections.
	 * 
	 * @param recordTraffic
	 */
	public void setRecording(boolean recordTraffic) {
		this.recordTraffic = recordTraffic;
	}

	/**
	 * Turns on recording of rest calls.
	 * 
	 * @param recordTraffic
	 */
	public void setRecordingRest(boolean recordTraffic) {
		this.recordRestTraffic = recordTraffic;
	}
}
