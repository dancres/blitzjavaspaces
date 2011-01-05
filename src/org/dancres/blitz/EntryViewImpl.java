package org.dancres.blitz;

import java.io.IOException;
import java.util.Set;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.UnknownTransactionException;

import org.dancres.blitz.entry.*;
import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.txn.TxnManager;
import org.dancres.blitz.txn.TxnState;
import org.dancres.blitz.txnlock.LockMgr;
import org.dancres.blitz.txnlock.TxnLock;
import org.dancres.blitz.txnlock.TxnLocks;
import org.dancres.blitz.notify.EventQueue;

/**
   <p>This class supports both iteration using the old JavaSpaceAdmin interface
   and the new JavaSpace05 interface.  The key differences amount to the
   the number of templates that can be passed and whether or not locks are
   held on the transaction.</p>

   <p>For JavaSpace05, there are multiple templates and we hold locks on the
   transaction.</p>
 */
class EntryViewImpl implements EntryView {
    private TxnState theTxn;

    private TransactionException theException;
    private int theStatus = ACTIVE;

    private static final int ACTIVE = -1;
    private static final int DECEASED = -2;

    private UIDSet theUIDs;

    private NewView theDynamicView;

    private EntryRx theBuffer;

    private boolean shouldUpdate;

    EntryViewImpl(Transaction aTxn, MangledEntry[] aTemplates,
                  boolean holdLocks, boolean doUpdate, long aLimit)
            throws TransactionException, IOException {

        shouldUpdate = doUpdate;

        theTxn = TxnManager.get().resolve(aTxn);

        /*
          Basic process is to assemble a set of fully matching Entry UID's which we
          then step through ascertaining whether these UID's are still valid and possibly
          asserting locks transactionally.

          Full matching is done in each view and the matching tuples are then
          logged/merged in the UIDSet

          These tuple ids are then scanned through using EntryReposImpl::find to load
          the entry, verify it's valid etc after which we may or may not maintain a
          transaction lock (these steps are done in EntryRx).
         */
        theUIDs = new UIDSet(aLimit);

        theBuffer = new EntryRx(theTxn, holdLocks);
        theDynamicView = new NewView(this, aTemplates, theUIDs);

        if (shouldUpdate)
            EventQueue.get().insert(theDynamicView.getSearchTask());

        /*
          For each template do a full tree search and assemble all matches
         */
        for (int i = 0; i < aTemplates.length; i++) {
            if (theUIDs.isFull())
                break;

            DiskView myFixedView = new DiskView(aTemplates[i], theUIDs);

            EntryRepository myRepos =
                    EntryRepositoryFactory.get().find(aTemplates[i].getType());

            if (myRepos != null) {
                myRepos.find(aTemplates[i], myFixedView);

                // Try subtypes
                Set<String> mySubtypes = myRepos.getSubtypes();

                for (String t: mySubtypes) {
                    myRepos = EntryRepositoryFactory.get().find(t);

                    if (myRepos != null) {
                        myRepos.find(aTemplates[i], myFixedView);
                    }
                }
            }
        }
    }

    TxnState getTxn() {
        return theTxn;
    }
    
    void resolved() {
        setStatus(DECEASED, 
                new TransactionException(
                "Transaction closed with view active"));
    }

    private void setStatus(int aStatus, TransactionException aTE) {
        // Make sure we only do this once
        //
        synchronized(this) {
            if (theStatus == DECEASED)
                return;

            theStatus = aStatus;
            theException = aTE;
        }

        if (shouldUpdate)
            theDynamicView.getSearchTask().taint();

        if (theTxn.isNull()) {
            try {
                TxnManager.get().prepareAndCommit(theTxn);
            } catch (UnknownTransactionException aUTE) {
                /*
                  Don't care much...if we got here, we're defining state
                  and everyone else will fail at the status test above
                */
                synchronized(this) {
                    theException = aUTE;
                }
            }
        }
    }

    public void close() {
        setStatus(DECEASED, null);
    }

    public EntryChit next() throws TransactionException, IOException {
        synchronized(this) {
            if (theStatus == DECEASED) {
                if (theException != null)
                    throw theException;
                else
                    throw new TransactionException("No longer active");
            }
        }

        SpaceEntryUID myUID;

        while ((myUID = theUIDs.pop()) != null) {
            EntryRepository myRepos =
                EntryRepositoryFactory.get().get(myUID.getType());

            myRepos.find(theBuffer, myUID.getOID(), null);

            MangledEntry myEntry = theBuffer.getEntry();

            if (myEntry != null)
                return new EntryChit(myEntry, myUID);
        }

        return null;
    }

    /**
       Used to receive the entry requested by id from the EntryRepository
       It performs the appropriate locking checks and updates the transaction
       if we're holding locks
     */
    private static class EntryRx implements SearchVisitor {
        private MangledEntry theEntry;

        private TxnState theTxn;
        private boolean keepLock;

        private TransactionException theFailure;

        EntryRx(TxnState aTxn, boolean holdLock) {
            theTxn = aTxn;
            keepLock = holdLock;
        }

        public int offer(SearchOffer anOffer) {
            OpInfo myInfo = anOffer.getInfo();

            // Try the lock
            LockMgr myMgr = TxnLocks.getLockMgr(myInfo.getType());
            TxnLock myLock = myMgr.getLock(myInfo.getOID());
            
            int myResult;
            
            synchronized(myLock) {
                myResult = myLock.acquire(theTxn, TxnLock.READ,
                                          null, null, false);
            }
            
            if (myResult == TxnLock.SUCCESS) {
                if (keepLock) {
                    /*
                      Need to track the lock under the transaction
                     */
                    try {
                        theTxn.add(new EntryTxnOp(TxnLock.READ, myInfo,
                                                  myLock));
                    } catch (TransactionException aTE) {
                        myLock.release(theTxn, TxnLock.READ);
                        theFailure = aTE;
                        return STOP;
                    }
                } else {
                    /*
                      No need to track this lock, we were just testing so
                      we can release it now
                     */
                    myLock.release(theTxn, TxnLock.READ);
                }
                
                theEntry = anOffer.getEntry();
            }

            return STOP;
        }
        
        public boolean isDeleter() {
            return false;
        }

        MangledEntry getEntry() throws TransactionException {
            /*
              If we're running under an external (non-null) transaction
              we may have failed whilst attempting to lock (we would have
              failed to record the lock in the transaction).
             */
            if (theFailure != null)
                throw theFailure;

            MangledEntry myResult = theEntry;
            theEntry = null;

            return myResult;
        }
    }
}