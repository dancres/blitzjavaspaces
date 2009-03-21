package org.dancres.blitz.txn;

import org.dancres.blitz.stats.TxnStat;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.StatGenerator;

/**
   Registered with the StatsBoard to generate statistics for transaction
   state.

   @see org.dancres.blitz.stats.TxnStat
 */
public class TxnStatGenerator implements StatGenerator {
    private long theId = StatGenerator.UNSET_ID;

    TxnStatGenerator() {
    }

    public void setId(long anId) {
        theId = anId;
    }

    public long getId() {
        return theId;
    }

    public synchronized Stat generate() {
        TxnStat myStat = new TxnStat(theId,
                                     TxnManager.get().getActiveTxnCount());

        return myStat;
    }
}
