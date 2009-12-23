package org.lds.sso.appwrap.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.AllowedUri;
import org.lds.sso.appwrap.AppEndPoint;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.SiteMatcher;
import org.lds.sso.appwrap.TrafficManager;
import org.lds.sso.appwrap.User;
import org.mortbay.log.Log;

public class RequestHandler implements Runnable {
	private static final Logger cLog = Logger.getLogger(RequestHandler.class);
	/**
	 * Formatter for creating a Date headers conformant to rfc2616.
	 */
	public static final SimpleDateFormat DATE_HDR_FORMATTER = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ");

	public static final String CRLF = "" + ((char) 13) + ((char) 10); // "\r\n";

	public static final String SP = " ";

	public static final String EMPTY_START_LINE = "empty-start-line";
	
	private static final String[] excludeHeaders = new String[] { "connection" };

	// public static final String REQUEST_LINE = "request-line";
	// public static final String RESPONSE_LINE = "response-line";
	public static final String HOST = "host";

	public static final String PORT = "port";

	private Socket pSocket;

	private FileOutputStream fos = null;

	// private PrintStream log = null;

	// the socketTimeout is used to time out the connection to
	// the remote server after a certain period of inactivity;
	// the value is in milliseconds -- use zero if you don't want
	// a timeout
	public static final int DEFAULT_TIMEOUT = 20 * 1000;

	// private static final String RESPONSE_CODE = "response-code";

	private int socketTimeout = DEFAULT_TIMEOUT;

	private Config cfg = null;

	private String connId;

	private boolean logClosed = false;

	/**
	 * Cache of html template pages loaded from files for some responses.
	 */
	private Map<String, String> templates = new HashMap<String, String>();

	/**
	 * Constructs a new thread for reading incoming request, sending it on to
	 * the target server, reading the response, returning it to the client, and
	 * closing the connection to enforce a single request/response cycle is
	 * handled per thread.
	 * @param s
	 * @param connId2
	 */
	public RequestHandler(Socket s, Config cfg, String connId) {
		pSocket = s;
		this.cfg = cfg;
		this.connId = connId;
	}

	public void setTimeoutSeconds(int timeout) {
		socketTimeout = timeout * 1000;
	}

