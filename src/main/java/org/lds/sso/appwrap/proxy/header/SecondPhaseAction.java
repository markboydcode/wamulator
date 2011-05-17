package org.lds.sso.appwrap.proxy.header;


/**
 * Implementation of action to be taken during the second phase of header
 * handling. Inline classes override the apply() method creating a closure to
 * operate on objects visible during the first phase but unalterable during 
 * that phase like the package's HeaderBuffer which will not have all headers
 * loaded at that point while the action to be taken is to remove a specific
 * header. An example is the Connection header whose tokens are the names of 
 * other headers.
 */
public abstract class SecondPhaseAction {

    /**
     * Implementation of some contingent way of handling a specific header in a
     * request or response during the second pass over the package's headers.
     * Unlike the first pass which is over the raw headers as they are read line
     * by line this phase passes over their Header objects and can choose to
     * act by removing them from the buffer or altering the package in some way.
     * 
     * WARNING: do NOT make any changes on the package's headerBfr object since
     * this method is called via that collection's iterator. Instead, return the
     * appropriate objects that convey headers to be added to the buffer or 
     * removed therefrom so that they can be handled after iteration over the
     * buffer contents is completed.
     * 
     * @param line
     * @param pkg
     */
    public abstract void apply();
}
