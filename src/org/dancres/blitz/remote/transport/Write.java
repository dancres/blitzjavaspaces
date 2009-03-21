package org.dancres.blitz.remote.transport;

import java.io.Serializable;

import net.jini.core.transaction.Transaction;

import org.dancres.blitz.mangler.MangledEntry;

/**
 */
public class Write implements Serializable {
    private long _leaseTime;
    private MangledEntry _entry;
    private Transaction _txn;

    public Write(MangledEntry anEntry, Transaction aTxn, long aLeaseTime) {
        _leaseTime = aLeaseTime;
        _entry = anEntry;
        _txn = aTxn;
    }

    public MangledEntry getEntry() {
        return _entry;
    }

    public Transaction getTxn() {
        return _txn;
    }

    public long getLeaseTime() {
        return _leaseTime;
    }
}
