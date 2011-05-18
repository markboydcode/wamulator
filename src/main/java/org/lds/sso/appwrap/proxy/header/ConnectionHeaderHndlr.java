package org.lds.sso.appwrap.proxy.header;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.HttpPackageType;

/**
 * Implements RFC 2616 section 14.10 for proper handling of the Connection
 * header by removing all headers indicated in the Connection header save for 
 * the 'close' token which marks the connection as non-persistent.
 * 
 * @author BoydMR
 *
 */
public class ConnectionHeaderHndlr implements HeaderHandler {

    public void handle(String lcHeaderName, String headerValue,
            final HttpPackage pkg, HandlerSet hSet, Config cfg) {
        final String[] tokens = headerValue.split("\\s");
        
        hSet.secondPhaseActions.add(new SecondPhaseAction() {
            @Override
            public void apply() {
                for (String hdrName : tokens) {
                    if (hdrName.equals("close")) {
                        pkg.isPersistent = false;
                    }
                    else {
                        HeaderDef def = HeaderDef.getDefByName(hdrName);
                        /*
                         * RFC 2817 implementation where simulator is not acting
                         * as a forward proxy but as a reverse proxy mimicking
                         * an origin server and offloading SSL with shielded 
                         * servers acting on the X-Forwarded-Scheme header to 
                         * indicate whether or not the connection is secure.
                         * Therefore, this upgrade response is passed back to 
                         * the client and upgrading to TLS/SSL will take place
                         * between the client and the simulator and not between
                         * the simulator and the server. The latter will still
                         * take place over http but the X-Forwarded-Header will
                         * show as 'https' to the back-end servers.
                         */
                        if (def == HeaderDef.Upgrade) {
                            // upgrade header will already have been copied to 
                            // the buffer, we just need a Connection header to
                            // trigger its use by the recipient
                            pkg.headerBfr.append(new Header(HeaderDef.Connection, 
                                    "Upgrade"));
                        }
                        else if (def == HeaderDef.Extension) {
                            pkg.headerBfr.removeExtensionHeader(hdrName);
                        }
                        else {
                            pkg.headerBfr.removeHeader(def);
                        }
                    }
                }
            }
        });
    }

    public boolean appliesTo(String lcHeaderName, HttpPackageType reqType) {
        return HeaderDef.Connection.getLcName().equals(lcHeaderName);
    }

}
