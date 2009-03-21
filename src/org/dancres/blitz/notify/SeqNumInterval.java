package org.dancres.blitz.notify;

import java.io.IOException;
import java.io.Serializable;

import java.util.logging.Level;

import org.dancres.blitz.txn.TxnState;
import org.dancres.blitz.txn.TxnOp;

import org.dancres.blitz.disk.DiskTxn;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.meta.RegistryFactory;
import org.dancres.blitz.meta.Registry;

/**
   Log action which restores a snapshotted sequence number to an event
   generator.  Rather than save an update to disk for each event we generate,
   we log a single change record after a fixed number of events have been
   dispatched.  In this way, we reduce I/O's and can still keep the
   EventGenerator sequence number up-to-date.  Note that in the case of
   restore from log, a <code>RemoteEventListener</code> will likely see a
   slightly bigger jump than it might expect - that's okay.  Note also that
   such restarts will "eat up" a few sequence numbers (i.e. they will never
   be used on a RemoteEvent) - that's okay too.
 */
class SeqNumInterval implements TxnOp {
    private OID theOID;
    private long theSeqNum;

    SeqNumInterval(OID aOID, long aSeqNum) {
        theOID = aOID;
        theSeqNum = aSeqNum;
    }

    public void commit(TxnState aState) throws IOException {
        // Nothing to do, we're just recording state
    }

    public void abort(TxnState aState) throws IOException {
        // Nothing to do, we're just recording state
    }

    public void restore(TxnState aState) throws IOException {
        EventQueue.theLogger.log(Level.FINE, "Patching notify reg" +
                                 theOID + ", " + theSeqNum);

        EventGeneratorFactory.get().recover(theOID, theSeqNum);
    }

    public String toString() {
        return " SN : " + theOID + " : " + theSeqNum;
    }
}
