package org.lds.sso.appwrap;

import java.io.IOException;
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
     * Class for handing some avilable ports around.
     * 
     * @author boydmr
     *
     */
    public static class Ports {
        public int console = -1;
        public int proxy = -1;
    }
    
    /**
     * Obtains some available ports for use in running the simulator for tests.
     * 
     * @return
     * @throws IOException
     */
    public static Ports getAvailableSimulatorPorts() throws IOException {
        Ports ports = new Ports();

        ServerSocket css = new ServerSocket();
        css.setReuseAddress(true);
        css.bind(null);
        ports.console = css.getLocalPort();
        
        ServerSocket pss = new ServerSocket();
        pss.setReuseAddress(true);
        pss.bind(null);
        ports.proxy = pss.getLocalPort();
        
        css.close();
        pss.close();
        return ports;
    }

}
