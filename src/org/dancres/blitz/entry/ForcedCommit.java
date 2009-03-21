package org.dancres.blitz.entry;

import java.io.IOException;

import org.dancres.blitz.txn.TxnOp;
import org.dancres.blitz.txn.TxnState;

class ForcedCommit implements TxnOp {
    private OpInfo theInfo;

    ForcedCommit(OpInfo anInfo) {
        theInfo = anInfo;
    }

    public void restore(TxnState aState) throws IOException {
        theInfo.restore();
    }

    public void commit(TxnState aState) throws IOException {
        theInfo.commit(aState);
    }

    public void abort(TxnState aState) throws IOException {
        theInfo.abort(aState);
    }

    public String toString() {
        return " FC : " + theInfo;
    }
}