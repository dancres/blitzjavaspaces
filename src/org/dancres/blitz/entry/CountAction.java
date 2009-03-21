package org.dancres.blitz.entry;

import java.io.IOException;

import org.dancres.blitz.txn.TxnOp;
import org.dancres.blitz.txn.TxnState;

/**
   If logging of instance counts is enabled we will emit one of these to
   the log file as part of the first destructive operation we perform post
   reloading/initialization of the relevant <code>EntryRepository</code>.
 */
class CountAction implements TxnOp {
    private int theInstanceCount;
    private String theType;

    CountAction(String aType, int aCount) {
        theInstanceCount = aCount;
        theType = aType;
    }

    public void restore(TxnState aState) throws IOException {
        // Informational - nothing to do
    }

    public void commit(TxnState aState) throws IOException {
        // Informational - nothing to do
    }

    public void abort(TxnState aState) throws IOException {
        // Informational - nothing to do
    }

    public String toString() {
        return " IC : " + theType + " : " + theInstanceCount;
    }
}