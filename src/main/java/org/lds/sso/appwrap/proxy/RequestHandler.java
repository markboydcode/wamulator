package org.lds.sso.appwrap.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.lds.sso.appwrap.AppEndPoint;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.EndPoint;
import org.lds.sso.appwrap.LocalFileEndPoint;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.SiteMatcher;
import org.lds.sso.appwrap.TrafficManager;
import org.lds.sso.appwrap.conditions.evaluator.GlobalHeaderNames;
import org.lds.sso.appwrap.identity.Session;
import org.lds.sso.appwrap.identity.SessionManager;
import org.lds.sso.appwrap.identity.User;
import org.lds.sso.appwrap.io.LogUtils;
import org.lds.sso.appwrap.proxy.header.HandlerSet;
import org.lds.sso.appwrap.proxy.header.Header;
import org.lds.sso.appwrap.proxy.header.HeaderBuffer;
import org.lds.sso.appwrap.proxy.header.HeaderDef;
import org.lds.sso.appwrap.proxy.header.HeaderHandler;
import org.lds.sso.appwrap.proxy.header.SecondPhaseAction;
import org.lds.sso.appwrap.proxy.tls.TrustAllManager;
import org.lds.sso.appwrap.security.LocalHostOnlyEnforcingHandler;
import org.lds.sso.appwrap.ui.rest.SignInPageCdssoHandler;

public class RequestHandler implements Runnable {
    private static final Logger cLog = Logger.getLogger(RequestHandler.class.getName());

    /**
     * Formatter for creating a Date headers conformant to rfc2616.
     */
    public static final SimpleDateFormat DATE_HDR_FORMATTER = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ");

    public static final String CRLF = "" + ((char) 13) + ((char) 10); // "\r\n";

    public static final char SP = ' ';

    public static final char HT = ((char)((int)9));

    public static final String EMPTY_START_LINE = "empty-start-line";

    public static final String HOST = "host";

    public static final String PORT = "port";

    private Socket pSocket;

    private FileOutputStream fos = null;

    // private PrintStream log = null;

    private Config cfg = null;

    private String connId;

    private boolean logClosed = false;

    /**
     * Cache of html template pages loaded from files for some responses.
     */
    private Map<String, String> templates = new HashMap<String, String>();

    /**
     * Indicates whether or not the client side connection came over TLS/SSL.
     */
    private boolean isSecure;

    private SSLContext tlsContext;

