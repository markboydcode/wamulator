/**
 * 
 */
package org.lds.sso.appwrap;

import org.lds.sso.appwrap.AppEndPoint.InboundScheme;

/**
 * Marker class for unenforced URIs.
 * 
 * @author BoydMR
 *
 */
public class UnenforcedUri extends OrderedUri {

    public UnenforcedUri(InboundScheme scheme, String host, int port, String path,
            String query, String cpathDeclaration ) {
        super(scheme, host, port, path, query, cpathDeclaration);
    }
}