package org.dancres.blitz.stats;

/**
   <p>Tracks transaction statistics.  This stat is permanently enabled and
   cannot be switched on or off.  It's maintenance costs are entirely absorbed
   by the caller recovering stats from StatsBoard.</p>

   @see org.dancres.blitz.txn.TxnStatGenerator
 */
public class TxnStat implements Stat {
    private long theId;

    private int theActiveTxnCount;

    public TxnStat(long anId, int aTxnCount) {
        theId = anId;
        theActiveTxnCount = aTxnCount;
    }

    public long getId() {
        return theId;
    }

    public int getActiveTxnCount() {
        return theActiveTxnCount;
    }

    public String toString() {
        return "Active Txns: " + theActiveTxnCount;
    }
}
