package org.dancres.blitz.txn;

import java.io.IOException;

/**
   The checkpointer implementation is deliberately separated out from
   TxnManager itself so that the actual method which performs the checkpoint
   [<code>issueCheckpoint()</code>] is not publicly visible.
 */
class CheckpointerImpl implements Checkpointer {
    private TxnManager theManager;

    CheckpointerImpl(TxnManager aManager) {
        theManager = aManager;
    }

    public void sync() throws IOException {
        theManager.requestAsyncCheckpoint();
    }
}