	/**
	 * The main handler for a single http request/response cycle. It performs
	 * the following steps in order:
	 * 
	 * 1) opens the input and output streams of the socket connection from the
	 * client making the http request. 2) reads in the headers of the request
	 * capturing key header values and stores the headers as a unit in a
	 * StringBuffer while capturing the body content if any in a byte buffer. 3)
	 * determines if a proper cookie is had for app wrap and if not redirects to
	 * the selectUser page. 4) determines if any registered application has a
	 * root context that matches the start of the request line's request uri. If
	 * a match is identified then the request uri is rewritten for the matching
	 * portion and the replaced part is added as a query parameter with key of
	 * cctx. The matched application port then serves as the destination to
	 * which the proxy will route the request.
	 * 
	 * If a match is not found then a 404 response is served up indicating that
	 * the request uri does not match any registered application root context
	 * and can't be routed. 5) for requests routed to an application port the
	 * response is read completely if a content-length header is included or
	 * read until the server closes the connection and then returned to the
	 * client. 6) the server and client streams and sockets are closed.
	 */
	public void run() {
		PrintStream log = null;
		User user = null;
		HttpPackage reqPkg = null;
		long startTime = System.currentTimeMillis();
		try {
			if (cLog.isDebugEnabled()) {
				fos = new FileOutputStream(connId + ".log");
				log = new PrintStream(fos);
			}

			// client streams (make sure you're using streams that use
			// byte arrays, so things like GIF and JPEG files and file
			// downloads will transfer properly)
			BufferedInputStream clientIn = new BufferedInputStream(pSocket.getInputStream());
			BufferedOutputStream clientOut = new BufferedOutputStream(pSocket.getOutputStream());

			reqPkg = getHttpPackage(clientIn, excludeHeaders, false, log);
			if (reqPkg.type == HttpPackageType.EMPTY_RESPONSE) {
				byte[] bytes = getResponse("400", "Bad Request", 
						"400 Bad Request",
						"The request sent by the client was empty." + CRLF
						+ "Ensure that the site(s) is(are) configured correctly.",
					    null, reqPkg);
				sendProxyResponse(400, bytes, reqPkg, clientIn, clientOut, user, startTime,	log);
				return;
			}
			if (((StartLine) reqPkg.requestLine).isProxied) {
				reqPkg.scheme = ((StartLine) reqPkg.requestLine).proxy_scheme;
			}
			else {
				// if we ever add support for https we will need to pass the
				// appropriate scheme in here. hard code for now.
				reqPkg.scheme = "http";
			}
			determineTrafficType(reqPkg);
			if (reqPkg.redirectLoopDetected || responseThreasholdExceeded(reqPkg)) {
				/*
				 * TODO Need to add to this. If the response from a server is
				 * for a 302 and the location header is not fully qualified
				 * (which is against spec) and its path matches the path of the
				 * incoming request the we have a redirect loop. also true if
				 * location header is fully qualified and host, port, and path
				 * matches that of the incoming request. So catching these two
				 * scenarios would require placing such a check at the end of
				 * this method.
				 */
				String title = (reqPkg.rapidRepeatRequestDetected ? "500 " + reqPkg.repeatRequestErrMsg : "Infinite Redirect Detected"); 
				byte[] bytes = getResponse("500", "Infinite Redirect Detected", 
						title,
						"The proxy received a request from itself. This can happen:</br> " + CRLF
						+ "<ul><li>when a request doesn't match the configured site(s) " + CRLF
						+ "and is handled as an outbound proxy but the targeted " + CRLF
						+ "host and port resolves back to this proxy.</li>" + CRLF
						+ "<li>when the configured sign-in page is not made an unenforced " + CRLF
						+ "url and hence each subsequent request is redirected to the " + CRLF
						+ "sign-in page with an ever increasing goto query parameter.</li>" + CRLF
						+ "</ul><br/>" + CRLF
						+ "Ensure that the site(s) is(are) configured correctly.",
					    null, reqPkg);
				sendProxyResponse(500, bytes, reqPkg, clientIn, clientOut, user, startTime,
						log);
				return;
			}
			// ensure that server will close connection after completion of 
			// sending content so that servers that don't include a content-length
			// header don't cause the proxy to await the tcp-timeout expiration
			// before sending the received content back to the client.
			reqPkg.headerBfr.append("Connection: close").append(CRLF);
			
			// add header for prevention of infinite loops directly back to the proxy
			reqPkg.headerBfr.append(HttpPackage.SHIM_HANDLED_HDR).append(" handled").append(RequestHandler.CRLF);
			
			// for non-ignored traffic perform the enforcements and translations
			RequestLine appReqLn = reqPkg.requestLine; // default to request
														// line as-is
			AppEndPoint endpoint = new AppEndPoint(null, null, reqPkg.host, reqPkg.port); // default
																							// to
																							// no
																							// translation
			byte[] response = null;
			byte[] request = null;

			// we only want to manage and log site related traffic
			if (reqPkg.trafficType == TrafficType.SITE) {
				cfg.injectGlobalHeaders(reqPkg.headerBfr);
				// include our connId in request headers to coordinate app logs
				// with proxy logs if needed when troubleshooting app issues
				reqPkg.headerBfr.append(HttpPackage.CONN_ID_HDR).append(this.connId).append(CRLF);

				TrafficManager appMgr = cfg.getTrafficManager();
				String token = cfg.getTokenFromCookie(reqPkg.cookiesHdr);
				if (!cfg.getSessionManager().isValidToken(token)) {
					token = null;
				}
				String username = cfg.getUsernameFromToken(token);
				user = cfg.getUserManager().getUser(username);

				// do we have targeted site canonical context mapped to an
				// endpoint?
				endpoint = reqPkg.site.getAppEndpointForCanonicalUrl(reqPkg.requestLine.getUri());

				if (endpoint == null) {
					byte[] bytes = getResponse("404", "Not Found", 
							"404 Not Found",
						    "No registered application in a &lt;by-site&gt; declaration has a canonical context that matches the URL.",
						    null, reqPkg);
					sendProxyResponse(404, bytes, reqPkg, clientIn, clientOut, user,
							startTime, log);
					return;
				}

				// endpoint is registered, translate request Uri and set cctx
				// header
				appReqLn = endpoint.getAppRequestUri(reqPkg);
				reqPkg.headerBfr.append("cctx").append(": ").append(endpoint.getCanonicalContextRoot()).append(CRLF);

				if (!appMgr.isUnenforced(reqPkg.scheme, reqPkg.host, reqPkg.port, reqPkg.path, reqPkg.query)) {
					// so it requires enforcement

					// missing session cookie or invalid session? redirect to
					// login
					if (user == null || !cfg.getSessionManager().isValidToken(token)) {
						sendProxyResponse(302, get302RedirectToLoginPage(reqPkg), reqPkg, clientIn, clientOut, user,
								startTime, log);
						return;
					}

					// is user authorized to view?
					String path = reqPkg.requestLine.getUri();
					String query = null;

					if (!appMgr.isPermitted(reqPkg.scheme, reqPkg.host, reqPkg.port, reqPkg.requestLine.getMethod(),
							reqPkg.path, reqPkg.query, user)) {
						byte[] bytes = getResponse("403", "Forbidden", 
								"403 Forbidden",
							    "The specified action on the URI is not allowed by this user.",
							    user, reqPkg);
						sendProxyResponse(403, bytes, reqPkg, clientIn, clientOut,
								user, startTime, log);
						return;
					}
					// so it is permitted for the user, inject user headers
					user.injectUserHeaders(reqPkg.headerBfr);
					cfg.getSessionManager().markSessionAsActive(token);
				}
			}
			else if (! cfg.getAllowForwardProxying()){
				byte[] bytes = getResponse("501", "Not Allowed - Forward Proxying", 
						"501 Not Implemented - Forward Proxying",
					    "Forward Proxying is not allowed. Should this URI be mapped into the &lt;sso-traffic&gt; of the site?",
					    null, reqPkg);
				sendProxyResponse(501, bytes, reqPkg, clientIn, clientOut, user,
						startTime, log);
				return;
			}
			// now build complete request to pass on to application
			request = serializePackage((StartLine) appReqLn, reqPkg);

			Socket server = null; // socket to remote server

			try {
				if (cLog.isDebugEnabled()) {
					cLog.debug("Connecting to: " + endpoint.getHost() + ":" + endpoint.getEndpointPort());
				}
				server = new Socket(endpoint.getHost(), endpoint.getEndpointPort());
			}
			catch (Exception e) {
				// tell the client there was an error
				String errMsg = "HTTP/1.0 500" + CRLF + "Content Type: text/plain" + CRLF + HttpPackage.CONN_ID_HDR
						+ connId + CRLF + CRLF + "Error connecting to the server:" + CRLF + e + CRLF;
				sendProxyResponse(500, errMsg.getBytes(), reqPkg, clientIn, clientOut, user, startTime, log);
				return;
			}

			if (cLog.isDebugEnabled()) {
				cLog.debug("Opening I/O to: " + endpoint.getHost() + ":" + endpoint.getEndpointPort());
			}
			server.setSoTimeout(socketTimeout);
			BufferedInputStream serverIn = new BufferedInputStream(server.getInputStream());
			BufferedOutputStream serverOut = new BufferedOutputStream(server.getOutputStream());
			if (cLog.isDebugEnabled()) {
				cLog.debug("getting server input/output streams...");
			}

			// send the request out
			if (cLog.isDebugEnabled()) {
				cLog.debug("Transmitting to: " + endpoint.getHost() + ":" + endpoint.getEndpointPort() + " " + appReqLn);
			}
			serverOut.write(request, 0, request.length);
			serverOut.flush();

			// now get the response.

			// set the waitForDisconnect parameter to 'true',
			// because some servers (like Google) don't always set the
			// Content-Length header field, so we have to listen until
			// they decide to disconnect (or the connection times out).
			if (cLog.isDebugEnabled()) {
				cLog.debug("Awaiting data from: " + endpoint.getHost() + ":" + endpoint.getEndpointPort());
			}
			HttpPackage resPkg = getHttpPackage(serverIn, excludeHeaders, true, log);
			if (cLog.isDebugEnabled()) {
				cLog.debug("Processing data from: " + endpoint.getHost() + ":" + endpoint.getEndpointPort());
			}

			if (resPkg.type == HttpPackageType.EMPTY_RESPONSE) {
				byte[] bytes = getResponse("500", "No Response from Server", 
						"500 No response received from server",
						"No bytes were found in the stream from the server.",
					    null, reqPkg);
				sendProxyResponse(500, bytes, reqPkg, clientIn, clientOut, user, startTime,
						log);
				return;
			}

			if (resPkg.type == HttpPackageType.REQUEST) {
				byte[] bytes = getResponse("502", "Bad Gateway", 
						"502 Bad Gateway",
						"An invalid response was received from the server.",
					    null, reqPkg);
				log.println("---- Bad Response from server ---");
				log.write(serializePackage((StartLine) resPkg.responseLine, resPkg));
				log.println("---- End Bad Response from server ---");
				log.println();
				sendProxyResponse(502, bytes, reqPkg, clientIn, clientOut, user, startTime,
						log);
				return;
			}

			// ensure that client will not attempt another request
			resPkg.headerBfr.append("Connection: close").append(CRLF);

			// include our connId in response headers to coordinate responses
			// with proxy logs if needed when troubleshooting app issues
			resPkg.headerBfr.append("X-connId: ").append(this.connId).append(CRLF);

			response = serializePackage((StartLine) resPkg.responseLine, resPkg);
			int responseLength = Array.getLength(response);

			if (cLog.isDebugEnabled()) {
				cLog.debug("Closing I/O to: " + endpoint.getHost() + ":" + endpoint.getEndpointPort());
			}
			serverIn.close();
			serverOut.close();

			// send the response back to the client
			if (cLog.isDebugEnabled()) {
				cLog.debug("Returning response for: "  + appReqLn);
			}
			clientOut.write(response, 0, response.length);
			clientOut.flush();

			if (reqPkg.trafficType == TrafficType.SITE) {
				cfg.getTrafficRecorder().recordHit(startTime, connId, (user == null ? "???" : user.getUsername()),
						resPkg.responseCode, false, reqPkg.requestLine.getMethod(), reqPkg.requestLine.getUri());
			}
			logTraffic(log, reqPkg.requestLine, request, response, startTime);
			if (cLog.isDebugEnabled()) {
				cLog.debug("Closing I/O to client");
			}
			shutdown(clientIn, clientOut, log, fos);
		}
		catch (Exception e) {
			e.printStackTrace();
			if (log != null && logClosed == false) {
				cfg.getTrafficRecorder().recordHit(startTime, connId, (user == null ? "???" : user.getUsername()), 500,
						true, (reqPkg != null && reqPkg.requestLine != null ? reqPkg.requestLine.getMethod() : "???"),
						(reqPkg != null && reqPkg.requestLine != null ? reqPkg.requestLine.getUri() : "???"));
				log.println("\nError in RequestHandler for connection " + this.connId + ": " + e);
				e.printStackTrace(log);
				log.flush();
				log.close();
			}
		}
	}

