package org.lds.sso.appwrap.security;

import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerCollection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Logger;


/**
 * Object acting both as a Jetty handler and as a resource to the proxy port's
 * RequestHandler instance for a connection to restrict access to the local host
 * only if configuration dictates and provide a consistent enforcement response
 * for proxy, console, and rest traffic.
 *  
 * @author BoydMR
 *
 */
public class LocalHostOnlyEnforcingHandler extends HandlerCollection {
    private static final Logger cLog = Logger.getLogger(LocalHostOnlyEnforcingHandler.class.getName());

	// statics holding response code and http message constants
	public static final int INT_HTTP_RESPONSE_CODE = 403;
	public static final String HTTP_RESPONSE_CODE = "" + INT_HTTP_RESPONSE_CODE;
	public static final String HTTP_RESPONSE_MSG = "Remote Connections Forbidden";
	public static final String HTML_TITLE = HTTP_RESPONSE_CODE + " " + HTTP_RESPONSE_MSG;

	/**
	 * Returns true if the socket is for a local connection meaning from the
	 * same host on which the wamulator is running.
	 * 
	 * @return
	 */
	public static boolean isLocalAccess(InetAddress addr) {
		return addr.isLoopbackAddress() || addr.isLinkLocalAddress();
	}

	//////////////////// INSTANCE METHODS /////////////////////////////
	
	/**
	 * Handle jetty destined traffic and enforce access to local traffic only.
	 */
	public void handle(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException,
			ServletException {
        String msg = null;
        boolean allowed = false;

        HttpChannel<?> channel = baseRequest.getHttpChannel();
        if (channel!=null)
        {
            EndPoint endp=channel.getEndPoint();
            if (endp!=null)
            {
                InetSocketAddress address = endp.getRemoteAddress();
                if (address != null) {

                    InetAddress addr = address.getAddress();

                    if (addr == null) {
                        msg = this.getClass().getSimpleName()
                                + " is attempting to enforce local access only but can't obtain InetAddress instance.";
                    }
                    else if (! isLocalAccess(addr)) {
                        msg = getIllegalAddressMessage(addr);
                    }
                    else {
                        allowed = true;
                    }
                }
                else {
                    msg = this.getClass().getSimpleName()
                            + " is attempting to enforce local access only but can't obtain InetSocketAddress instance.";
                }
            }
            else {
                msg = this.getClass().getSimpleName()
                        + " is attempting to enforce local access only but can't obtain channel EndPoint instance.";
            }
        }
        else {
            msg = this.getClass().getSimpleName()
                    + " is attempting to enforce local access only but can't obtain HttpChannel instance.";
        }

        if (allowed) {
            super.handle(target, baseRequest, request, response);
        }
        else {
            msg +=  " Blocking "
                    + " traffic for " + request.getMethod()
                    + " on " + request.getRequestURI();
            cLog.warning(msg);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            baseRequest.setHandled(true);
        }
	}

    /**
     * Crafts message indicating that the remote party is not allowed to connect.
     *
     * @param addr
     * @return
     */
    public static String getIllegalAddressMessage(InetAddress addr) {
        return "Only local traffic is allowed but request was received from host "
                + addr.getHostName() + " with address "
                + addr.getHostAddress() + ".";
    }
}
