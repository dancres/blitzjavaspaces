package org.dancres.blitz.notify;

import java.io.IOException;

import org.dancres.blitz.txn.TxnOp;
import org.dancres.blitz.txn.TxnState;

import org.dancres.blitz.oid.OID;

/**
   An instance of this class is saved to the log whenever we apply a
   restart jump to an EventGenerator.  When we restart, a jump must be applied
   to each persisted Generator which yields one SequenceJumpRecord per
   jump applied.  The presence of this record ensures that:

   <ol>
   <li>If we have to restore state entirely from log files, the EventGenerator
   will still have an appropriate sequence number.</li>
   <li>If we crash again before the EventGenerator is sync'd to disk, via a
   checkpoint, the sequence number will still be accurate.</li>
   </ol>
 */
class SequenceJumpRecord implements TxnOp {
    private OID theOID;
    private long theSeqNum;

    SequenceJumpRecord(OID aOID, long aSeqNum) {
        theOID = aOID;
        theSeqNum = aSeqNum;
    }

    public void commit(TxnState aState) throws IOException {
        // Nothing to do
    }

    public void abort(TxnState aState) throws IOException {
        // Nothing to do
    }

    public void restore(TxnState aState) throws IOException {
        EventGeneratorFactory.get().jumpSequenceNumber(theOID, theSeqNum);
    }

    public String toString() {
        return " RJ : " + theOID + " : " + theSeqNum;
    }
}