	/**
	 * Watches to see if a replica of a previous request has occurred within too
	 * short a period and increments the number of times halting those seen more
	 * than the allowed amount in the allowed period of time since they most
	 * likely represent an infinite redirect loop. Replicas are identified by a 
	 * uri with any query string stripped off if present.
	 * 
	 * @param reqPkg
	 * @return
	 */
	public boolean responseThreasholdExceeded(HttpPackage reqPkg) {
		String reqUri = reqPkg.requestLine.getUri();
		AllowedUri uri = new AllowedUri(reqPkg.scheme, reqPkg.host, reqPkg.port, reqPkg.path, null,
				new String[] { reqPkg.requestLine.getMethod() });

		Config.RepeatRecord record = cfg.getRepeatRequestRecord(uri);
		if (record == null) {
			record = new Config.RepeatRecord();
			record.millisOfLastCall = System.currentTimeMillis();
			record.repeatCount++;
			cfg.addRepeatRequestRecord(uri, record);
		}
		else {
			record.repeatCount++;
			long elapsedSinceLastSeen = System.currentTimeMillis() - record.millisOfLastCall;

			if (elapsedSinceLastSeen < cfg.getMinimumRepeatMillis()) {
				if (record.repeatCount > cfg.getMaxRepeatCount()) {
					String msg = "Inifinite Redirect detected. Request '" + uri + "' seen " + elapsedSinceLastSeen
							+ " milliseconds since last occurrence.";
					Log.debug(msg);
					reqPkg.rapidRepeatRequestDetected = true;
					reqPkg.repeatRequestErrMsg = msg;
					return true;
				}
			}
			else { // window exceeded so reset count
				record.repeatCount = 1;
			}
			record.millisOfLastCall = System.currentTimeMillis();
		}
		return false;
	}

