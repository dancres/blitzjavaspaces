package org.dancres.blitz;

import org.dancres.blitz.lease.ReapFilter;
import org.dancres.blitz.lease.LeasedResource;

import org.dancres.blitz.entry.EntrySleeve;

import org.dancres.blitz.txnlock.*;

/**
   Blocks collection of EntrySleeve instances which are currently held under
   some sort of transaction lock.
 */
public class TxnReapFilter implements ReapFilter {
    public boolean filter(LeasedResource anObject) {
        if (anObject instanceof EntrySleeve) {
            EntrySleeve mySleeve = (EntrySleeve) anObject;

            LockMgr myMgr = 
                TxnLocks.getLockMgr(mySleeve.getEntry().getType());

            // System.err.println("HasLock: " +
            //                    myMgr.hasActiveLock(mySleeve.getOID()));

            return myMgr.hasActiveLock(mySleeve.getOID());
        }

        // We don't mind if this object is reaped - return "not filtered"
        return false;
    }
}
