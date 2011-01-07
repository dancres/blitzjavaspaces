package org.dancres.blitz;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;

import net.jini.core.transaction.TransactionException;
import net.jini.space.JavaSpace;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.entry.*;

import org.dancres.blitz.notify.*;
import org.dancres.blitz.txn.TxnState;

import org.dancres.blitz.txnlock.*;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.util.Time;

class BulkTakeVisitor implements BulkMatchTask, SearchVisitor {
    private static final Logger theLogger =
        Logging.newLogger("org.dancres.blitz.BulkTakeVisitor");

    private MangledEntry[] theTemplates;
    private TxnState theTxnState;
    private long theLimit;
    private EventGeneratorImpl theSearchTask;
    private SearchVisitorAdapter theAdapter = new SearchVisitorAdapter();
    private BaulkedParty theParty;

    private ArrayList theEntries = new ArrayList();

    private int theStatus = SearchVisitor.TRY_AGAIN;

    private TransactionException theException;

    private boolean needsWakeup = false;

    BulkTakeVisitor(MangledEntry[] aTemplates, TxnState aTxnState,
                    long aLimit, VisitorBaulkedPartyFactory aFactory)
        throws IOException {

        theTemplates = aTemplates;
        theTxnState = aTxnState;
        theLimit = aLimit;
        theSearchTask = new EventGeneratorImpl(theTemplates);
        theParty = aFactory.newParty(this);
        SearchTasks.get().add(this);
        EventQueue.get().insert(theSearchTask);
    }

    private void resolved() {
        setStatus(STOP, new TransactionException());
    }

    public SearchVisitor getVisitor() {
        return theAdapter;
    }

    public int offer(SearchOffer anOffer) {
        theLogger.log(Level.FINE, "offer");

        synchronized(this) {
            if (haveCompleted()) {
                theLogger.log(Level.FINE, "Have completed");
                return STOP;
            }
        }

        OpInfo myInfo = anOffer.getInfo();
        MangledEntry myEntry = anOffer.getEntry();

        LockMgr myMgr = TxnLocks.getLockMgr(myInfo.getType());
        TxnLock myLock = myMgr.getLock(myInfo.getOID());

        synchronized (this) {
            int myResult;

            // Picked up enough matches in the meantime? Quit...
            if (haveCompleted())
                return STOP;

            VisitorBaulkedPartyFactory.Handback myHandback =
                new VisitorBaulkedPartyFactory.Handback(myInfo.getType(),
                    myInfo.getOID(), myEntry);

            synchronized (myLock) {
                myResult = myLock.acquire(theTxnState,
                    TxnLock.DELETE,
                    theParty, myHandback, false);
            }

            if (myResult == TxnLock.SUCCESS) {
                // Got the lock
                try {
                    theTxnState.add(new EntryTxnOp(TxnLock.DELETE,
                        myInfo,
                        myLock));
                } catch (TransactionException aTE) {
                    synchronized (myLock) {
                        myLock.release(theTxnState, TxnLock.DELETE);
                    }
                    return setStatus(STOP, aTE);
                }

                // Add the Entry to our list of matches
                theEntries.add(myEntry);

                /*
                  Picked up enough matches? Quit else carry on
                */
                if (haveCompleted())
                    return setStatus(STOP, null);
                else
                    return TRY_AGAIN;
            } else {
                /*
                 One of our templates matched but we didn't
                 get a lock.  No point in trying other templates
                 because even if they match we might get another
                 conflict.  We'll leave it to settle instead and
                 look for other matches.
                */
                return TRY_AGAIN;
            }
        }
    }

    public boolean isDeleter() {
        return true;
    }

    private boolean haveCompleted() {
        /*
          If we're blocking, then we'll unblock as soon as we get one
          match, otherwise, we want to be greedy and grab as many entries
          as our limit allows
        */
        if (needsWakeup) {
            return (theEntries.size() != 0) || (theStatus == STOP);
        } else {
            return (theEntries.size() == theLimit) || (theStatus == STOP);
        }
    }

    public synchronized List getEntries(long aTimeout)
        throws TransactionException, InterruptedException {

        if ((theEntries.size() == 0) && (aTimeout != 0)) {
            needsWakeup = true;

            long myCurrentTime = System.currentTimeMillis();
            long myExpiry = Time.getAbsoluteTime(myCurrentTime, aTimeout);

            while (true) {
                long myWait = myExpiry - myCurrentTime;

                if (myWait > 0)
                    wait(myWait);
                else
                    break;

                if (haveCompleted())
                    break;

                myCurrentTime = System.currentTimeMillis();
            }

            needsWakeup = false;
        }

        // We're returning - ensure we don't allow any more operations to
        // avoid doing a take we'll never return....
        setStatus(STOP, null);

        if (theException != null)
            throw theException;

        return theEntries;
    }

    /**
       We're greedy and always want more if some might be available.
       i.e. If we haven't got to the point where we've scanned the entire
       space (in which case user should be calling wouldBlock()) we want more!
     */
    public synchronized boolean wantsMore() {
        return (theEntries.size() < theLimit);
    }

    /**
       We only request block if we have no entries.
     */
    public synchronized boolean wouldBlock() {
        return (theEntries.size() == 0);
    }