	private void sendProxyResponse(int respCode, byte[] response, HttpPackage reqPkg, InputStream in, OutputStream out,
			User user, long startTime, PrintStream log) throws IOException {
		String method = EMPTY_START_LINE;
		String uri = EMPTY_START_LINE;
		
		if (reqPkg.requestLine != null) {
			method = reqPkg.requestLine.getMethod();
			uri =  reqPkg.requestLine.getUri();
		}
		cfg.getTrafficRecorder().recordHit(startTime, connId, (user == null ? "???" : user.getUsername()), respCode,
				true, method, uri);
		out.write(response, 0, response.length);
		out.flush();
		byte[] request = serializePackage((StartLine) reqPkg.requestLine, reqPkg);
		logTraffic(log, reqPkg.requestLine, request, response, startTime);
		shutdown(in, out, log, fos);
	}

	/**
	 * Serializes an HttpPackage instance for sending across the socket.
	 * 
	 * @param pkg
	 * @return
	 * @throws IOException
	 */
	private byte[] serializePackage(StartLine httpStartLine, HttpPackage pkg) throws IOException {
		ByteArrayOutputStream appRespBfr = new ByteArrayOutputStream();
		String startLineContent = EMPTY_START_LINE;
		if (httpStartLine != null) {
			startLineContent = httpStartLine.toString();
		}
		appRespBfr.write(startLineContent.getBytes());
		appRespBfr.write(CRLF.getBytes());
		appRespBfr.write(pkg.headerBfr.toString().getBytes());
		appRespBfr.write(CRLF.getBytes());
		appRespBfr.write(pkg.bodyStream.toByteArray());

		return appRespBfr.toByteArray();
	}