    /**
     * Constructs a new thread for reading incoming request, sending it on to
     * the target server, reading the response, returning it to the client, and
     * closing the connection to enforce a single request/response cycle is
     * handled per thread.
     * @param s the Socket of the incoming connection
     * @param cfg the Config instance
     * @param connId2 the id of the connection being handled
     * @param isSecure whether or not the connection came over TLS/SSL
     */
    public RequestHandler(Socket s, Config cfg, String connId, boolean isSecure) {
        pSocket = s;
        this.cfg = cfg;
        this.connId = connId;
        this.isSecure = isSecure;

        if(cfg.isDebugLoggingEnabled()) {
            cLog.setLevel(Level.FINE);
        }
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
        BufferedInputStream clientIn = null;
        BufferedOutputStream clientOut = null;

        try {
            // client streams (make sure you're using streams that use
            // byte arrays, so things like GIF and JPEG files and file
            // downloads will transfer properly)
            clientIn = new BufferedInputStream(pSocket.getInputStream());
            clientOut = new BufferedOutputStream(pSocket.getOutputStream());


            reqPkg = getHttpPackage(true, clientIn, false, log);

            if (cLog.isLoggable(Level.FINE)) {
                fos = new FileOutputStream(Config.LOG_FILES_LOCATION + connId 
                        + Config.LOG_FILES_SUFFIX);
                log = new PrintStream(fos);
            }

            // see if we are locked down to local traffic only and enforce as needed
            if (cfg.isAllowingLocalTrafficOnly() && 
            		! LocalHostOnlyEnforcingHandler.isLocalAccess(pSocket.getInetAddress())) {
                byte[] bytes = getResponse(LocalHostOnlyEnforcingHandler.HTTP_RESPONSE_CODE, 
                		LocalHostOnlyEnforcingHandler.HTTP_RESPONSE_MSG,
                		LocalHostOnlyEnforcingHandler.HTML_TITLE,
                		LocalHostOnlyEnforcingHandler.getHtmlMessage(pSocket.getInetAddress()),
                        null, reqPkg);
                sendProxyResponse(LocalHostOnlyEnforcingHandler.INT_HTTP_RESPONSE_CODE, 
                		LocalHostOnlyEnforcingHandler.HTTP_RESPONSE_MSG,bytes, reqPkg, clientIn, clientOut, user, startTime, log, true);
                return;
            }

            if(reqPkg.socketTimeout) {
                byte[] bytes = getResponse("408", "Request Timeout",
                        "408 Request Timeout",
                        "The request content sent by the client was not received within '"
                        + cfg.getProxyInboundSoTimeout() + " milliseconds."+ CRLF
                        + "If this is insufficient time for your client, adjust the inbound "
                        + "proxy-timeout.",
                        null, reqPkg);
                sendProxyResponse(408, "Request Timeout",bytes, reqPkg, clientIn, clientOut, user, startTime, log, true);
                return;
            }

            if(reqPkg.type == HttpPackageType.EMPTY) {
                byte[] bytes = getResponse("404", "Bad Request",
                        "404 Bad Request",
                        "The request sent by the client was empty." + CRLF
                        + "Ensure that the site(s) is(are) configured correctly.",
                        null, reqPkg);
                //Lets not even try to send a 404 in this case. WAMULAT-53
                //sendProxyResponse(404, "Bad Request", bytes, reqPkg, clientIn, clientOut, user, startTime, log, true); //don't log these
                return;
            }
            
            if (reqPkg.type == HttpPackageType.BAD_STARTLINE) {
                byte[] bytes = getResponse("400", "Bad Request Line",
                        "400 Bad Request Line",
                        "An invalid request line '" + reqPkg.responseLine + 
                        "' was received from the client.",
                        null, reqPkg);
                log.println("---- Bad Request line from server ---");
                log.write(serializePackage((StartLine) reqPkg.responseLine, reqPkg));
                log.println("---- End Bad Request line from server ---");
                log.println();
                sendProxyResponse(400, "Bad Request Line",bytes, reqPkg, clientIn, clientOut, user, startTime,
                        log, true);
                return;
            }

            // HACK!!! to rewrite embedded host references during webdav tests
            //rewriteEnbeddedDomainPattern(reqPkg, "--->", "localhost.lds.org", "mail.ldschurch.org");

            // indicate in the package over which scheme the client request was received 
            if (isSecure) {
                reqPkg.scheme = "https";
            }
            else {
                reqPkg.scheme = "http";
            }

//            if (((StartLine) reqPkg.requestLine).isProxied) {
//                reqPkg.scheme = ((StartLine) reqPkg.requestLine).proxy_scheme;
//            }
//            else {
//                // if we ever add support for https we will need to pass the
//                // appropriate scheme in here. hard code for now.
//                reqPkg.scheme = "http";
//            }
            determineTrafficType(reqPkg);
            // see if we are being redirected for cross domain single sign-in
            if (reqPkg.query != null && reqPkg.query.contains(SignInPageCdssoHandler.CDSSO_PARAM_NAME + "=")) {
                byte[] bytes = get302CdssoRedirect(reqPkg, "CDSSO");
                SessionManager smgr = cfg.getSessionManager();
                smgr.addSessionViaCdsso(reqPkg.tokenForCdsso, reqPkg.host);
                String username = cfg.getUsernameFromToken(reqPkg.tokenForCdsso);
                user = cfg.getUserManager().getUser(username);
                sendProxyResponse(302, "CDSSO", bytes, reqPkg, clientIn, clientOut, user, startTime,
                        log, true);
                return;
            }
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
                sendProxyResponse(500, "Infinite Redirect Detected", bytes, reqPkg, clientIn, clientOut, user, startTime,
                        log, true);
                return;
            }
            // add header for prevention of infinite loops directly back to the proxy
            purgeAndInjectHeader(reqPkg.headerBfr, HttpPackage.SHIM_HANDLED_HDR_NM, "handled");

            // for non-ignored traffic perform the enforcements and translations
            RequestLine appReqLn = reqPkg.requestLine; // default to request line as-is
            // default to no translation
            EndPoint endpoint = new AppEndPoint(null, null, null, reqPkg.host, reqPkg.port, true, null, null);
            byte[] response = null;
            byte[] request = null;

            // we only want to manage and log site related traffic
            if (reqPkg.trafficType == TrafficType.SITE) {
//   ######   ######   #######     ######## ########  ######## ########  ######
//  ##    ## ##    ## ##     ##       ##    ##     ## ##       ##       ##    ##
//  ##       ##       ##     ##       ##    ##     ## ##       ##       ##
//   ######   ######  ##     ##       ##    ########  ######   ######   ##
//        ##       ## ##     ##       ##    ##   ##   ##       ##       ##
//  ##    ## ##    ## ##     ##       ##    ##    ##  ##       ##       ##    ##
//   ######   ######   #######        ##    ##     ## ##       ##        ######


                cfg.injectGlobalHeaders(reqPkg.headerBfr);
                // include our connId in request headers to coordinate app logs
                // with proxy logs if needed when troubleshooting app issues
                purgeAndInjectHeader(reqPkg.headerBfr, HttpPackage.CONN_ID_NM, this.connId);

                TrafficManager appMgr = cfg.getTrafficManager();
                String token = cfg.getTokenFromCookie(reqPkg.cookiesHdr);
                Session s = cfg.getSessionManager().getSessionForToken(token);
                if (s == null || ! s.isActive()) {
                    token = null;
                }
                else {
                    String username = s.getUsername();
                    user = cfg.getUserManager().getUser(username);
                }
                // check for signin/signout query parms and act accordingly
                if (reqPkg.signMeInDetected && token == null) {
                    String httpMsg = "Found, Redirecting for sign-in signal";
                    sendProxyResponse(302, httpMsg, get302RedirectToLoginPage(reqPkg, httpMsg), reqPkg, clientIn, clientOut, user,
                            startTime, log, true);
                    return;
                }
                if (reqPkg.signMeOutDetected && token != null) {
                    String httpMsg = "Found, Redirecting for sign-out signal";
                    sendProxyResponse(302, httpMsg, logoutAndGet302RedirectToSameRequest(reqPkg, token, httpMsg), reqPkg, clientIn, clientOut, user,
                            startTime, log, true);
                    return;
                }

                // do we have targeted site canonical context mapped to an
                // endpoint?
                endpoint = reqPkg.site.getEndpointForCanonicalUrl(reqPkg.requestLine.getUri());

                if (endpoint == null) {
                    byte[] bytes = getResponse("501", "cctx-mapping Not Found",
                            "501 CCTX Match Not Found in config file",
                            "There is no CCTX-MAPPING element whose CCTX attribute matches the canonical context of the URL request in the configuration file.",
                            null, reqPkg);
                    sendProxyResponse(501, "CCTX-MAPPING Not Found in config file", bytes, reqPkg, clientIn, clientOut, user,
                            startTime, log, true);
                    return;
                }

                // endpoint is registered, translate request Uri and set cctx
                // header
                purgeAndInjectHeader(reqPkg.headerBfr, "cctx", endpoint.getCanonicalContextRoot());

                // policies only set for http but cover both http and https for now
                if (!appMgr.isUnenforced("http", reqPkg.host, reqPkg.port, reqPkg.path, reqPkg.query)) {
                    // so it requires enforcement
					cLog.fine("@@$$ The path: " + reqPkg.path + " requires authentication.");

                    // missing session cookie or invalid session? redirect to
                    // login
                    if (user == null) {
                        sendProxyResponse(302, "Found, Redirecting to sign-in for URL enforcement", get302RedirectToLoginPage(reqPkg, "Resource not in Unenforced List"), reqPkg, clientIn, clientOut, user,
                                startTime, log, true);
                        return;
                    }

                    // is user authorized to view?

                    // policies only set for http but cover both http and https for now
                    if (!appMgr.isPermitted("http", reqPkg.host, reqPkg.port, reqPkg.requestLine.getMethod(),
                            reqPkg.path, reqPkg.query, user)) {
                        byte[] bytes = getResponse("403", "Forbidden",
                                "403 Forbidden",
                                "The specified action on the URI is not allowed by this user.",
                                user, reqPkg);
                        sendProxyResponse(403, "Forbidden",bytes, reqPkg, clientIn, clientOut,
                                user, startTime, log, true);
                        return;
                    }
                }
                // mark session as active
                if (user != null) {
                    cfg.getSessionManager().markSessionAsActive(token, reqPkg.host);
                }
                // now inject app specific headers, policy-service-url, host adjustments, and strip empties
                if  (endpoint instanceof AppEndPoint) {
                    AppEndPoint appEndpoint = (AppEndPoint) endpoint;
                    appEndpoint.injectHeaders(cfg, reqPkg, user, isSecure);
                    stripEmptyHeadersAsNeeded(reqPkg);
                }
            }
            else if (! cfg.getAllowForwardProxying()){
                byte[] bytes = getResponse("501", "Not Allowed - Forward Proxying",
                        "501 Not Allowed - Forward Proxying",
                        "Forward Proxying is not allowed. Either this URI is not " +
                        "mapped into the &lt;sso-traffic&gt; of the site or it is " +
                        "being accessed with a method not allowed for this URI.",
                        null, reqPkg);
                sendProxyResponse(501, "Not Allowed - Forward Proxying", bytes, reqPkg, clientIn, clientOut, user,
                        startTime, log, true);
                return;
            }

            /////////// handle via appropriate endpoint AppEndPoint or LocalFileEndPoint

            HttpPackage resPkg = null;
            String endpointMsg = null;

            if  (endpoint instanceof AppEndPoint) {
//
//     ###    ########  ########        ######## ########
//    ## ##   ##     ## ##     ##       ##       ##     ##
//   ##   ##  ##     ## ##     ##       ##       ##     ##
//  ##     ## ########  ########        ######   ########
//  ######### ##        ##              ##       ##
//  ##     ## ##        ##              ##       ##
//  ##     ## ##        ##              ######## ##
//
                AppEndPoint appEndpoint = (AppEndPoint) endpoint;
                appReqLn = appEndpoint.getAppRequestUri(reqPkg);
                request = serializePackage((StartLine) appReqLn, reqPkg);

                Socket server = null; // socket to remote server
                String serverScheme = "http";
                
                try {
                    if (appEndpoint.useHttpsScheme()) {
                        endpointMsg = "Connecting via TLS to: " + appEndpoint.getHost() + ":" + appEndpoint.getEndpointPort();
                        LogUtils.fine(cLog, endpointMsg);
                        SocketFactory fact = getTlsSocketFactory();
                        server = fact.createSocket(appEndpoint.getHost(), 
                                appEndpoint.getEndpointPort());
                        serverScheme = "https";
                    }
                    else {
                        endpointMsg = "Connecting to: " + appEndpoint.getHost() + ":" + appEndpoint.getEndpointPort();
                        LogUtils.fine(cLog, endpointMsg);
                        server = new Socket(appEndpoint.getHost(), appEndpoint.getEndpointPort());
                    }
                }
                catch (Exception e) {
                    // tell the client there was an error
                    String errMsg = "HTTP/1.0 502 Bad Gateway" + CRLF + "Content Type: text/plain" + CRLF + HttpPackage.CONN_ID_HDR
                            + connId + CRLF + CRLF + "Error connecting to the server:" + CRLF + e + CRLF;
                    sendProxyResponse(502, "Bad Gateway", errMsg.getBytes(), reqPkg, clientIn, clientOut, user, startTime, log, true);
                    return;
                }

                LogUtils.fine(cLog, "Opening I/O to: {0}:{1}", appEndpoint.getHost(), appEndpoint.getEndpointPort());
                server.setSoTimeout(cfg.getProxyOutboundSoTimeout());
                BufferedInputStream serverIn = new BufferedInputStream(server.getInputStream());
                BufferedOutputStream serverOut = new BufferedOutputStream(server.getOutputStream());
                LogUtils.fine(cLog, "getting server input/output streams...");

                // send the request out
                LogUtils.fine(cLog, "Transmitting to: {0}:{1} {2}", appEndpoint.getHost(), appEndpoint.getEndpointPort(), appReqLn);
                serverOut.write(request, 0, request.length);
                serverOut.flush();

                // now get the response.

                // set the waitForDisconnect parameter to 'true',
                // because some servers (like Google) don't always set the
                // Content-Length header field, so we have to listen until
                // they decide to disconnect (or the connection times out).
                LogUtils.fine(cLog, "Awaiting data from: {0}:{1}", appEndpoint.getHost(), appEndpoint.getEndpointPort());
                resPkg = getHttpPackage(false, serverIn, true, log);
                resPkg.scheme = serverScheme;

                if (resPkg.socketTimeout) {
                    byte[] bytes = getResponse("504", "Gateway Timeout",
                            "504 Gateway Timeout",
                            "A SocketTimeoutException occurred while reading from the server.",
                            null, reqPkg);
                    log.println("---- SocketTimeoutException for server ---");
                    log.write(serializePackage((StartLine) resPkg.responseLine, resPkg));
                    log.println("---- End SocketTimeoutException for server ---");
                    log.println();
                    sendProxyResponse(504, "Gateway Timeout", bytes, reqPkg, clientIn, clientOut, user, startTime,
                            log, true);
                    return;
                }
                LogUtils.fine(cLog, "Processing data from: {0}:{1}", appEndpoint.getHost(), appEndpoint.getEndpointPort());

                if (resPkg.type == HttpPackageType.EMPTY) {
                    byte[] bytes = getResponse("502", "Bad Gateway Response from Server",
                            "502 Bad Gateway response received from server",
                            "No bytes were found in the stream from the server.",
                            null, reqPkg);
                    sendProxyResponse(502, "Bad Gateway Response from Server", bytes, reqPkg, clientIn, clientOut, user, startTime,
                            log, true);
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
                    sendProxyResponse(502, "Bad Gateway",bytes, reqPkg, clientIn, clientOut, user, startTime,
                            log, true);
                    return;
                }

                if (resPkg.type == HttpPackageType.BAD_STARTLINE) {
                    byte[] bytes = getResponse("502", "Bad Gateway",
                            "502 Bad Gateway",
                            "An invalid response line '" + resPkg.responseLine + 
                            "' was received from the server.",
                            null, reqPkg);
                    log.println("---- Bad Response line from server ---");
                    log.write(serializePackage((StartLine) resPkg.responseLine, resPkg));
                    log.println("---- End Bad Response line from server ---");
                    log.println();
                    sendProxyResponse(502, "Bad Gateway",bytes, reqPkg, clientIn, clientOut, user, startTime,
                            log, true);
                    return;
                }
                /////// HACK to rewrite embedded host references during webdav tests
                //rewriteEnbeddedDomainPattern(resPkg, "<---", "mail.ldschurch.org", "localhost.lds.org");
//                List<Header> removed = resPkg.headerBfr.removeHeader(HeaderDef.TransferEncoding); 
//                if(removed.size() > 0) {
//                    for(Header h : removed) {
//                        System.out.println("<--- removed header: " + h);
//                    }
//                }
                LogUtils.fine(cLog, "Closing I/O to: {0}:{1}", appEndpoint.getHost(), appEndpoint.getEndpointPort());
                serverIn.close();
                serverOut.close();
            }
            else { // file endpoint
//
//    ######## #### ##       ########       ######## ########
//    ##        ##  ##       ##             ##       ##     ##
//    ##        ##  ##       ##             ##       ##     ##
//    ######    ##  ##       ######         ######   ########
//    ##        ##  ##       ##             ##       ##
//    ##        ##  ##       ##             ##       ##
//    ##       #### ######## ########       ######## ##
//
                LocalFileEndPoint fileEp = (LocalFileEndPoint) endpoint;
                resPkg = getFileHttpPackage(reqPkg, fileEp, log);
                endpointMsg = fileEp.servedFromMsg;
                request = serializePackage((StartLine) resPkg.responseLine, reqPkg);
            }
//
// ########  ########  ######  ########   #######  ##    ##  ######  ########
// ##     ## ##       ##    ## ##     ## ##     ## ###   ## ##    ## ##
// ##     ## ##       ##       ##     ## ##     ## ####  ## ##       ##
// ########  ######    ######  ########  ##     ## ## ## ##  ######  ######
// ##   ##   ##             ## ##        ##     ## ##  ####       ## ##
// ##    ##  ##       ##    ## ##        ##     ## ##   ### ##    ## ##
// ##     ## ########  ######  ##         #######  ##    ##  ######  ########
//
            // include our connId in response headers to coordinate responses
            // with proxy logs if needed when troubleshooting app issues
            resPkg.headerBfr.append(new Header(HttpPackage.CONN_ID_NM, this.connId));

            response = serializePackage((StartLine) resPkg.responseLine, resPkg);
            int responseLength = Array.getLength(response);

            // send the response back to the client
            LogUtils.fine(cLog, "Returning response for: {0}", appReqLn);

            clientOut.write(response, 0, response.length);
            clientOut.flush();

            // record all traffic regardless of whether forward or reverse (sso) proxy traffic
            cfg.getTrafficRecorder().recordHit(startTime, connId, (user == null ? "???" : user.getUsername()),
                    resPkg.responseCode, 
                    (resPkg.responseLine == null ? "null response line" : resPkg.responseLine.getMsg()), 
                    false, reqPkg.trafficType, 
                    reqPkg.requestLine.getMethod(), reqPkg.hostHdr, reqPkg.requestLine.getUri(), 
                    (reqPkg.scheme != null && reqPkg.scheme.equals("https")),
                    (resPkg.scheme != null && resPkg.scheme.equals("https")));
            logTraffic(log, reqPkg.requestLine, request, response, startTime, endpointMsg, reqPkg);
        }
        catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            e.printStackTrace(ps);
            ps.flush();
            String stack = baos.toString().replace(CRLF, "<br/>");
            byte[] bytes = getResponse("500", "Error in RequestHandler",
                    "500 Error in Simulator's RequestHandler", stack, null,
                    reqPkg);
            try {
                sendProxyResponse(500, "Error in RequestHandler", bytes,
                        reqPkg, clientIn, clientOut, user, startTime, log, true);
            } catch (IOException e2) {
                // things are looking bad for our hero
            	LogUtils.severe(cLog, "Exception occurred in RequestHandler and response could not be generated. Original Exception:", e);
            	LogUtils.severe(cLog, "Exception occurred while generating exception response:", e2);
            }
        } finally {
            LogUtils.fine(cLog, "Closing I/O to client");
            shutdown(clientIn, clientOut, log, fos);
        }
    }

    /**
     * Purge any header prior to injecting it. 
     * 
     * @param headerBfr
     * @param name
     * @param value
     */
    private void purgeAndInjectHeader(HeaderBuffer headerBfr, String name, String value) {
        headerBfr.removeHeader(HttpPackage.SHIM_HANDLED_HDR_NM);
        headerBfr.append(new Header(HttpPackage.SHIM_HANDLED_HDR_NM, "handled"));
	}

	/**
     * Hack for scanning body content in both directions for a domain and 
     * replacing it with another during testing of webdav proxying. Kept here 
     * for future reference but not currently used. Needs more work if we want
     * to enable a general content rewriter mechanism and support it via config
     * directives.
     * 
     * @param pkg
     * @param msgPrfx
     * @param oldPtrn
     * @param newPtrn
     * @throws IOException
     */
    private void rewriteEnbeddedDomainPattern(HttpPackage pkg, String msgPrfx, String oldPtrn, String newPtrn) throws IOException {
        Header hdr = pkg.headerBfr.getHeader(HeaderDef.ContentType);
        if (hdr != null && hdr.getValue().contains("text/xml")
                && pkg.bodyStream != null
                && pkg.bodyStream.size() > oldPtrn.length()) {
            System.out.println(msgPrfx + " Scanning HTML to replace host.");
            byte[] bytes = pkg.bodyStream.toByteArray();
            int end = bytes.length - oldPtrn.length();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] repl = newPtrn.getBytes();

            for (int i = 0; i < end; i++) {
                String chunk = new String(bytes, i,
                        oldPtrn.length());
                if (chunk.equals(oldPtrn)) {
                    System.out.println(msgPrfx + " replaced ref at " + i);
                    baos.write(repl);
                    i = i + oldPtrn.length() - 1;
                } else {
                    baos.write(bytes[i]);
                }
            }
            baos.write(bytes, end, oldPtrn.length());
            pkg.bodyStream = baos;
            pkg.headerBfr.getHeader(HeaderDef.ContentLength);
            pkg.headerBfr.removeHeader(HeaderDef.ContentLength);
            pkg.headerBfr.append(new Header(HeaderDef.ContentLength, "" + baos.size()));
            System.out.println(msgPrfx + " rewrote from: " + new String(bytes));
            System.out.println(msgPrfx + " rewrote   to: " + new String(baos.toByteArray()));
        }
    }

