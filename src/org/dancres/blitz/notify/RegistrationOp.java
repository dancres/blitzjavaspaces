package org.dancres.blitz.notify;

import java.io.IOException;

import org.dancres.blitz.txn.TxnOp;
import org.dancres.blitz.txn.TxnState;

/**
   Every persistent (those that aren't transaction-related) notify registration
   is recorded in the log for purposes of recovery.
 */
class RegistrationOp implements TxnOp {
    private EventGeneratorState theState;

    RegistrationOp(EventGeneratorState aState) {
        theState = aState;
    }

    public void commit(TxnState aState) throws IOException {
        // Nothing to do
    }

    public void abort(TxnState aState) throws IOException {
        // Nothing to do
    }

    public void restore(TxnState aState) throws IOException {
        EventGeneratorFactory.get().recover(theState);
    }

    public String toString() {
        return " NO : " + theState.toString();
    }
}