	/**
	 * Records traffic for this connection to the connection's specific log
	 * file.
	 * 
	 * @param log
	 * @param requestLine
	 * @param request
	 * @param response
	 * @param startTime
	 */
	private void logTraffic(PrintStream log, RequestLine requestLine, byte[] request, byte[] response, long startTime) {
		if (log != null) {
			String startLn = EMPTY_START_LINE;
			if (requestLine != null) {
				startLn = requestLine.toString();
			}
			long endTime = System.currentTimeMillis();
			log.println("Elapsed Time: " + Long.toString(endTime - startTime));
			log.println();
			log.println("REQUEST bytes sent to server: " + request.length);
			log.println("Canonical Req. Line: " + startLn);
			log.println("Rewritten Req. Line: " + (new String(request)));
			log.println();
			log.println("RESPONSE bytes returned to client: " + response.length);
			log.println(new String(response));
			log.flush();
		}
	}

	private class ReqInfo {
		public TrafficType trafficType = null;

		public RequestLine reqLine = null;

		public String hostAndPort = null;

		public String host = null;
	}

	/**
	 * Determine the type of site/application traffic we are dealing with.
	 * @throws URISyntaxException
	 */
	private void determineTrafficType(HttpPackage pkg) throws URISyntaxException {
		TrafficManager appMgr = cfg.getTrafficManager();
		URI uri = new URI(pkg.requestLine.getUri());
		pkg.path = uri.getPath();
		pkg.query = uri.getQuery();
		SiteMatcher site = appMgr.getSite(pkg.scheme, pkg.host, pkg.port, pkg.path, pkg.query);

		if (site != null) {
			pkg.site = site;
			pkg.trafficType = TrafficType.SITE;
		}
	}

	private static final String HEADER_TEMPLATE = 
		"HTTP/1.1 {{http-resp-code}} {{http-resp-msg}}" + CRLF
		+ "Content-Type: text/html; charset=utf-8" + CRLF
		+ "Server: " + Config.SERVER_NAME + CRLF
		+ "Content-Length: {{content-length}}" + CRLF
		+ CRLF // empty line terminating headers
		+ "{{body}}" 
		+ CRLF; // empty line terminating body
	
	private static final String BODY_TEMPLATE = 
		"<html xmlns='http://www.w3.org/1999/xhtml'>" + CRLF
		+ "<head><title>{{title}}</title></head>" + CRLF
		+ "<body style='background-color: #EEF; margin: 0px; padding: 0px;'>" + CRLF
		+ "<div style='padding: 0 10 10 10px;'>" + CRLF
		+ "<h3>{{title}}</h3>" + CRLF
		+ "<div style='font-weight: bold; font-style: italic; color: green; padding: 12px 3px 3px 3px'>" + CRLF
		+ "{{message}}"
		+ "</div>" + CRLF
		+ "<table border='0'><tr><td>Username:</td><td>{{username}}</td></tr><tr><td>Action:</td><td>{{action}}</td></tr><tr><td>URI:</td><td>{{uri}}</td></tr></table>" + CRLF
		+ "</div>" + CRLF
		+ "</body>" + CRLF
		+ "</html>"; 

