package org.lds.sso.appwrap.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.exception.ServerFailureException;

public class RemoteStartServiceCommand extends Command {
	public RemoteStartServiceCommand(String cfgPath, Integer timeout) {
		super(cfgPath, timeout);
	}
	public RemoteStartServiceCommand(String cfgPath) {
		super(cfgPath);
	}

	@Override
	void doExecute(Service service, Config cfg) {
		String classpath = System.getProperty("java.class.path");
		try {
			String[] args = { "java", "-cp", classpath, Service.class.getName(), "start", cfgPath };
			Process p = Runtime.getRuntime().exec(args);
			/*StreamOutputter err = new StreamOutputter("STDERR", p.getErrorStream());
			StreamOutputter out = new StreamOutputter("STDOUT", p.getInputStream());
			new Thread(err).start();
			new Thread(out).start();*/
		} catch ( IOException e ) {
			throw new ServerFailureException(e);
		}
	}

	@Override
	int getTargetResponseCode() {
		return 200;
	}

	static class StreamOutputter implements Runnable {
		private String name;
		private InputStream is;

		public StreamOutputter(String name, InputStream is) {
			this.name = name;
			this.is = is;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);

				while (true) {
					String s = br.readLine();
					if (s == null)
						break;
					System.out.println("[" + name + "] " + s);
				}

				is.close();

			} catch (Exception ex) {
				System.out.println("Problem reading stream " + name + "... :"
						+ ex);
				ex.printStackTrace();
			}
		}

	}
}