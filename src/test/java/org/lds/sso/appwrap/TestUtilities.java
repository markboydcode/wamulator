package org.lds.sso.appwrap;

import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Helper functions for unit tests.
 */
public class TestUtilities {


    /**
     * Creates a client that proxies through the indicated port (ie: the wamulator's proxy port)
     * and is a non-redirect-following, non-retrying,
     * 1 second tcp-timeout honoring http client instance. Making it use a proxy disables
     * dns lookups offloading that responsibility to the proxy and thus allowing us to test
     * matching against by-site declarations that may not resolve via DNS during unit tests.
     *
     * @return
     */
    public static CloseableHttpClient createWamulatorProxiedHttpClient(int localPortToConnectThrough) {
        // route requests through local wamulator proxy port
        HttpHost prxy = new HttpHost("127.0.0.1", localPortToConnectThrough);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(prxy);

        CloseableHttpClient client = HttpClients.custom()
                .disableAutomaticRetries()
                .disableRedirectHandling()
                .setRoutePlanner(routePlanner)
                .setDefaultSocketConfig(
                        // ensures that if a server error drops the connection or doesn't
                        // respond we don't wait for the default 30 seconds of inactivity
                        // before TCP throwing a socket timeout error.
                        SocketConfig.custom().setSoTimeout(1000).build()
                )
                .build();
        return client;
    }

    /**
     * Creates a directly connecting, non-redirect-following, non-retrying,
     * 1 second tcp-timeout honoring http client instance.
     *
     * @return
     */
    public static CloseableHttpClient createNonProxiedHttpClient() {
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory() )
                .build();

        // resolve all hosts names to local box
        DnsResolver dnsResolver = new SystemDefaultDnsResolver() {

            @Override
            public InetAddress[] resolve(final String host) throws UnknownHostException {
                return new InetAddress[] { InetAddress.getByAddress(new byte[] {127, 0, 0, 1}) };
            }
        };

        HttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry, dnsResolver);

        CloseableHttpClient client = HttpClients.custom()
                .disableAutomaticRetries()
                .disableRedirectHandling()
                .setConnectionManager(connMgr)
                .setDefaultSocketConfig(
                        // ensures that if a server error drops the connection or doesn't
                        // respond we don't wait for the default 30 seconds of inactivity
                        // before TCP throwing a socket timeout error.
                        SocketConfig.custom().setSoTimeout(1000).build()
                )
                .build();
        return client;
    }

    /**
     * Hits the authentication service for the simulator and obtains the returned
     * cookie for the domain specified which must match the site of the cookie or a subdomain
     * for a site-wide cookie such as domain='.lds.org'.
     *
     * @param username
     * @param domain
     * @return
     */
    public static String authenticateUser(String username, String domain) throws IOException {
        Config cfg = Config.getInstance();
        String endpoint = "http://" + domain + ":" + cfg.getConsolePort() + cfg.getWamulatorServiceUrlBase() + "/action/set-user/" + username;

        CloseableHttpClient client = createNonProxiedHttpClient();

        HttpGet get = new HttpGet(endpoint);
        CloseableHttpResponse response = client.execute(get);
        int status = response.getStatusLine().getStatusCode();

        Header[] hdrs = response.getHeaders("set-cookie");
        if (hdrs.length == 0) {
            throw new IOException("No set-cookie header found in sign-in response.");
        }
        String cookieNm = Config.getInstance().getCookieName();
        for(Header hdr : hdrs) {
            // set-cookie header has formatted element structure with parameters name=value; p1=x; ...
            HeaderElement elm = hdr.getElements()[0];
            if (elm.getName().equals(cookieNm)) {
                return elm.getValue();
            }
        }
        throw new IOException("No cookie with name'" + cookieNm + "' found in sign-in response.");
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

    /**
     * Accepts an entity of content type "text/*" and returns its content extracted into a String.
     *
     * @param entity
     * @return
     */
    public static String readHttpComponentsStringEntity(HttpEntity entity) throws IOException {
        Header hdr = entity.getContentType();
        if (hdr == null) {
            return null; // don't know what content type therefore there can't be any content in the response
        }
        String type = hdr.getValue();
        if (!type.toLowerCase().startsWith("text/")) {
            throw new IllegalArgumentException("Attempting to read text from entity with type " + type);
        }
        HeaderElement elm = hdr.getElements()[0]; // content-type header: formatted content with params so convert to header element
        NameValuePair csp = elm.getParameterByName("charset");
        String charSet = "utf-8"; // default
        if (csp != null) {
            charSet = csp.getValue();
        }
        StringBuffer sb = new StringBuffer();
        InputStream in = entity.getContent();
        InputStreamReader rdr = new InputStreamReader(in, charSet);
        char[] chars = new char[1024];
        int charsRead = 0;

        while (charsRead != -1) {
            sb.append(chars, 0, charsRead);
            charsRead = rdr.read(chars);
        }
        return sb.toString();
    }

    /**
     * Accepts an entity of content type "application/json" and returns its content extracted into a String.
     *
     * @param entity
     * @return
     */
    public static String readHttpComponentsJsonEntityAsString(HttpEntity entity) throws IOException {
        Header hdr = entity.getContentType();
        String type = hdr.getValue();
        if (!type.toLowerCase().startsWith("application/json")) {
            throw new IllegalArgumentException("Attempting to read text from application/json entity but found type " + type);
        }
        HeaderElement elm = hdr.getElements()[0]; // content-type header: formatted content with params so convert to header element
        NameValuePair csp = elm.getParameterByName("charset");
        String charSet = "utf-8"; // default
        if (csp != null) {
            charSet = csp.getValue();
        }
        StringBuffer sb = new StringBuffer();
        InputStream in = entity.getContent();
        InputStreamReader rdr = new InputStreamReader(in, charSet);
        char[] chars = new char[1024];
        int charsRead = 0;

        while (charsRead != -1) {
            sb.append(chars, 0, charsRead);
            charsRead = rdr.read(chars);
        }
        return sb.toString();
    }
}
