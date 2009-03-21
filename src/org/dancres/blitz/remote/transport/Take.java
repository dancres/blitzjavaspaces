package org.dancres.blitz.remote.transport;

import java.io.Serializable;

import net.jini.core.transaction.Transaction;

import org.dancres.blitz.mangler.MangledEntry;

/**
 */
public class Take implements Serializable {
    private MangledEntry _entry;
    private Transaction _txn;
    private long _waitTime;

    public MangledEntry getEntry() {
        return _entry;
    }

    public Transaction getTxn() {
        return _txn;
    }

    public long getWaitTime() {
        return _waitTime;
    }

    public Take(MangledEntry anEntry, Transaction aTxn, long aWaitTime) {
        _entry = anEntry;
        _txn = aTxn;
        _waitTime = aWaitTime;
    }
}
