package org.dancres.blitz.remote.nio;

import org.dancres.blitz.mangler.MangledEntry;
import net.jini.core.transaction.Transaction;

/**
 */
public class GenericSpaceOp implements Operation {
    static final int WRITE = 1;
    static final int READ = 2;
    static final int TAKE = 3;
    static final int TAKE_EXISTS = 4;
    static final int READ_EXISTS = 5;
    
    private int _operation;
    private MangledEntry _entry;
    private Transaction _txn;
    private long _lease;

    public GenericSpaceOp(int anOp, MangledEntry anEntry,
                          Transaction aTxn, long aLease) {
        _operation = anOp;
        _entry = anEntry;
        _txn = aTxn;
        _lease = aLease;
    }

    int getOperation() {
        return _operation;
    }

    MangledEntry getEntry() {
        return _entry;
    }

    Transaction getTxn() {
        return _txn;
    }

    long getLease() {
        return _lease;
    }
}