/////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////

    /**
     * Sets up our SSLContext if not in place already and gets a corresponding
     * SocketFactory. 
     * @throws NoSuchAlgorithmException 
     * @throws KeyManagementException 
     */
    private SocketFactory getTlsSocketFactory()
            throws NoSuchAlgorithmException, KeyManagementException {
        if (this.tlsContext == null) {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[] { new TrustAllManager() }, null);
            this.tlsContext = ctx;
        }
        return tlsContext.getSocketFactory();
    }

    /**
     * Removes any empty headers in SSO traffic requests if configured to do so.
     *
     * @param reqPkg
     */
    private void stripEmptyHeadersAsNeeded(HttpPackage reqPkg) {
        if (cfg.getStripEmptyHeaders()) {
            StringBuffer removed = new StringBuffer();
            for(Iterator<Header> itr = reqPkg.headerBfr.iterator(); itr.hasNext();) {
                Header hdr = itr.next();
                if ("".equals(hdr.getValue())) {
                    removed.append(",").append(hdr.getName());
                    itr.remove();
                }
            }
            if (removed.length() > 1) {
                // take off leader comma
                String list = removed.substring(1).toString();
                purgeAndInjectHeader(reqPkg.headerBfr, HttpPackage.SHIM_STRIPPED_HEADERS, list);
            }
        }
    }

    private byte[] logoutAndGet302RedirectToSameRequest(HttpPackage reqPkg,
            String token, String httpMsg) throws IOException {
        String origReq = reqPkg.scheme+"://" + reqPkg.hostHdr + reqPkg.requestLine.getUri();

        String domain = cfg.getSessionManager().getCookieDomainForHost(reqPkg.host);
        cfg.getSessionManager().terminateSession(token, domain);
        // deleting current session so clear out cookie
        String resp = REDIRECT_CLEARING_SESSION_TEMPLATE;
        resp = resp.replace("{{http-resp-code}}", "302");
        resp = resp.replace("{{http-resp-msg}}", httpMsg);
        resp = resp.replace("{{location}}", origReq);
        resp = resp.replace("{{cookie-domain}}", domain);
        return resp.getBytes();
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
// #CRN# I'm commenting this out so that Gerardo can get around his false infinite-redirect problem.
// #CRN# This will be in a version of the simulator that no one but Gerardo will get.
//        String reqUri = reqPkg.requestLine.getUri();
//        AllowedUri uri = new AllowedUri(reqPkg.scheme, reqPkg.host, reqPkg.port, reqPkg.path, reqPkg.query,
//                new String[] { reqPkg.requestLine.getMethod() });
//
//        Config.RepeatRecord record = cfg.getRepeatRequestRecord(uri);
//        if (record == null) {
//            record = new Config.RepeatRecord();
//            record.millisOfLastCall = System.currentTimeMillis();
//            record.repeatCount++;
//            cfg.addRepeatRequestRecord(uri, record);
//        }
//        else {
//            record.repeatCount++;
//            long elapsedSinceLastSeen = System.currentTimeMillis() - record.millisOfLastCall;
//
//            if (elapsedSinceLastSeen < cfg.getMinimumRepeatMillis()) {
//                if (record.repeatCount > cfg.getMaxRepeatCount()) {
//                    String msg = "Inifinite Redirect detected. Request '" + uri + "' seen " + elapsedSinceLastSeen
//                            + " milliseconds since last occurrence.";
//                    Log.debug(msg);
//                    reqPkg.rapidRepeatRequestDetected = true;
//                    reqPkg.repeatRequestErrMsg = msg;
//                    return true;
//                }
//            }
//            else { // window exceeded so reset count
//                record.repeatCount = 1;
//            }
//            record.millisOfLastCall = System.currentTimeMillis();
//        }
        return false;
    }

    /**
     * Sends a proxy generated response to the http client.
     *
     * @param respCode the http response code, ie: the 500 in "HTTP1.1 500 Internal Server Error"
     * @param respMsg the http response message that is part of the http start line ie: the text after the 500 in "HTTP1.1 500 Internal Server Error"
     * @param response the html payload of the http response
     * @param reqPkg the package representing the parsed original request
     * @param in the client input stream
     * @param out the client output stream
     * @param user the current user as identified by the cookie of the incoming request or null
     * @param startTime the time at which the request was received
     * @param log the request specific log if logging is enabled
     * @param recordTraffic indicates if console recording of traffic is enabled
     * @throws IOException
     */
	private void sendProxyResponse(int respCode, String respMsg,
			byte[] response, HttpPackage reqPkg, InputStream in,
			OutputStream out, User user, long startTime, PrintStream log,
			boolean recordTraffic) throws IOException {
		try {
			String method = EMPTY_START_LINE;
			String uri = EMPTY_START_LINE;

			if (reqPkg.requestLine != null) {
				method = reqPkg.requestLine.getMethod();
				uri = reqPkg.requestLine.getUri();
			}
			if (recordTraffic) {
				cfg.getTrafficRecorder()
						.recordHit(
								startTime,
								connId,
								(user == null ? "???" : user.getUsername()),
								respCode,
								respMsg,
								true,
								reqPkg.trafficType,
								method,
								reqPkg.hostHdr,
								(reqPkg.signMeInDetected
										|| reqPkg.signMeOutDetected ? reqPkg.origRequestList
										.getUri() : uri),
								(reqPkg.scheme != null && reqPkg.scheme.equals("https")),
								false);
			}
			out.write(response, 0, response.length);
			out.flush();
			byte[] request = serializePackage((StartLine) reqPkg.requestLine,
					reqPkg);
			if (log != null) {
				logTraffic(log, reqPkg.requestLine, request, response, startTime,
						"Proxy Generated Response.", reqPkg);
			}
		} finally {
			shutdown(in, out, log, fos);
		}
	}

    /**
     * Serializes an HttpPackage instance for sending across the socket.
     *
     * @param pkg
     * @return
     * @throws IOException
     */
    protected byte[] serializePackage(StartLine httpStartLine, HttpPackage pkg) throws IOException {
        ByteArrayOutputStream appRespBfr = new ByteArrayOutputStream();
        String startLineContent = EMPTY_START_LINE;
        if (httpStartLine != null) {
            startLineContent = httpStartLine.toString();
        }
        appRespBfr.write(startLineContent.getBytes());
        appRespBfr.write(CRLF.getBytes());

        for (Header hdr : pkg.headerBfr) {
            hdr.writeTo(appRespBfr);
        }
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
     * @param endpointMsg
     */
    private void logTraffic(PrintStream log, RequestLine requestLine, byte[] request, byte[] response, long startTime, String endpointMsg, HttpPackage reqPkg) {
        if (log != null) {
            String startLn = EMPTY_START_LINE;
            if (requestLine != null) {
                startLn = requestLine.toString();
            }
            long endTime = System.currentTimeMillis();
            log.println("Elapsed Time (ms): " + Long.toString(endTime - startTime));
            log.println("REQUEST Bytes to SERVER: " + request.length);
            log.println();
            if (reqPkg.signMeInDetected || reqPkg.signMeOutDetected) {
                log.println("Pre-SISOC Req. Line: " + reqPkg.origRequestList);
            }
            if (reqPkg.trafficType == TrafficType.SITE) {
                log.println("Canonical Req. Line: " + startLn);
                log.println("Rewritten Req. Line: " + (new String(request)));
            }
            else {
                log.println(new String(request));
            }
            log.println();

            // allow for logging requests that caused an exception
            if (endpointMsg != null && response != null) {
                log.println(endpointMsg);
                log.println();
                log.println("RESPONSE Bytes to CLIENT: " + response.length);
                log.println(new String(response));
            }
            log.flush();
        }
    }

    /**
     * Determine the type of site/application traffic we are dealing with. That
     * means seeing if we have a match on the targeted host and optional port
     * with a host definition in the configuration. 
     * 
     * @throws URISyntaxException
     */
    private void determineTrafficType(HttpPackage pkg) throws URISyntaxException {
        TrafficManager appMgr = cfg.getTrafficManager();
        StartLine sl = (StartLine)pkg.requestLine;
        
        SiteMatcher site = appMgr.getSite(pkg.host, pkg.port);

        if (site != null) {
            pkg.site = site;
            pkg.trafficType = TrafficType.SITE;
        }
        else {
            pkg.trafficType = TrafficType.NOT_SITE;
        }
    }

    private static final String HEADER_TEMPLATE =
        "HTTP/1.1 {{http-resp-code}} {{http-resp-msg}}" + CRLF
        + "Content-Type: text/html; charset=utf-8" + CRLF
        + "Server: " + Config.serverName() + CRLF
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
        + "<div style='font-weight: bold; color: green; padding: 12px 3px 3px 3px'>" + CRLF
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
        String body = getResponseBody(htmlTitle, htmlMsg, user, pkg);

        String resp = HEADER_TEMPLATE;
        resp = resp.replace("{{http-resp-code}}", httpRespCode);
        resp = resp.replace("{{http-resp-msg}}", httpRespMsg);
        resp = resp.replace("{{content-length}}", "" + body.length());
        resp = resp.replace("{{body}}", body);

        return resp.getBytes();
    }

    /**
     * Builds a dynamic response body from a canned template.
     *
     * @param httpRespCode the non-null value of the http response code.
     * @param httpRespMsg the non-null short message of the http response line
     * @param htmlTitle the non-null title of the html page in the browser
     * @param htmlMsg the non-null message embedded in the html page in the browser
     * @param user the user object if applicable; if null the "n/a" is used
     * @param origReqLn contains the http method and http URL if applicable or uses "n/a" for both if this object is null
     * @return
     */
    public static String getResponseBody(String htmlTitle, String htmlMsg,
            User user, HttpPackage pkg) {
        String body = BODY_TEMPLATE;
        body = body.replace("{{title}}", htmlTitle);
        body = body.replace("{{message}}", htmlMsg);
        body = body.replace("{{username}}", (user == null ? "n/a" : user.getUsername()));
        body = body.replace("{{action}}", (pkg == null || pkg.requestLine == null ? "n/a" : pkg.requestLine.getMethod()));
        body = body.replace("{{uri}}", (pkg == null  || pkg.requestLine == null ? "n/a"
                : pkg.scheme + "://" + pkg.hostHdr + pkg.requestLine.getUri()));

        return body;
    }

    private static final String REDIRECT_TEMPLATE =
        "HTTP/1.1 {{http-resp-code}} {{http-resp-msg}}" + CRLF
        + "Content-Type: text/html; charset=utf-8" + CRLF
        + "Server: " + Config.serverName() + CRLF
        + "Location: {{location}}" + CRLF
        + CRLF // empty line terminating headers
        + CRLF; // empty line terminating body

    private static final String REDIRECT_CLEARING_SESSION_TEMPLATE =
        "HTTP/1.1 {{http-resp-code}} {{http-resp-msg}}" + CRLF
        + "Content-Type: text/html; charset=utf-8" + CRLF
        + "Server: " + Config.serverName() + CRLF
        + "Set-Cookie: " + Config.getInstance().getCookieName()
        +     "=cookie-monster;Version=1;Path=/;Domain={{cookie-domain}}"
        +     ";Max-Age=0" + CRLF
        + "Location: {{location}}" + CRLF
        + CRLF // empty line terminating headers
        + CRLF; // empty line terminating body

    private static final String REDIRECT_TO_SET_CDSSO_SESSION_TEMPLATE =
        "HTTP/1.1 {{http-resp-code}} {{http-resp-msg}}" + CRLF
        + "Content-Type: text/html; charset=utf-8" + CRLF
        + "Server: " + Config.serverName() + CRLF
        + "Set-Cookie: " + Config.getInstance().getCookieName()
        +     "={{token}};Version=1;Path=/;Domain={{cookie-domain}};Discard" + CRLF
        + "Location: {{location}}" + CRLF
        + CRLF // empty line terminating headers
        + CRLF; // empty line terminating body


    private byte[] get302RedirectToLoginPage(HttpPackage pkg, String httpReasonMsg) throws IOException {
        String origReq = pkg.scheme+"://" + pkg.hostHdr + pkg.requestLine.getUri();
        String origEncReq = URLEncoder.encode(origReq, "utf-8");
        String location = getLoginPageWithGotoUrl(origEncReq);

        String resp = REDIRECT_TEMPLATE;
        resp = resp.replace("{{http-resp-code}}", "302");
        resp = resp.replace("{{http-resp-msg}}", httpReasonMsg);
        resp = resp.replace("{{location}}", location);

        return resp.getBytes();
    }

    /**
     * Returns bytes for an http payload response to set a cookie in the domain
     * in which the request was received while redirecting back to the same
     * requested resource but without the cdsso query parameter used to trigger
     * setting the cookie in this domain. Other query parameters are preserved.
     *
     * @param pkg
     * @param httpReasonMsg
     * @return
     * @throws IOException
     */
    private byte[] get302CdssoRedirect(HttpPackage pkg, String httpReasonMsg) throws IOException {
        String q = pkg.query;
        String cleaned = null;
        String token = null;

        if (q.startsWith(SignInPageCdssoHandler.CDSSO_PARAM_NAME)) {
            int idxE = q.indexOf("&");

            if (idxE == -1) { // only param
                cleaned = null;
                token = q.substring((SignInPageCdssoHandler.CDSSO_PARAM_NAME + "=").length());
            }
            else { // first param
                cleaned = q.substring(idxE+1);
                token = q.substring((SignInPageCdssoHandler.CDSSO_PARAM_NAME + "=").length(), idxE);
            }
        }
        else {
            // idx > 0 otherwise we wouldn't be calling this method
            int idx = q.indexOf(SignInPageCdssoHandler.CDSSO_PARAM_NAME);
            int idxE = q.indexOf("&",idx);

            if (idxE == -1) { // last param
                cleaned = q.substring(0, idx);
                token = q.substring(idx + (SignInPageCdssoHandler.CDSSO_PARAM_NAME + "=").length());
            }
            else { // middle param
                cleaned = q.substring(0, idx) + q.substring(idxE+1);
                token = q.substring(idx + (SignInPageCdssoHandler.CDSSO_PARAM_NAME + "=").length(), idxE);
            }
        }
        String location = pkg.scheme+"://" + pkg.hostHdr + pkg.path;
        if (cleaned != null) {
            location += "?" + cleaned;
        }
        SessionManager smgr = cfg.getSessionManager();
        String ckDomain = smgr.getCookieDomainForHost(pkg.host);
        String resp = REDIRECT_TO_SET_CDSSO_SESSION_TEMPLATE;
        resp = resp.replace("{{http-resp-code}}", "302");
        resp = resp.replace("{{http-resp-msg}}", httpReasonMsg);
        resp = resp.replace("{{location}}", location);
        resp = resp.replace("{{token}}", token);
        resp = resp.replace("{{cookie-domain}}", ckDomain);
        pkg.tokenForCdsso = token;
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

    private void shutdown(InputStream clientIn, OutputStream clientOut, PrintStream log, FileOutputStream fos) {
    	try {
    		if(clientOut != null) {
    			clientOut.close();
    		}
    	} catch(IOException e) { /* Do nothing */ }
    	try {
    		if(clientIn != null) {
    			clientIn.close();
    		}
    	} catch(IOException e) { /* Do nothing */ }
    	try {
    		if(pSocket != null) {
    			pSocket.close();
    		}
    	} catch(IOException e) { /* Do nothing */ }

        if (log != null) {
            logClosed = true;
            log.flush();
            try {
            	fos.close();
            } catch(IOException e) { /* Do nothing */ }
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
     * Creates an HttpPackage object filled with the data representing the
     * stream of characters and bytes for the http request of response.
     */
    private HttpPackage getHttpPackage(boolean isForRequest, InputStream in, boolean waitForDisconnect,
            PrintStream log) {
        HttpPackage pkg = new HttpPackage();

        try {
            determinePackageType(pkg, isForRequest, in, log);
            if (pkg.type == HttpPackageType.BAD_STARTLINE) {
                return pkg;
            }
            captureHeaders(pkg, in, log);
            
            // do we need to capture body or can we move on with just headers?
            // leave this in this open/multi-if-statement form for ease of
            // comprehension
			if (pkg.type == HttpPackageType.EMPTY) {
                return pkg;
            }
			if (pkg.type == HttpPackageType.RESPONSE) {
				// may need to add support for HEAD request at some point so 
				// that we don't go on listening for a body
				if (pkg.responseCode == 304 // per rfc2616, section 10.3.5
						|| pkg.responseCode == 204 // per rfc2616, section 10.2.5
						|| pkg.responseCode == 205 // per rfc2616, section 10.2.6
						) { 
					return pkg; 
				}
				if (pkg.responseCode != 200 && pkg.contentLength == 0) {
					return pkg;
				}
			}

            captureBody(pkg, in, log, waitForDisconnect);
        }
        catch (Exception e) {
            if (log != null) {
                log.println("\nError getting HTTP startline or header data: " + e);
                e.printStackTrace(log);
            }
            LogUtils.severe(cLog, "Error getting HTTP startline or header data:", e);
            if (e instanceof SocketTimeoutException) {
                pkg.socketTimeout = true;
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
     * Creates an HttpPackage object filled with the data representing the
     * stream of bytes for the file requested.
     */
    private HttpPackage getFileHttpPackage(HttpPackage reqPkg, LocalFileEndPoint endpoint, PrintStream log) {
        HttpPackage pkg = new HttpPackage();
        pkg.type = HttpPackageType.RESPONSE;

        String path = endpoint.getFilepathTranslated(reqPkg);
        InputStream is = null;

        try {
            if (path.startsWith(Service.CLASSPATH_PREFIX)) {
                String cpath = path.substring(Service.CLASSPATH_PREFIX.length());
                is = this.getClass().getClassLoader().getResourceAsStream(cpath);
                endpoint.servedFromMsg = "Served from: " + path;
            }
            else {
                File file = new File(path);
                if (file.exists() && file.isFile()) {
                    is = new FileInputStream(file);
                    endpoint.servedFromMsg = "Served from: " + file.getAbsolutePath();
                }
            }
            if (is != null) {
                DataInputStream dis = new DataInputStream(is);
                int size = dis.available();
                byte[] bytes = new byte[size];
                dis.read(bytes);
                pkg.bodyStream.write(bytes);
                dis.close();
                pkg.responseLine = new StartLine("HTTP/1.1", "200", "OK");
                pkg.responseCode = 200;
                pkg.headerBfr.set(new Header(HeaderDef.ContentLength, Integer.toString(size)));
                pkg.headerBfr.set(new Header(HeaderDef.ContentType, endpoint.getContentType()));
                pkg.bodyStream.flush();
            }
            else {
                pkg.responseLine = new StartLine("HTTP/1.1", "404", "Not Found");
                pkg.responseCode = 404;
                endpoint.servedFromMsg = "Unable to serve " + path;
            }
        }
        catch (IOException e) {
            if (log != null) {
                log.println("\nError serving up file: " + path + e);
            }
            pkg = new HttpPackage();
            pkg.type = HttpPackageType.RESPONSE;
            try {
                pkg.responseLine = new StartLine("HTTP/1.1", "500", "Internal Server Error");
            } catch (MalformedURLException e1) {
                // ignore since we are creating a response line here and hence
                // won't incur any URL parsing
            }
            pkg.responseCode = 500;
            endpoint.servedFromMsg = "Unable to serve " + path;
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
            LogUtils.fine(cLog, "content length = 0 for '{0}', will wait for disconnect or tcp timeout to terminate body content.",
            	(pkg.requestLine != null ? pkg.requestLine :
            		(pkg.responseLine != null ? pkg.responseLine :
                    	"???")));
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
        LogUtils.fine(cLog, "done with body content.");
    }

    /**
     * Reads the input stream until it sees the empty line separating headers
     * from the body of the http package and captures values of headers of
     * interest placing them in the passed-in package object.
     *
     * @param pkg
     * @param in
     * @param excludeHeaders
     * @param log
     * @throws IOException
     */
    private void captureHeaders(HttpPackage pkg, InputStream in, 
            PrintStream log) throws IOException {
        if (pkg.type == HttpPackageType.EMPTY) {
            return;
        }
        String data = "";
        // create handler set for processing headers for this package
        HandlerSet hSet = HandlerSet.newInstance();

        // get header info
        while ((data = readLine(in, log)) != null) {
            if (data.length() == 0) {
                // the headers ends at the first blank line
                break;
            }

            boolean headerHandled = false;
            int colon = data.indexOf(':');
            String hdrName = data.substring(0, colon).trim().toLowerCase();
            String hdrVal = data.substring(colon+1).trim();
            
            for (HeaderHandler xhdr : hSet.handlers) {
                if (xhdr.appliesTo(hdrName, pkg.type)) {
                    headerHandled = true;
                    xhdr.handle(hdrName, hdrVal, pkg, hSet, cfg);
                    break;
                }
            }
            // if no special handling occurred then pass the header onward
            if (headerHandled == false) {
                pkg.headerBfr.append(new Header(hdrName, hdrVal));
            }
        }
        
        // now apply contingent handling if any which could remove headers
        for (SecondPhaseAction action : hSet.secondPhaseActions) {
            action.apply();
        }
    }

    private void determinePackageType(HttpPackage pkg, boolean isForRequest, InputStream in, PrintStream log) throws IOException {
        // get the first line of the message, get the response code if a
        // response.
        String line = readLine(in, log);
        if (line == null || line.equals("")) {
            pkg.type = HttpPackageType.EMPTY;
            return;
        }
        else {
            StartLine startLn = new StartLine(line);
            if (startLn.isBadLine()) {
                if (isForRequest) {
                    pkg.requestLine = startLn;
                }
                else {
                    pkg.responseLine = startLn;
                }
                pkg.type = HttpPackageType.BAD_STARTLINE;
                return;
            }
            /*
             * Distinguish between parsing of an http request versus http
             * response. Ie: first line looks like:
             *
             * this: "GET /opensso/UI/Login?parmAAA=111&parmBBB=222 HTTP/1.1"
             * versus: "HTTP/1.1 200 OK"
             */
            if (startLn.isResponseLine()) {
                pkg.responseLine = startLn;
                if (pkg.responseLine.getHttpVer().equals("1.0")) {
                    pkg.httpVer = 0;
                }
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
            else { // isRequestLine
                pkg.requestLine = startLn;
                pkg.origRequestList = startLn;

                if (pkg.requestLine.getHttpVer().equals("1.0")) {
                    pkg.httpVer = 0;
                }
                // if absolute REQ-URI can set these values here, else wait for
                // host header processing to determine
                if (startLn.isAbsReqURI()) {
                    pkg.host = pkg.requestLine.getAbsReqUri_host();
                    pkg.port = pkg.requestLine.getAbsReqUri_port();
                    pkg.hasNonDefaultPort = ! pkg.requestLine.getAbsReqUriUsesDefaultPort();
                }
                pkg.signMeInDetected = GlobalHeaderNames.detectedAndStrippedSignMeIn(pkg);
                pkg.signMeOutDetected = GlobalHeaderNames.detectedAndStrippedSignMeOut(pkg);
                pkg.type = HttpPackageType.REQUEST;
                pkg.path = pkg.requestLine.getReqPath();
                pkg.query = pkg.requestLine.getReqQuery();
            }
        }
    }

    /**
     * Used to cache the next line to be read from an input stream as received
     * from calling readLine. This is used in unfolding "folded" headers.
     */
    private boolean nextLineIsCached = false;
    private String cachedNextLine = null;

    private String readLine(InputStream in, PrintStream log) throws IOException {
        // reads a line of text from an InputStream
        StringBuffer data = new StringBuffer("");
        int c;

            // if we have nothing to read, just return null
            in.mark(1);
            if (in.read() == -1) {
                return null;
            }
            else
                in.reset();

            while ((c = in.read()) >= 0) {
                // check for an end-of-line character
                if ((c == 0) || (c == 10) || (c == 13)) {
                    break;
                }
                else {
                    data.append((char) c);
                }
            }

            // deal with the case where the end-of-line terminator is \r\n
            if (c == 13) {
                in.mark(1);

                if (in.read() != 10) {
                    in.reset();
                }
            }

        // and return what we have
        return data.toString();
    }
}
