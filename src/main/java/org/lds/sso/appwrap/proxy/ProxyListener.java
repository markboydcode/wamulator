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

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DecimalFormat;

	public class ProxyListener implements Runnable
	{
		private static final Logger cLog = Logger.getLogger(ProxyListener.class);
		
		private ServerSocket server = null;
		private boolean started = false;
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
		public void closeSocket ()
		{
			try {
				// close the open server socket
				server.close();
				// send it a message to make it stop waiting immediately
				// (not really necessary)
				/*Socket s = new Socket("localhost", thisPort);
				OutputStream os = s.getOutputStream();
				os.write((byte)0);
				os.close();
				s.close();*/
			}  catch(Exception e)  { 
					cLog.error("Error occurred closing listener socket.", e);
			}
			
			server = null;
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
				
				while (true)
				{
					count++;
					if (count > cfg.getMaxEntries()) {
						count = 1;
					}
					String connId = "C-" + fmt.format(count); 
					Socket client = server.accept();
					client.setSoTimeout(cfg.getProxyInboundSoTimeout());
					RequestHandler h = new RequestHandler(client, cfg, connId);
					Thread t = new Thread(h, "RequestHandler " + connId);
					t.setDaemon(true);
					t.start();
				}
			} catch ( SocketException e ) {
				String msg = "Proxy Listener error: " + e;
				e.printStackTrace();
				System.out.println(msg);
				cLog.error("Proxy Listener error: ", e); 
			} catch ( IOException e ) {
				String msg = "Proxy Listener error: " + e;
				e.printStackTrace();
				System.out.println(msg);
				cLog.error("Proxy Listener error: ", e); 				
			} finally {
				started = false;
			}
		}
		
	}


