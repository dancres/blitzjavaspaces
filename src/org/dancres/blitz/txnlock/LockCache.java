package org.dancres.blitz.txnlock;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

import org.dancres.blitz.oid.OID;

class LockCache {
    private ConcurrentHashMap<OID, LockHolder> theLocks = new ConcurrentHashMap();

    private ReferenceQueue theDeadLocks = new ReferenceQueue();

    LockCache() {
    }

    TxnLock getOrInsert(OID aOID) {
        cleanQueue();

        TxnLock myLock = getImpl(aOID);

        if (myLock != null) {
            return myLock;
        } else {
            TxnLock myNewLock = new TxnLock();
            LockHolder myNewHolder = new LockHolder(aOID, myNewLock, theDeadLocks);

            do {
                LockHolder myCurrentHolder = theLocks.putIfAbsent(aOID, myNewHolder);

                if (myCurrentHolder != null) {
                    TxnLock myCurrentLock = (TxnLock) myCurrentHolder.get();

                    if (myCurrentLock == null) {
                        theLocks.remove(aOID, myCurrentHolder);
                    } else {
                        myLock = myCurrentLock;
                    }
                } else {
                    myLock = myNewLock;
                }
            } while (myLock == null);

            return myLock;
        }
    }
    
    TxnLock get(OID aOID) {
        cleanQueue();
        return getImpl(aOID);
    }

    private TxnLock getImpl(OID anOID) {
        LockHolder myHolder = theLocks.get(anOID);
        return (myHolder == null) ? null : (TxnLock) myHolder.get();
    }

    void put(OID aOID, TxnLock aLock) {
        cleanQueue();

        LockHolder myHolder = new LockHolder(aOID, aLock, theDeadLocks);
        theLocks.put(aOID, myHolder);
    }

    private void cleanQueue() {
        LockHolder myRef;
        
        while ((myRef = (LockHolder) theDeadLocks.poll()) != null) {
            theLocks.remove(myRef.getOID(), myRef);
        }
    }

    private class LockHolder extends WeakReference {
        private OID theOID;

        LockHolder(OID aOID, TxnLock aLock, ReferenceQueue aQueue) {
            super(aLock, aQueue);
            theOID = aOID;
        }

        OID getOID() {
            return theOID;
        }
    }
}
