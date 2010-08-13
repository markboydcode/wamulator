package org.lds.sso.appwrap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.testng.Assert;

public class TestUtilities {

    /**
     * Hits the authentication service for the simulator and obtains the returned
     * cookie.
     * 
     * @param username
     * @param simulatorAdminPort
     * @return
     * @throws HttpException
     * @throws IOException
     */
    public static String authenticateUser(String username, int simulatorAdminPort) throws HttpException, IOException {
        String endpoint = "http://127.0.0.1:" + simulatorAdminPort + "/auth/ui/authenticate?username=" + username;
        
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(endpoint);
        method.setFollowRedirects(false);
        int status = client.executeMethod(method);
        
        Header ck = method.getResponseHeader("set-cookie");
        String[] tokens = ck.getValue().split("=");
        String cookie = tokens[0];
        String cookieParms = tokens[1];
        String[] parms = cookieParms.split(";");
        return parms[0];
    }
    
    /**
     * Reads all available http headers watching for an empty line to indicate
     * completion and returning the entire chunk including line termination
     * characters in a String. Includes blocking until data is available.
     * 
     * @param in
     * @return
     * @throws IOException
     */
    public static String readAllHttpHeaders(InputStream in) throws IOException {
        boolean emptyLineFound = false;
        StringBuffer buffer = new StringBuffer("");
        int c = -1;
        
        while (!emptyLineFound) {
            StringBuffer line = new StringBuffer("");
            if (c != -1) { // capture any char left over from last loop
                line.append((char)c);
            }
                // if we have nothing to read, just return null
                c = in.read();
                if (c == -1) {
                    if (buffer.length() == 0) {
                        return null; // no content due to end of stream
                    }
                    return buffer.toString();
                }

                do {
                    line.append((char)c);
                    // check for an end-of-line character
                    if ((c == 0) || (c == 10) || (c == 13)) {
                        break;
                    }
                    else {
                    }
                    c = in.read();
                } while (c >= 0); 

                // deal with the case where the end-of-line terminator is \r\n
                if (c == 13) {
                    c = in.read(); 
                    
                    if (c == -1) {
                        emptyLineFound = true;
                    }
                    else if (c != 10) {
                    }
                    else {
                        line.append((char)c);
                        if (line.length() == 2) {
                            emptyLineFound = true;
                        }
                        else {
                            c = -1; // purge char so it isn't consumed by next loop
                        }
                    }
                }

            buffer.append(line.toString());
            line.setLength(0);
        }
        return buffer.toString();
    }
}
