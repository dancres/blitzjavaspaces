package org.dancres.blitz.txnlock;

import java.util.HashMap;

/**
   Contains lockmanagers split and indexed by Entry type.
 */
public class TxnLocks {

    private static HashMap theLockMgrs = new HashMap();

    public static LockMgr getLockMgr(String aType) {
        synchronized(theLockMgrs) {
            LockMgr myMgr = (LockMgr) theLockMgrs.get(aType);

            if (myMgr == null) {
                myMgr = new LockMgr();
                theLockMgrs.put(aType, myMgr);
            }

            return myMgr;
        }
    }
}
