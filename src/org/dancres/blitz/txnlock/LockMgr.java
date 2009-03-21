package org.dancres.blitz.txnlock;

import org.dancres.blitz.oid.OID;

/**
   A lock mgr instance is responsible for tracking all transaction locks
   for a particular Entry type.
 */
public class LockMgr {
    /**
       For as long as any transaction has a reference to the lock, it will
       be maintained by the LockMgr.  Obviously, if there are no pending
       transactions there's no need to hold the lock or it's state because
       the lock should be clear.  If it's not clear, we have a bug!!!! :)
     */
    // private SoftHashMap theLocks = new SoftHashMap();
    private LockCache theLocks = new LockCache();

    public TxnLock getLock(OID aOID) {
        return theLocks.getOrInsert(aOID);
    }

    public TxnLock newLock(OID aOID) {
        TxnLock myLock = new TxnLock();

        theLocks.put(aOID, myLock);

        return myLock;
    }

    /**
       Test to see if the specified OID has an active lock associated
       with it.  This is only truly useful in situations where the associated
       Entry is in an unchanging state perhaps because it has been DELETED
       or it's lease has expired.
     */
    public boolean hasActiveLock(OID aOID) {
        TxnLock myLock = (TxnLock) theLocks.get(aOID);
        
        if (myLock != null) {
            synchronized(myLock) {
                return myLock.isActive();
            }
        }
        
        return false;
    }

}
