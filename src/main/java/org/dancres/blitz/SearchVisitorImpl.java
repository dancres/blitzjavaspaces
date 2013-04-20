package org.dancres.blitz;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.core.lease.Lease;
import net.jini.core.transaction.TransactionException;
import net.jini.space.JavaSpace;

import org.dancres.blitz.entry.*;
import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.notify.*;
import org.dancres.blitz.oid.OID;
import org.dancres.blitz.txn.TxnState;
import org.dancres.blitz.txnlock.LockMgr;
import org.dancres.blitz.txnlock.TxnLock;
import org.dancres.blitz.txnlock.TxnLocks;
import org.dancres.blitz.txnlock.BaulkedParty;
import org.dancres.blitz.util.Time;

/**
   All search results obtained by the lower layers are offered to a
   SearchVisitor instance which can then determine whether the offered Entry
   is suitable.  This includes "deep matching" which requires that we fully
   compare the fields of template to entry.  The lower-layers do not perform
   this task - they return the entry's that are a probable match.  In
   addition, instances of search visitor check transaction locks etc. which
   the lower layers know nothing about.
 */
class SearchVisitorImpl implements SingleMatchTask,
    SearchVisitor {

    private static final Logger theLogger =
        Logging.newLogger("org.dancres.blitz.entry.SearchVisitorImpl");

    private MangledEntry theTemplate;
    private TxnState theTxnState;
    private boolean isTaking;
    private int theLockOp;

    private EventGeneratorImpl theSearchTask;
    private SearchVisitor theAdapter = new SearchVisitorAdapter();
    private BaulkedParty theParty;

    private CompletionEvent theCompletion;
    
    private boolean needsWakeup = false;

    /**
       @param isTake indicates the kind of txn lock we would need to assert
     */
    SearchVisitorImpl(MangledEntry aTemplate, boolean isTake,
                      TxnState aState, VisitorBaulkedPartyFactory aFactory)
        throws IOException {

        theTemplate = aTemplate;
        isTaking = isTake;
        theTxnState = aState;
        theLockOp = (isTaking == true) ? TxnLock.DELETE : TxnLock.READ;
        theSearchTask = new EventGeneratorImpl(aTemplate);
        theParty = aFactory.newParty(this);
        SearchTasks.get().add(this);
        EventQueue.get().insert(theSearchTask);
    }

    public EventGenerator getSearchTask() {
        return theSearchTask;
    }

    public SearchVisitor getVisitor() {
        return theAdapter;
    }

    /***********************************************************************
     * SearchVisitor
     ***********************************************************************/

    /**
     * This is an unfiltered offer method which only checks to see if it can
     * perform an acquire.  Actual matching is done in the Adapter class
     * or in the satellite EventGenerator implementation.  i.e.  By the time
     * we get to this method we need to ensure that matching has been done.
     *
     * @todo Consider re-arranging this method as described in the comment
     * below.
     */
    public int offer(SearchOffer anOffer) {
        OpInfo myInfo = anOffer.getInfo();

        MangledEntry myEntry = anOffer.getEntry();

        LockMgr myMgr = TxnLocks.getLockMgr(myInfo.getType());
        TxnLock myLock = myMgr.getLock(myInfo.getOID());

        synchronized (this) {

            int myResult;

            // Do we need to try and secure this?
            if (haveFinished())
                return STOP;

            VisitorBaulkedPartyFactory.Handback myHandback =
                new VisitorBaulkedPartyFactory.Handback(myInfo.getType(),
                    myInfo.getOID(), myEntry);

            synchronized (myLock) {
                myResult = myLock.acquire(theTxnState, theLockOp,
                    theParty, myHandback, false);
            }

            if (myResult == TxnLock.SUCCESS) {

                if (theLogger.isLoggable(Level.FINE))
                    theLogger.log(Level.FINE, theTxnState.getId() +
                        " Acq: " +
                        myInfo + ", " + myLock);

                try {
                    theTxnState.add(new EntryTxnOp(theLockOp, myInfo,
                            myLock));
                } catch (TransactionException aTE) {

                    /*
                        If we catch a transactionexception here we're dead,
                        we will exit with invalid state.  If we have the
                        lock above we know we have the Entry and no-one
                        can remove it from under us.

                        Once we've locked the entry we're done
                        matching because either we will return it
                        successfully after tracking our action in the
                        transaction or we will blow up on the transaction
                        and release the entry lock and return an exception.
                        Either way we can stop all searching as soon as
                        we have the Entry locked.

                        Thus we can set theEntry above once we see success
                        and then exit the sync block rather than holding
                        it whilst we update the transaction.  Before
                        exiting the sync block we need to check for
                        conflict as we do below and set the didconflict
                        flag.

                        Currently we stop searches and wakeup blocked
                        threads in getEntry via setStatus thus setting
                        theEntry is currently insufficient.  However
                        it would be safe to modify haveCompleted to also
                        include a test for theEntry being set.  Note we
                        must NEVER use haveCompleted in getEntry()
                        as a consequence!
                    */
                    myLock.release(theTxnState, theLockOp);
                    return sendEvent(new CompletionEvent(aTE));
                }

                if (theLogger.isLoggable(Level.FINE))
                    theLogger.log(Level.FINE, "Succeeded");

                return sendEvent(new CompletionEvent(myEntry));
            }
        }

        return TRY_AGAIN;
    }

    public int sendEvent(CompletionEvent anEvent) {
        synchronized (this) {
            if (haveFinished())
                return STOP;

            theCompletion = anEvent;

            theSearchTask.taint();
            SearchTasks.get().remove(this, wasNotSatisfied());

            if (needsWakeup)
                notify();

            return STOP;
        }
    }

    public synchronized MangledEntry getEntry(long aTimeout)
        throws TransactionException,
               InterruptedException {

        if (wouldBlock() && (aTimeout != 0)) {
            needsWakeup = true;

            long myCurrentTime = System.currentTimeMillis();
            long myExpiry = Time.getAbsoluteTime(myCurrentTime, aTimeout);

            while (true) {
                long myWait = myExpiry - myCurrentTime;

                if (myWait > 0)
                    wait(myWait);
                else
                    break;

                if (haveFinished())
                    break;

                myCurrentTime = System.currentTimeMillis();
            }

            needsWakeup = false;
        }

        // We're returning - ensure we don't allow any more operations to
        // avoid doing a take we'll never return....
        sendEvent(CompletionEvent.COMPLETED);

        if (theCompletion.getException() != null)
            throw theCompletion.getException();

        return theCompletion.getEntry();
    }

    public synchronized boolean wouldBlock() {
        return (theCompletion == null);
    }

    private synchronized boolean haveFinished() {
        return (theCompletion != null);
    }

    private boolean wasNotSatisfied() {
        return (theCompletion.getEntry() == null);
    }
    
    public boolean isDeleter() {
        return isTaking;
    }

    private void resolved() {
        sendEvent(new CompletionEvent(new TransactionException(
                "Transaction completed with operations still outstanding: " +
            (isTaking ? "take" : "read"))));
    }

    private class EventGeneratorImpl extends EventGeneratorBase {
        private AtomicBoolean isTainted = new AtomicBoolean(false);
        private MangledEntry theTemplate;

        EventGeneratorImpl(MangledEntry aTemplate) {
            theTemplate = aTemplate;
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

        public void taint() {
            if (!isTainted.compareAndSet(false, true))
                return;

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

        private boolean isTainted() {
            return isTainted.get();
        }

        public boolean canSee(QueueEvent anEvent, long aTime) {
            if (isTainted())
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
            if (isTainted())
                return false;

            return Types.isSubtype(theTemplate.getType(), anEntry.getType()) &&
                theTemplate.match(anEntry);
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
            if (isTainted())
                return;

            LongtermOffer myOffer = null;

            QueueEvent.Context myContext = anEvent.getContext();
            MangledEntry myEntry = myContext.getEntry();
            OID myOID = myContext.getOID();

            try {
                EntryRepository myRepos =
                    EntryRepositoryFactory.get().find(myEntry.getType());

                myOffer = myRepos.getOffer(myOID);

                if (myOffer == null)
                    return;

                myOffer.offer(SearchVisitorImpl.this);

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
            return SearchVisitorImpl.this.isDeleter();
        }

        public int offer(SearchOffer anOffer) {
            if (theLogger.isLoggable(Level.FINE))
                theLogger.log(Level.FINE, "Offer");

            if (haveFinished()) {
                if (theLogger.isLoggable(Level.FINE))
                    theLogger.log(Level.FINE, theTxnState.getId() +
                        " Have completed");
                return STOP;
            }

            OpInfo myInfo = anOffer.getInfo();

            if (!Types.isSubtype(theTemplate.getType(), myInfo.getType())) {
                if (theLogger.isLoggable(Level.FINE))
                    theLogger.log(Level.FINE, "Not subtype");

                return TRY_AGAIN;
            }

            MangledEntry myEntry = anOffer.getEntry();

            if (theTemplate.match(myEntry)) {
                return SearchVisitorImpl.this.offer(anOffer);
            } else
                return TRY_AGAIN;
        }
    }
}