	/**
	 * Builds a dynamic response from canned templates for first the html page
	 * body and then for the http response including http response headers. 
	 * 
	 * @param httpRespCode the non-null value of the http response code.
	 * @param httpRespMsg the non-null short message of the http response line
	 * @param htmlTitle the non-null title of the html page in the browser
	 * @param htmlMsg the non-null message embedded in the html page in the browser
	 * @param user the user object if applicable; if null the "n/a" is used
	 * @param origReqLn contains the http method and http URL if applicable or uses "n/a" for both if this object is null
	 * @return
	 */
	private byte[] getResponse(String httpRespCode, String httpRespMsg, 
			String htmlTitle, String htmlMsg, User user, HttpPackage pkg) {
		String body = BODY_TEMPLATE;
		body = body.replace("{{title}}", htmlTitle);
		body = body.replace("{{message}}", htmlMsg);
		body = body.replace("{{username}}", (user == null ? "n/a" : user.getUsername()));
		body = body.replace("{{action}}", (pkg == null || pkg.requestLine == null ? "n/a" : pkg.requestLine.getMethod()));
		body = body.replace("{{uri}}", (pkg == null  || pkg.requestLine == null ? "n/a" 
				: pkg.scheme + "://" + pkg.hostHdr + pkg.requestLine.getUri()));
		
		String resp = HEADER_TEMPLATE;
		resp = resp.replace("{{http-resp-code}}", httpRespCode);
		resp = resp.replace("{{http-resp-msg}}", httpRespMsg);
		resp = resp.replace("{{content-length}}", "" + body.length());
		resp = resp.replace("{{body}}", body);
		
		return resp.getBytes();
	}

	private static final String REDIRECT_TEMPLATE = 
		"HTTP/1.1 {{http-resp-code}} {{http-resp-msg}}" + CRLF
		+ "Content-Type: text/html; charset=utf-8" + CRLF
		+ "Server: " + Config.SERVER_NAME + CRLF
		+ "Location: {{location}}" + CRLF
		+ CRLF // empty line terminating headers
		+ CRLF; // empty line terminating body
	

	private byte[] get302RedirectToLoginPage(HttpPackage pkg) throws IOException {
		String origReq = "http://" + pkg.hostHdr + pkg.requestLine.getUri();
		String origEncReq = URLEncoder.encode(origReq, "utf-8");
		String location = getLoginPageWithGotoUrl(origEncReq);

		String resp = REDIRECT_TEMPLATE;
		resp = resp.replace("{{http-resp-code}}", "302");
		resp = resp.replace("{{http-resp-msg}}", "Found");
		resp = resp.replace("{{location}}", location);
		
		return resp.getBytes();
	}

	/**
	 * Generates the login page URL with the goto parameter appended to any
	 * query string if found or as the sole query string parameter if the
	 * configured login URL does not contain a query string.
	 * 
	 * @param origEncReq
	 * @return
	 */
	private String getLoginPageWithGotoUrl(String origEncReq) {
		String cfgdLogin = cfg.getLoginPage();
		int qryIdx = cfgdLogin.indexOf("?");
		String loginUrl = null;

		if (qryIdx == -1) {
			loginUrl = cfgdLogin + "?goto=" + origEncReq;
		}
		else {
			loginUrl = cfgdLogin + "&goto=" + origEncReq;
		}
		return loginUrl;
	}

	private void shutdown(InputStream clientIn, OutputStream clientOut, PrintStream log, FileOutputStream fos)
			throws IOException {
		clientOut.close();
		clientIn.close();
		pSocket.close();

		if (log != null) {
			logClosed = true;
			log.flush();
			fos.close();
		}
	}

	/**
	 * Return a Date header conformant to rfc2616 consiting of the corresponding
	 * GMT for the current locale time.
	 * 
	 * @return
	 */
	public static String getCurrentDateHeader() {
		Date theDate = new Date();
		long theTime = theDate.getTime();
		int offMillis = DATE_HDR_FORMATTER.getTimeZone().getOffset(theTime);
		long gmtTime = theTime - offMillis;
		return DATE_HDR_FORMATTER.format(new Date(gmtTime)) + "GMT";
	}

