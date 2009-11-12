package org.lds.sso.appwrap;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Keeps track of url hits and outcomes.
 * 
 * @author Mark Boyd
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class TrafficRecorder {

	private SortedSet<Hit> hits = new TreeSet<Hit>();
	private boolean recordTraffic = false;
	private PrintWriter consoleLog;
	
	public synchronized void recordHit(long time, String connId, String username, int respCode, boolean isProxyRes, String method, String uri) {
		consoleLog.println(
				connId 
				+ " " + username 
				+ " " + (isProxyRes ? 'P' : '-') 
				+ " " + respCode 
				+ " " + method
				+ " " + uri);
		consoleLog.flush();
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
	
	public void clear() {
		hits.clear();
	}
	
	public Set<Hit> getHits() {
		return hits;
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

	public boolean isRecording() {
		return recordTraffic;
	}

	public void setRecording(boolean recordTraffic) {
		this.recordTraffic = recordTraffic;
	}

	public void setConsoleLog(PrintWriter consoleLog) {
		this.consoleLog = consoleLog;
	}
}
