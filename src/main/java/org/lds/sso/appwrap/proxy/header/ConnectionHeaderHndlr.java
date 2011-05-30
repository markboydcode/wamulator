package org.lds.sso.appwrap.proxy.header;

import org.lds.sso.appwrap.Config;
import org.lds.sso.appwrap.proxy.HttpPackage;
import org.lds.sso.appwrap.proxy.HttpPackageType;

/**
 * Implements RFC 2616 section 14.10 for proper handling of the Connection
 * header by removing all headers indicated in the Connection header save for 
 * the 'close' token which marks the connection as non-persistent. Also has 
 * support for RFC 2817 if browsers ever support it letting the Upgrade token
 * of Connection pass through.
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
                /*
                 * Until the wamulator supports persistent connections we force
                 * a connection close header in both directions.
                 */
                pkg.headerBfr.removeHeader(HeaderDef.Connection);
                boolean injected = false;
                
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
                                    "Upgrade close"));
                            injected = true;
                        }
                        else if (def == HeaderDef.Extension) {
                            pkg.headerBfr.removeExtensionHeader(hdrName);
                        }
                        else {
                            pkg.headerBfr.removeHeader(def);
                        }
                    }
                }
                if (!injected) {
                    pkg.headerBfr.append(new Header(HeaderDef.Connection, "close"));
                    pkg.isPersistent = false;
                }
            }
        });
    }

    public boolean appliesTo(String lcHeaderName, HttpPackageType reqType) {
        return HeaderDef.Connection.getLcName().equals(lcHeaderName);
    }

}