	/**
	 * Creates an HttpPackage object filled with the date representing the
	 * stream of characters and bytes for the http request of response.
	 */
	private HttpPackage getHttpPackage(InputStream in, String[] excludeHeaders, boolean waitForDisconnect,
			PrintStream log) {
		HttpPackage pkg = new HttpPackage();

		try {
			determinePackageType(pkg, in, log);
			captureHeaders(pkg, in, excludeHeaders, log);
			if (pkg.type == HttpPackageType.EMPTY_RESPONSE
					|| (pkg.type == HttpPackageType.RESPONSE && pkg.responseCode != 200 && pkg.contentLength == 0)) {
				return pkg;
			}

			captureBody(pkg, in, log, waitForDisconnect);
		}
		catch (Exception e) {
			if (log != null) {
				log.println("\nError getting HTTP data: " + e);
			}
		}

		// flush the OutputStream and return
		try {
			pkg.bodyStream.flush();
		}
		catch (Exception e) {
		}
		return pkg;
	}

	/**
	 * Captures the bytes of the Http package's body in the passed-in package's
	 * body buffer.
	 * 
	 * @param pkg
	 * @param in
	 * @param log
	 * @param waitForDisconnect
	 */
	private void captureBody(HttpPackage pkg, InputStream in, PrintStream log, boolean waitForDisconnect) {
		// get the body, if any; we try to use the Content-Length header to
		// determine how much data we're supposed to be getting, because
		// sometimes the client/server won't disconnect after sending us
		// information...
		if (pkg.contentLength > 0) {
			waitForDisconnect = false;
		}
		else {
			if (cLog.isDebugEnabled()) {
				cLog.debug("content length = 0 for '" 
						+ (pkg.requestLine != null ? pkg.requestLine :
							(pkg.responseLine != null ? pkg.responseLine :
								"???")) + "', will wait for disconnect or tcp timeout to terminate body content.");
			}
		}
		int byteCount = 0;

		if ((pkg.contentLength > 0) || (waitForDisconnect)) {
			try {
				byte[] buf = new byte[4096];
				int bytesIn = 0;
				while (((byteCount < pkg.contentLength) || (waitForDisconnect)) && ((bytesIn = in.read(buf)) >= 0)) {
					pkg.bodyStream.write(buf, 0, bytesIn);
					byteCount += bytesIn;
				}
			}
			catch (Exception e) {
				if (log != null) {
					log.println("\nError getting HTTP body: " + e);
				}
			}
		}
		if (cLog.isDebugEnabled()) {
			cLog.debug("done with body content.");
		}
	}

