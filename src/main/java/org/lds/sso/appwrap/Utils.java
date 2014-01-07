package org.lds.sso.appwrap;

import java.io.*;
import java.net.Socket;

/**
 * Holder of general utility functions needed across the codebase.
 *
 * Created by markboyd on 1/6/14.
 */
public class Utils {

    /**
     * Closes the passed input stream ignoring any thrown exception.
     *
     * @param is
     */
    public static void quietlyClose(InputStream is) {
        try{
            if (is != null) {
                is.close();
            }
        }
        catch (IOException ioe) {
            // ignore the exception and move on
        }
    }

    /**
     * Closes the passed reader ignoring any thrown exceptions.
     * @param rd
     */
    public static void quietlyClose(Reader rd) {
        try{
            if (rd != null) {
                rd.close();
            }
        }
        catch (IOException ioe) {
            // ignore the exception and move on
        }
    }

    /**
     * Closes the passed in writer ignoring any thrown exceptions.
     * @param writer
     */
    public static void quietlyClose(Writer writer) {
        try{
            if (writer != null) {
                writer.close();
            }
        }
        catch (IOException ioe) {
            // ignore the exception and move on
        }
    }

    /**
     * Closes the passed in socket ignoring any thrown exceptions.
     * @param s
     */
    public static void quietlyClose(Socket s) {
        try{
            if (s != null) {
                s.close();
            }
        }
        catch (IOException ioe) {
            // ignore the exception and move on
        }
    }

    /**
     * Closes the passed output stream ignoring any thrown exception.
     *
     * @param os
     */
    public static void quietlyClose(OutputStream os) {
        try{
            if (os != null) {
                os.close();
            }
        }
        catch (IOException ioe) {
            // ignore the exception and move on
        }
    }

}


