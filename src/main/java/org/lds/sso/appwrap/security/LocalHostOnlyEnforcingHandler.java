package org.lds.sso.appwrap.security;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.HandlerCollection;

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
	 * Object placed on thread local for conveying the inet address of the 
	 * socket originating the handler request.
	 */
	public static class InetAddressHolder {
		private InetAddress addr = null;
		
		public void setAddress(InetAddress addr) {
			this.addr = addr;
		}
	}
	
	/**
	 * Threadlocal holder of InetAddressHolder allowing {@link LocalHostOnlyEnforcingHandler}
	 * to access the InetAddress object of the socket that originated this request.
	 */
	private static ThreadLocal<InetAddressHolder> holderHolder = new ThreadLocal<InetAddressHolder>() {

		/**
		 * Sets up a holder of an InetAddress object.
		 */
		@Override
		protected InetAddressHolder initialValue() {
			return new InetAddressHolder();
		}
	};

	/**
	 * Get an InetAddress holder for the current thread.
	 * 
	 * @return
	 */
	public static InetAddressHolder getAddressHolder() {
		return holderHolder.get();
	}
	
	/**
	 * Get an InetAddress holder for the current thread.
	 * 
	 * @return
	 */
	public static void removeAddressHolder() {
		holderHolder.remove();
	}
	
	/**
	 * Crafts the message to include in the html body of the response.
	 * 
	 * @param addr
	 * @return
	 */
	public static String getHtmlMessage(InetAddress addr) {
		return "Only local traffic is allowed but request was received from host "
        + addr.getHostName() + " with address "
        + addr.getHostAddress() + ".";
	}

	/**
	 * Returns true if the socket is for a local connection meaning from the 
	 * same host on which the wamulator is running.
	 * 
	 * @param socket
	 * @return
	 */
	public static boolean isLocalAccess(InetAddress addr) {
		return addr.isLoopbackAddress() || addr.isLinkLocalAddress();
	}

	//////////////////// INSTANCE METHODS /////////////////////////////
	
	/**
	 * Handle jetty destined traffic and enforce access to local traffic only.
	 */
	public void handle(String target, HttpServletRequest request,
			HttpServletResponse response, int dispatch) throws IOException,
			ServletException {
		InetAddress addr = holderHolder.get().addr;
		
		if (addr == null) {
			String msg = this.getClass().getSimpleName() 
					+ " is attempting to enforce local access only but has no "
					+ "information to determine if access is allowed. Blocking "
					+ " traffic for " + request.getMethod()
					+ " on " + request.getRequestURI();
			cLog.warning(msg);
			response.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            ((Request) request).setHandled(true);
			return;
		}
		
		if (! isLocalAccess(addr)) {
			String msg = getHtmlMessage(addr) + " Method: " + request.getMethod()
					+ ", uri: " + request.getRequestURI();
			cLog.warning(msg);
			response.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            ((Request) request).setHandled(true);
			return;
		}
		
		super.handle(target, request, response, dispatch);
	}
}
