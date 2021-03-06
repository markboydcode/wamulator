package org.lds.sso.appwrap.exception;

/**
 * Exception thrown if the simulator is unable to start the Jetty server.
 * 
 * @author BOYDMR
 *
 */
public class UnableToStartJettyServerException extends Exception {

    /**
     * Allows for for serialize-ability.
     */
    private static final long serialVersionUID = 1L;

    public UnableToStartJettyServerException(Throwable t) {
        super(t);
    }
}
