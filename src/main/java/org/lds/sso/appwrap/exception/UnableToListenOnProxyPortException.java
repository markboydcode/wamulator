package org.lds.sso.appwrap.exception;

/**
 * Exception thrown if the simulator is unable to start the ProxyListener.
 * 
 * @author BOYDMR
 *
 */
public class UnableToListenOnProxyPortException extends Exception {

    /**
     * Allows for for serialize-ability.
     */
    private static final long serialVersionUID = 1L;

    public UnableToListenOnProxyPortException(String msg, Throwable t) {
        super(msg, t);
    }
}
