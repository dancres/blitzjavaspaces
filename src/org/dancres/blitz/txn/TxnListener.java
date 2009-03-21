package org.dancres.blitz.txn;

/**
   Notify'd when transaction's state is resolved (that is committed or
   aborted).

   @see org.dancres.blitz.txn.TxnState
 */
public interface TxnListener {
    public void resolved(TxnState aState);
}
