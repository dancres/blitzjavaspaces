package org.dancres.blitz;

import java.io.IOException;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.entry.OpInfo;

import org.dancres.blitz.txn.TxnOp;
import org.dancres.blitz.txn.TxnState;

import org.dancres.blitz.txnlock.*;

import org.dancres.blitz.notify.QueueEvent;
import org.dancres.blitz.notify.EventQueue;

/**
   Each operation against an Entry is represented by an instance of this class.
   These are stored in <code>TxnState</code> instances and constitute part of
   the record of operations performed by a particular transaction.

   @see org.dancres.blitz.txn.TxnState

   @todo As postEvent happens before txnlock is released one can get
   temporary conflicts which could be alleviated if txnlock was released first
   but to do this would mean needing a snapshot of txnlock state from before
   the release was performed.
 */
class EntryTxnOp implements TxnOp {
    static final long serialVersionUID = -347168809827671347L;

    /**
       Required when we figure out what lock type to assert.
       Lock is transient because it's a memory only structure so we will
       have to recover it if necessary.
    */
    private int theOp;
    private OpInfo theInfo;

    private transient TxnLock theTxnLock;

    EntryTxnOp(OpInfo anInfo) {
        theInfo = anInfo;
    }

    EntryTxnOp(int anOp, OpInfo anInfo, TxnLock aLock) {
        theOp = anOp;
        theInfo = anInfo;
        theTxnLock = aLock;
    }

    public void restore(TxnState aState) throws IOException {
        if (theInfo.isDebugOp())
            return;

        theInfo.restore();

        LockMgr myMgr = TxnLocks.getLockMgr(theInfo.getType());
        theTxnLock = myMgr.getLock(theInfo.getOID());

        synchronized(theTxnLock) {
            theTxnLock.acquire(aState, theOp, null, null, true);
        }
    }

    public void commit(TxnState aState) throws IOException {
        if (theInfo.isDebugOp())
            return;

        MangledEntry myEntry = theInfo.commit(aState);

        postEvent(aState, myEntry, true);

        theTxnLock.release(aState, theOp);
    }

    public void abort(TxnState aState) throws IOException {
        if (theInfo.isDebugOp())
            return;

        MangledEntry myEntry = theInfo.abort(aState);

        postEvent(aState, myEntry, false);

        theTxnLock.release(aState, theOp);
    }

    public String toString() {
        return theInfo.toString();
    }

    private void postEvent(TxnState aState, MangledEntry anEntry,
                           boolean isCommit) {

        // No Entry means no Event - if Entry is present, we then need to
        // figure out whether or not we generate an event based on commit/abort
        // and the kind of operation
        if (anEntry == null)
            return;

        switch (theOp) {
            case TxnLock.READ : {
                if (theTxnLock.hasOnly(aState.getId(), TxnLock.READ)) {
                    // Entry still exists and we are the last outstanding
                    // read - which means we resolve a conflict
                    QueueEvent myEvent =
                        new QueueEvent(QueueEvent.ENTRY_NOT_CONFLICTED, aState,
                                       new QueueEvent.Context(anEntry,
                                           theInfo.getOID()));
            
                    EventQueue.get().add(myEvent);        
                }

                break;
            }

            case TxnLock.DELETE : {
                if (!isCommit) {
                    // We're aborting a take, if we wrote this Entry
                    // we shouldn't generate an Event
                    if (!theTxnLock.hasWriter(aState.getId())) {
                        QueueEvent myEvent =
                            new QueueEvent(QueueEvent.ENTRY_VISIBLE, aState,
                                new QueueEvent.Context(anEntry,
                                    theInfo.getOID()));

                        EventQueue.get().add(myEvent);
                    }
                }
                break;
            }

            /**
             * If we've been doing a write, the Entry is pinned and cannot
             * be flushed from cache thus even if it becomes lease expired
             * it will be present in cache when we query it.
             */
            case TxnLock.WRITE : {
                if (isCommit) {
                    // If we're commiting a write, an Entry became visible
                    QueueEvent myEvent =
                        new QueueEvent(QueueEvent.ENTRY_WRITTEN,
                            aState,
                            new QueueEvent.Context(anEntry, theInfo.getOID()));
                    EventQueue.get().add(myEvent);
                }
                break;
            }
        }
    }
}