    private boolean wasNotSatisfied() {
        return (theEntries.size() == 0);
    }
    
    private synchronized int setStatus(int aState, TransactionException aTE) {
        /*
         * Test only for STOP, do not use haveCompleted in this case
         * which can declare us completed and cause us to axit before
         * we actually set STOP.
         */
        if (theStatus == STOP)
            return STOP;
        
        theStatus = aState;
        theException = aTE;

        theSearchTask.taint(false);
        SearchTasks.get().remove(this, wasNotSatisfied());

        if (needsWakeup)
            notify();

        return theStatus;
    }

    private class EventGeneratorImpl extends EventGeneratorBase {
        private AtomicBoolean isTainted = new AtomicBoolean(false);
        private MangledEntry[] theTemplates;

        EventGeneratorImpl(MangledEntry[] aTemplates) {
            theTemplates = aTemplates;
        }

        public void assign(OID anOID) {
            theOID = anOID;
        }

        public long getStartSeqNum() {
            return 0;
        }

        public boolean isPersistent() {
            return false;
        }

        public long getSourceId() {
            return 0;
        }

        void taint(boolean signal) {
            if (!isTainted.compareAndSet(false, true))
                return;

            if (signal)
                setStatus(STOP, new TransactionException("Destroyed"));

            try {
                EventQueue.get().kill(this);
            } catch (IOException anIOE) {
                theLogger.log(Level.SEVERE,
                    "Encountered IOException during kill", anIOE);
            }

            /*
            try {
                Tasks.queue(new CleanTask(getId()));
            } catch (InterruptedException anIE) {
                theLogger.log(Level.WARNING,
                    "Failed to lodge cleanup for: " + getId(), anIE);
            }
            */
        }

        public void taint() {
            taint(true);
        }

        public boolean canSee(QueueEvent anEvent, long aTime) {
            if (isTainted.get())
                return false;

            // Check if it's txn_ended and my txn and call resolved if it is
            if ((anEvent.getType() == QueueEvent.TRANSACTION_ENDED) &&
                    (theTxnState.getId().equals(anEvent.getTxn().getId()))) {
                resolved();
                return false;
            }

            // We want to see new writes from a transaction
            //
            return (anEvent.getType() == QueueEvent.ENTRY_WRITE);
        }

        public boolean matches(MangledEntry anEntry) {
            if (isTainted.get())
                return false;

            for (int i = 0; i < theTemplates.length; i++) {
                MangledEntry myTemplate = theTemplates[i];

                if (Types.isSubtype(myTemplate.getType(), anEntry.getType())) {
                    if (myTemplate.match(anEntry))
                        return true;
                }
            }

            return false;
        }

        public boolean renew(long aTime) {
            // Nothing to do as we expire by being tainted by the enclosing
            // class only
            //
            return true;
        }

        public void recover(long aSeqNum) {
            // Nothing to do
        }

        public long jumpSequenceNumber() {
            return 0;
        }

        public long jumpSequenceNumber(long aMin) {
            return 0;
        }

        public void ping(QueueEvent anEvent, JavaSpace aSource) {
            if (isTainted.get())
                return;

            LongtermOffer myOffer = null;

            try {
                QueueEvent.Context myContext = anEvent.getContext();
                MangledEntry myEntry = myContext.getEntry();
                OID myOID = myContext.getOID();

                EntryRepository myRepos =
                    EntryRepositoryFactory.get().find(myEntry.getType());

                myOffer = myRepos.getOffer(myOID);

                if (myOffer == null)
                    return;

                myOffer.offer(BulkTakeVisitor.this);

            } catch (IOException anIOE) {
                // Nothing can be done
                theLogger.log(Level.SEVERE,
                    "Encountered IOException during write offer", anIOE);
            } finally {
                if (myOffer != null) {
                    try {
                        myOffer.release();
                    } catch (IOException anIOE) {
                        theLogger.log(Level.SEVERE,
                            "Encountered IOException during write offer(release)",
                            anIOE);
                    }
                }
            }
        }

        public EventGeneratorState getMemento() {
            throw new RuntimeException(
                "Shouldn't be happening - we're transient");
        }
    }

    private class SearchVisitorAdapter implements SearchVisitor {

        public boolean isDeleter() {
            return BulkTakeVisitor.this.isDeleter();
        }

        public int offer(SearchOffer anOffer) {
            if (theLogger.isLoggable(Level.FINE))
                theLogger.log(Level.FINE, "Offer");

            synchronized (this) {
                if (haveCompleted()) {
                    theLogger.log(Level.FINE, "Have completed");
                    return STOP;
                }
            }

            OpInfo myInfo = anOffer.getInfo();
            MangledEntry myEntry = anOffer.getEntry();

            for (int i = 0; i < theTemplates.length; i++) {
                MangledEntry myTemplate = theTemplates[i];

                if (Types.isSubtype(myTemplate.getType(), myInfo.getType())) {

                    if ((myTemplate.isWildcard()) ||
                        (myTemplate.match(myEntry))) {

                        // If we get a match, we only need to try offer
                        // once to see if we can lock the entry so we can
                        // give up after the first match & offer.
                        //
                        return BulkTakeVisitor.this.offer(anOffer);
                    }
                }
            }

            return TRY_AGAIN;
        }
    }
}