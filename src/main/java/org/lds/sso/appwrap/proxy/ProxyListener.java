package org.lds.sso.appwrap.proxy;

	/* <!-- in case someone opens this in a browser... --> <pre> */
	/*
	 * This is a simple multi-threaded Java proxy server
	 * for HTTP requests (HTTPS doesn't seem to work, because
	 * the CONNECT requests aren't always handled properly).
	 * I implemented the class as a thread so you can call it
	 * from other programs and kill it, if necessary (by using
	 * the closeSocket() method).
	 *
	 * We'll call this the 1.1 version of this class. All I
	 * changed was to separate the HTTP header elements with
	 * \r\n instead of just \n, to comply with the official
	 * HTTP specification.
	 *
	 * This can be used either as a direct proxy to other
	 * servers, or as a forwarding proxy to another proxy
	 * server. This makes it useful if you want to monitor
	 * traffic going to and from a proxy server (for example,
	 * you can run this on your local machine and set the
	 * fwdServer and fwdPort to a real proxy server, and then
	 * tell your browser to use "localhost" as the proxy, and
	 * you can watch the browser traffic going in and out).
	 *
	 * One limitation of this implementation is that it doesn't
	 * close the RequestHandler socket if the client disconnects
	 * or the server never responds, so you could end up with
	 * a bunch of loose threads running amuck and waiting for
	 * connections. As a band-aid, you can set the server socket
	 * to timeout after a certain amount of time (use the
	 * setTimeout() method in the RequestHandler class), although
	 * this can cause false timeouts if a remote server is simply
	 * slow to respond.
	 *
	 * Another thing is that it doesn't limit the number of
	 * socket threads it will create, so if you use this on a
	 * really busy machine that processed a bunch of requests,
	 * you may have problems. You should use thread pools if
	 * you're going to try something like this in a "real"
	 * application.
	 *
	 * Note that if you're using the "main" method to run this
	 * by itself and you don't need the debug output, it will
	 * run a bit faster if you pipe the std output to 'nul'.
	 *
	 * You may use this code as you wish, just don't pretend
	 * that you wrote it yourself, and don't hold me liable for
	 * anything that it does or doesn't do. If you're feeling
	 * especially honest, please include a link to nsftools.com
	 * along with the code. Thanks, and good luck.
	 *
	 * Julian Robichaux -- http://www.nsftools.com
	 */

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.util.logging.Logger;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.io.LogUtils;

	public class ProxyListener implements Runnable
	{
		private static final Logger cLog = Logger.getLogger(ProxyListener.class.getName());

		private ServerSocket server = null;
		private volatile boolean started = false;
		private volatile int count = 0;
		private Config cfg = null;

		/* the proxy server just listens for connections and creates
		 * a new thread for each connection attempt (the RequestHandler
		 * class really does all the work)
		 */
		public ProxyListener (Config cfg) throws IOException
		{
			this.cfg  = cfg;
            if (cfg.getProxyPort() == 0) {
                server = new ServerSocket();
                server.setReuseAddress(true);
                server.bind(null);
                cfg.setProxyPort(server.getLocalPort());
            }
            else {
                server = new ServerSocket(cfg.getProxyPort());
            }
            server.setSoTimeout(1000);
		}

		/* return whether or not the socket is currently open
		 */
		public boolean isRunning ()
		{
			if (server == null || !started)
				return false;
			else
				return true;
		}


		/* closeSocket will close the open ServerSocket; use this
		 * to halt a running jProxy thread
		 */
		public void stop ()
		{
			started = false;
		}


		public void run()
		{
			try {
				// loop forever listening for client connections

				for (File f : new File(".").listFiles()) {
					String nm = f.getName();

					if (f.isFile() && (nm.equals("Requests.log")
							|| (nm.startsWith("C-") && nm.endsWith(".log")))) {
						f.delete();
					}
				}
				DecimalFormat fmt = new DecimalFormat("0000");
				started = true;

				while (started)
				{
					try {
						Socket client = server.accept();
						client.setSoTimeout(cfg.getProxyInboundSoTimeout());
						count++;
						if (count > cfg.getMaxEntries()) {
							count = 1;
						}
						String connId = "C-" + fmt.format(count);
						RequestHandler h = new RequestHandler(client, cfg, connId);
						Thread t = new Thread(h, "RequestHandler " + connId);
						t.setDaemon(true);
						t.start();
					} catch(SocketTimeoutException e) {
						cLog.finest("Accept timedout.  let's try again.");
					}
				}
			} catch ( IOException e ) {
				if(!e.getMessage().contains("socket closed")) {
					LogUtils.warning(cLog, "Proxy Listener error.  Increase logging level to see full error.  If you're seeing this message during shutdown you can probably ignore it.");
					LogUtils.fine(cLog, "Proxy Listener error: ", e);
				} else {
					LogUtils.severe(cLog, "Unexpected error:", e);
				}
			} finally {
				try {
					if(server != null) {
						server.close();
					}
				} catch(Exception e) { /* Do nothing */ }
				started = false;
			}
		}

	}


