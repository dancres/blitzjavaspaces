package org.dancres.blitz.remote.txn;

/**
 */
public class TxnDetails {
    private long theExpiry;

    TxnDetails(long anExpiry) {
        theExpiry = anExpiry;
    }

    boolean hasExpired(long aTime) {
        return (theExpiry < aTime);
    }

    boolean testAndSetExpiry(long aTime, long anExpiry) {
        if (theExpiry < aTime)
            return false;

        theExpiry = anExpiry;

        return true;
    }
}
