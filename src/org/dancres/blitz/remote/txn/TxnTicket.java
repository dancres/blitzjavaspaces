package org.dancres.blitz.remote.txn;

/**
 */
public class TxnTicket {
    private SpaceTxnUID theUID;
    private long theExpiryTime;

    TxnTicket(SpaceTxnUID aUID, long aLeaseTime) {
        theUID = aUID;
        theExpiryTime = aLeaseTime;
    }

    public SpaceTxnUID getUID() {
        return theUID;
    }

    public long getLeaseTime() {
        return theExpiryTime;
    }
}
