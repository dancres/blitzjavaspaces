package org.dancres.blitz.txn;

import java.io.Serializable;
import java.io.IOException;

import org.dancres.blitz.entry.OpInfo;

/**
   <p>
   Each separate operation of a Transaction identified by TxnId is a TxnOp
   which should implement this interface.  TxnOp's are repeatable, disk-level
   actions which can be logged to disk and re-applied during system recovery.
   </p>
 */
public interface TxnOp extends Serializable {
    /**
       Must be invoked inside an active DiskTxn.
     */
    public void restore(TxnState aState) throws IOException;
    public void commit(TxnState aState) throws IOException;
    public void abort(TxnState aState) throws IOException;
}
