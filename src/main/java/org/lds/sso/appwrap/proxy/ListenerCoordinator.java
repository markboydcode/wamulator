package org.lds.sso.appwrap.proxy;

import java.io.File;
import java.text.DecimalFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.lds.sso.appwrap.Config;

/**
 * Coordinates activity shared by http and https listeners.
 * 
 * @author BoydMR
 *
 */
public class ListenerCoordinator implements Runnable {
    AtomicInteger counter = new AtomicInteger();;
    Config cfg;
    private CountDownLatch logScrubGate;
    private DecimalFormat connIdFmtr;

    public ListenerCoordinator(Config cfg) {
        // first make sure that we have a log file directory
        makeLogDirectory();
        this.cfg  = cfg;
        String field = getFormatFieldFor(cfg.getMaxEntries());
        connIdFmtr = new DecimalFormat(field);

        this.logScrubGate = new CountDownLatch(1);
        startLogFileCleaner();
    }

    /**
     * Allows unit tests to forgo creating the log directory by overriding.
     */
    protected void makeLogDirectory() {
        File logDir = new File(Config.LOG_FILES_LOCATION);
        if (! logDir.exists()) {
            if (! logDir.mkdir()) {
                throw new IllegalStateException("Unable to make log directory "
                        + logDir.getAbsolutePath() + ". "
                        + "Please create it manually.");
            }
        }
    }

    /**
     * Allows unit tests to forgo starting the cleaner by overriding.
     */
    protected void startLogFileCleaner() {
        Thread cleaner = new Thread(this);
        cleaner.setName(ListenerCoordinator.class.getSimpleName() + "_logs-cleaner");
        cleaner.start();
    }

    /**
     * Returns String containing ascii zero characters, '0', 
     * @param max
     * @return
     */
    public String getFormatFieldFor(int max) {
        /*
         * I subtract one here since the count is zero based so that filename
         * is as short as can be and we don't have an additional column used 
         * only for the max value file name. 
         *  
         * ie: max = 10 gives names of:
         * 00
         * 01
         * ...
         * 09
         * 10
         * 
         *  Allowing only up to the max ensures we have max file names and the
         *  shortest meaningful names possible.
         */
        int digits = Integer.toString(max-1).length();
        StringBuffer buf = new StringBuffer();
        for(int i=0; i<digits; i++) {
            buf.append('0');
        }
        return buf.toString();
    }

    /**
     * Thread safe way of getting a connection id with a wrapping limit rolling
     * back to zero when the configured max is reached.
     * 
     * @return
     */
    public String getConnectionId() {
        counter.compareAndSet(cfg.getMaxEntries(), 0);
        int count = counter.getAndIncrement();
        String connId = Config.LOG_FILES_PREFIX + connIdFmtr.format(count);
        return connId;    
    }
    
    /**
     * Blocks the calling thread until cleanout of old log files has completed.
     */
    public void waitForFileCleanout() {
        try {
            logScrubGate.await();
        } catch (InterruptedException e) {
            // don't care, very rare, let it flow, off you go.
        }
    }

    /**
     * Cleans out old log files prior to accepting connections at startup.
     */
    public void run() {
        try {
            for (File f : new File(Config.LOG_FILES_LOCATION).listFiles()) {
                String nm = f.getName();

                if (f.isFile() && (nm.equals("Requests.log")
                        || (nm.startsWith("C-") && nm.endsWith(".log")))) {
                    f.delete();
                }
            }
        }
        finally {
            // let listeners start handling connections
            logScrubGate.countDown(); 
        }
    }
}
