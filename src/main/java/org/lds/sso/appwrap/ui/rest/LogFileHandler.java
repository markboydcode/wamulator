package org.lds.sso.appwrap.ui.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.rest.RestHandlerBase;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;

import org.lds.sso.appwrap.proxy.Header;
import org.lds.sso.appwrap.proxy.HeaderDef;
import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.HttpPackageType;
import org.lds.sso.appwrap.proxy.StartLine;

/**
 * Handles request for logs
 *  
 * @author Scott Lewis
 * @copyright: Copyright, 2009, The Church of Jesus Christ of Latter Day Saints
 *
 */
public class LogFileHandler extends RestHandlerBase {

    private static final Logger log = Logger.getLogger(RestHandlerBase.class);
    public static final String CRLF = "" + ((char) 13) + ((char) 10); // "\r\n";
    public static final char SP = ' ';
    public static final char HT = ((char) ((int) 9));
    public static final String EMPTY_START_LINE = "empty-start-line";

    public LogFileHandler(String pathPrefix) {
        super(pathPrefix);
    }

    /*
     */
    @Override
    protected void doHandle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
            throws IOException {
        //
        //		Get the current config instance each time which allows for reconfig
        //		of config object without restarting the service.
        Config cfg = Config.getInstance();
        String pathInfo = request.getPathInfo();
        String filePath = pathInfo.substring(pathPrefix.length() + 1);

        BufferedOutputStream clientOut = new BufferedOutputStream(response.getOutputStream());

        HttpPackage resPkg = getFileHttpPackage(filePath);

        byte[] res = serializePackage((StartLine) resPkg.responseLine, resPkg);
        clientOut.write(res, 0, res.length);
	clientOut.flush();
    }

    /**
     * Serializes an HttpPackage instance for sending across the socket.
     *
     * @param pkg
     * @return
     * @throws IOException
     */
    private byte[] serializePackage(StartLine httpStartLine, HttpPackage pkg) throws IOException {
        ByteArrayOutputStream appRespBfr = new ByteArrayOutputStream();
        String startLineContent = EMPTY_START_LINE;
        if (httpStartLine != null) {
            startLineContent = httpStartLine.toString();
        }
        appRespBfr.write(startLineContent.getBytes());
        appRespBfr.write(CRLF.getBytes());
        for(Iterator<Header> itr = pkg.headerBfr.getIterator(); itr.hasNext();) {
            itr.next().writeTo(appRespBfr);
        }
        appRespBfr.write(CRLF.getBytes());
        appRespBfr.write(pkg.bodyStream.toByteArray());

        return appRespBfr.toByteArray();
    }

    private HttpPackage getFileHttpPackage(String filePath) {
        HttpPackage pkg = new HttpPackage();
        pkg.type = HttpPackageType.RESPONSE;

        InputStream is = null;

        try {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                is = new FileInputStream(file);
            }

            if (is != null) {
                DataInputStream dis = new DataInputStream(is);
                int size = dis.available();
                byte[] bytes = new byte[size];
                dis.read(bytes);
                pkg.bodyStream.write(bytes);
                dis.close();
                pkg.responseLine = new StartLine("HTTP/1.1", "200", "OK");
                pkg.responseCode = 200;
                pkg.headerBfr.set(HeaderDef.createHeader(HeaderDef.ContentLength, Integer.toString(size)));
                pkg.headerBfr.set(HeaderDef.createHeader(HeaderDef.ContentType, "text/plain"));
                pkg.bodyStream.flush();
            } else {
                pkg.responseLine = new StartLine("HTTP/1.1", "404", "Not Found");
                pkg.responseCode = 404;
            }
        } catch (IOException e) {
            //if (log != null) {
            //    log.("\nError serving up file: " + filePath + e);
            //}
            pkg = new HttpPackage();
            pkg.type = HttpPackageType.RESPONSE;
            pkg.responseLine = new StartLine("HTTP/1.1", "500", "Internal Server Error");
            pkg.responseCode = 500;
        }
        return pkg;
    }

}
