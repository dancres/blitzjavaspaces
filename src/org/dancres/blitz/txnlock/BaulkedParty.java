package org.dancres.blitz.txnlock;

/**
 * <code>TxnLock</code> reports conflicts and their resolution to instances of
 * this interface.
 */
public interface BaulkedParty {
    public void blocked(Object aHandback);
    public void unblocked(Object aHandback);
}
