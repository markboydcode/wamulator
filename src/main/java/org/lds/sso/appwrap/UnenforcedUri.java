/**
 * 
 */
package org.lds.sso.appwrap;

/**
 * Marker class for unenforced URIs.
 * 
 * @author BoydMR
 *
 */
public class UnenforcedUri extends OrderedUri {

    public UnenforcedUri(String scheme, String host, int port, String path,
            String query, String cpathDeclaration ) {
        super(scheme, host, port, path, query, cpathDeclaration);
    }
}