	/**
	 * Reads the input stream until it sees the empty line separating headers
	 * from the body of the http package and captures values of headers of
	 * interest placing them in the passed-in package object.
	 * 
	 * TODO: At some point we should change this to not capture headers by writting
	 * them to a string buffer but capture them in a map with keys equal to the 
	 * lower case version of the header name and the value being the value minus
	 * any line termination. And order of insertion would have to be preserved in
	 * some fashion so that the headers block can be recreated as it appeared.
	 * 
	 * This would allow us to implement the proper handling of the "connection"
	 * header as specified in section 14.10 of rfc2616 whereby we parse the 
	 * values of the connection header splitting the values into connection-tokens
	 * which then represent header names which headers should be removed from 
	 * the packet by a proxy and the proxy should include its own versions if 
	 * needed to handle the connection to the targeted server.
	 * 
	 * @param pkg
	 * @param in
	 * @param excludeHeaders
	 * @param log
	 */
	private void captureHeaders(HttpPackage pkg, InputStream in, String[] excludeHeaders, PrintStream log) {
		if (pkg.type == HttpPackageType.EMPTY_RESPONSE) {
			return;
		}
		String data = "";
		String dataLC = "";
		int pos = -1;

		// get the rest of the header info
		while ((data = readLine(in, log)) != null) {
			if (data.length() == 0) {
				// the header ends at the first blank line
				break;
			}

			// filter the headers indicated via exclusion list so that they
			// don't
			// make it through the proxy
			if (excludeHeaders != null && excludeHeaders.length > 0) {
				boolean exlude = false;
				for (String xhdr : excludeHeaders) {
					int colon = data.indexOf(':');
					String hdrName = data.substring(0, colon).trim();
					if (hdrName.toLowerCase().equals(xhdr)) {
						exlude = true;
						break;
					}
				}
				if (exlude) {
					continue;
				}
			}

			dataLC = data.toLowerCase();

			if (pkg.type == HttpPackageType.REQUEST) {
				// check for the Cookie header
				pos = dataLC.indexOf(HttpPackage.COOKIE_HDR);
				if (pos >= 0) {
					pkg.cookiesHdr = data.substring(pos + HttpPackage.COOKIE_HDR.length()).trim();
				}
				// check for header from this proxy exposing an infinite
				// redirect
				pos = dataLC.indexOf(HttpPackage.SHIM_HANDLED_HDR.toLowerCase());
				if (pos >= 0) {
					pkg.redirectLoopDetected = true;
				}
				// check for host header
				pos = dataLC.indexOf(HttpPackage.HOST_HDR);
				if (pos >= 0) {
					pkg.hostHdr = data.substring(pos + HttpPackage.HOST_HDR.length()).trim();
					int colon = pkg.hostHdr.indexOf(':');
					String host = null;

					if (colon == -1) {
						pkg.host = pkg.hostHdr; // no port so is internet dflt:
												// 80
					}
					else {
						pkg.host = pkg.hostHdr.substring(0, colon);
						String sPort = pkg.hostHdr.substring(colon + 1);
						pkg.port = Integer.parseInt(sPort);
						pkg.hasNonDefaultPort = true;
					}
				}
			}

			// check for response header special handling
			pos = dataLC.indexOf(HttpPackage.CONTENT_LNG);
			if (pos >= 0) {
				pkg.contentLength = Integer.parseInt(data.substring(pos + HttpPackage.CONTENT_LNG.length()).trim());
			}
			pos = dataLC.indexOf(HttpPackage.LOCATION_HDR);
			if (pos >= 0) {
				String redirect = data.substring(pos + HttpPackage.LOCATION_HDR.length()).trim();
				TrafficManager mgr = cfg.getTrafficManager();
				String rewrite = mgr.rewriteRedirect(redirect); 
				if (rewrite != null) {
					// rewrite matched, replace
					if (cLog.isDebugEnabled()) {
						cLog.debug("rewriting redirect from: " + redirect + " to: "
								+ rewrite);
					}
					data = HttpPackage.LOCATION_HDR + " " + rewrite;
				}
			}
			// write the header to the buffer
			pkg.headerBfr.append(data + CRLF);
		}
	}

	private void determinePackageType(HttpPackage pkg, InputStream in, PrintStream log) throws MalformedURLException {
		// get the first line of the message, get the response code if a
		// response.
		String line = readLine(in, log);
		if (line == null || line.equals("")) {
			pkg.type = HttpPackageType.EMPTY_RESPONSE;
			return;
		}
		else {
			StartLine startLn = new StartLine(line);
			/*
			 * Distinguish between parsing of an http request versus http
			 * response. Ie: first line looks like:
			 * 
			 * this: "GET /opensso/UI/Login?parmAAA=111&parmBBB=222 HTTP/1.1"
			 * versus: "HTTP/1.1 200 OK"
			 */
			if (startLn.token1.toLowerCase().startsWith("http")) {
				pkg.responseLine = startLn;
				pkg.type = HttpPackageType.RESPONSE;

				try {
					pkg.responseCode = Integer.parseInt(pkg.responseLine.getRespCode());
				}
				catch (Exception e) {
					if (log != null) {
						log.println("\n>>> Error parsing response code " + pkg.responseLine.getRespCode());
					}
				}
			}
			else {
				pkg.requestLine = startLn;
				pkg.type = HttpPackageType.REQUEST;
			}
		}
	}

	private String readLine(InputStream in, PrintStream log) {
		// reads a line of text from an InputStream
		StringBuffer data = new StringBuffer("");
		int c;

		try {
			// if we have nothing to read, just return null
			in.mark(1);
			if (in.read() == -1)
				return null;
			else
				in.reset();

			while ((c = in.read()) >= 0) {
				// check for an end-of-line character
				if ((c == 0) || (c == 10) || (c == 13))
					break;
				else
					data.append((char) c);
			}

			// deal with the case where the end-of-line terminator is \r\n
			if (c == 13) {
				in.mark(1);
				if (in.read() != 10)
					in.reset();
			}
		}
		catch (Exception e) {
			if (log != null) {
				log.println("\nError getting header: " + e);
			}
		}

		// and return what we have
		return data.toString();
	}
}
