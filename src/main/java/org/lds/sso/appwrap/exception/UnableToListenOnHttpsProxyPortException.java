package org.lds.sso.appwrap.exception;

/**
 * Exception thrown if the simulator is unable to start the HttpsProxyListener.
 * 
 * @author BOYDMR
 *
 */
public class UnableToListenOnHttpsProxyPortException extends Exception {

    /**
     * Allows for for serialize-ability.
     */
    private static final long serialVersionUID = 1L;

    public UnableToListenOnHttpsProxyPortException(Throwable t) {
        super(t);
    }
}
