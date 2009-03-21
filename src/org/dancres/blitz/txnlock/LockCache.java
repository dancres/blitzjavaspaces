package org.dancres.blitz.txnlock;

import java.util.HashMap;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import org.dancres.blitz.oid.OID;

class LockCache {
    private HashMap theLocks = new HashMap();

    private ReferenceQueue theDeadLocks = new ReferenceQueue();

    LockCache() {
    }

    TxnLock getOrInsert(OID aOID) {
        cleanQueue();
        
        synchronized(this) {
            TxnLock myLock = get(aOID);
            
            if (myLock == null) {
                myLock = new TxnLock();
                put(aOID, myLock);
            }
            
            return myLock;
        }
    }
    
    TxnLock get(OID aOID) {
        cleanQueue();

        synchronized(this) {
            LockHolder myHolder = (LockHolder) theLocks.get(aOID);
            
            return (myHolder == null) ? null : (TxnLock) myHolder.get();
        }
    }

    void put(OID aOID, TxnLock aLock) {
        cleanQueue();

        synchronized(this) {
            LockHolder myHolder = new LockHolder(aOID, aLock, theDeadLocks);
            
            theLocks.put(aOID, myHolder);
        }
    }

    private void cleanQueue() {
        LockHolder myRef;
        
        while ((myRef = (LockHolder) theDeadLocks.poll()) != null) {
            synchronized(this) {
                LockHolder myOther = (LockHolder) theLocks.remove(myRef.getOID());
                
                /*
                   Check that the reference we're releasing is the same as the
                   one we currently have in the table.  Otherwise:

                   It could be that get(OID) was called above and the holder was
                   recovered but it's reference had been cleared resulting in
                   allocation of a new lock BEFORE we've processed the reference
                   from the queue.  Thus we allocate the new lock, the old
                   reference (from get(OID)) is enqueued and we then delete the
                   new lock - oops!
                 */
                if ((myOther != null) &&(! myOther.equals(myRef))) {
                    theLocks.put(myOther.getOID(), myOther);
                }
            }
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
