package org.dancres.blitz;

import java.io.IOException;

import java.util.LinkedList;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;

import net.jini.core.transaction.TransactionException;
import net.jini.space.JavaSpace;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.entry.*;

import org.dancres.blitz.txn.TxnState;

import org.dancres.blitz.txnlock.*;

import org.dancres.blitz.oid.OID;
import org.dancres.blitz.notify.*;


/**
   <p>All search results obtained by the lower layers are offered to a
   SearchVisitor instance which can then determine whether the offered Entry
   is suitable.  This includes "deep matching" which requires that we fully
   compare the fields of template to entry.  The lower-layers do not perform
   this task - they return the entry's that are a probable match.  In
   addition, instances of search visitor check transaction locks etc. which
   the lower layers know nothing about.</p>

   <p>FifoSearchVisitorImpl enforces some ordering requirements that aren't
   present in SearchVisitorImpl relating to fairness which is sometimes more
   important than speed.</p>
 */
class FifoSearchVisitorImpl implements SingleMatchTask,
    SearchVisitor {

    private static final Logger theLogger =
        Logging.newLogger("org.dancres.blitz.entry.FifoSearchVisitorImpl");

    private MangledEntry theTemplate;
    private TxnState theTxnState;
    private boolean isTaking;

    private EventGeneratorImpl theSearchTask;
    private SearchVisitor theAdapter = new SearchVisitorAdapter();
    private BaulkedParty theParty;

    private int theLockOp;

    private boolean needsWakeup = false;

    private CompletionEvent theCompletion;
    
    private long theStartTime = System.currentTimeMillis();

    private LinkedList theNewWrites = new LinkedList();

    /**
       @param isTake indicates the kind of txn lock we would need to assert
     */
    FifoSearchVisitorImpl(MangledEntry aTemplate, boolean isTake,
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

    public int offer(SearchOffer anOffer) {
        OpInfo myInfo = anOffer.getInfo();

        MangledEntry myEntry = anOffer.getEntry();

        LockMgr myMgr = TxnLocks.getLockMgr(myInfo.getType());
        TxnLock myLock = myMgr.getLock(myInfo.getOID());

        synchronized (this) {

            int myResult;

            // Do we need to try and secure this?
            if (haveCompleted())
                return STOP;

            VisitorBaulkedPartyFactory.Handback myHandback =
                new VisitorBaulkedPartyFactory.Handback(myInfo.getType(),
                    myInfo.getOID(), myEntry);

            synchronized (myLock) {
                myResult = myLock.acquire(theTxnState, theLockOp,
                    theParty, myHandback, false);
            }

            if (myResult == TxnLock.SUCCESS) {

                try {
                    theTxnState.add(new EntryTxnOp(theLockOp, myInfo,
                            myLock));
                } catch (TransactionException aTE) {
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
        synchronized(this) {
            if (haveCompleted())
                return STOP;

            theCompletion = anEvent;

            theSearchTask.taint();
            SearchTasks.get().remove(this, wasNotSatisfied());

            if (needsWakeup)
                notify();

            return STOP;
        }
    }

    public MangledEntry getEntry(long aTimeout)
        throws TransactionException,
               InterruptedException {

        synchronized(this) {
            needsWakeup = true;
        }

        while (true) {
            synchronized(this) {
                /*
                  If we've completed, throw exception or return Entry
                  accordingly, cleaning up state appropriately
                */
                if (haveCompleted()) {

                    // We're returning - ensure we don't allow any more
                    // operations to avoid doing a take we'll never return.
                    //
                    needsWakeup = false;

                    if (theCompletion.getException() != null)
                        throw theCompletion.getException();
                    
                    return theCompletion.getEntry();
                }

                // We haven't completed, yet, can we process queue elements?
                if (theNewWrites.size() == 0) {

                    long myRemaining = aTimeout - (System.currentTimeMillis() -
                                                   theStartTime);
                    
                    // Is there more time to wait?
                    if (myRemaining > 0)
                        wait(myRemaining);
                    else {
                        // No, force exit
                        needsWakeup = false;
                        sendEvent(CompletionEvent.COMPLETED);
                    }
                }
            }

            // We must flush the queue outside of lock
            try {
                flushQueue();
            } catch (IOException anIOE) {
                TransactionException myTE =
                    new TransactionException("I/O Error whilst processing queue");
                myTE.initCause(anIOE);
                sendEvent(new CompletionEvent(myTE));
            }
        }
    }

    private void flushQueue() throws IOException {
        while (true) {
            // Something has caused us to stop, give up now
            //
            if (haveCompleted())
                break;

            SpaceEntryUID myUID = null;

            synchronized(this) {
                if (theNewWrites.size() != 0) {
                    myUID = (SpaceEntryUID) theNewWrites.removeFirst();
                }
            }

            // Nothing else in queue?
            //
            if (myUID == null)
                break;

            EntryRepository myRepos =
                EntryRepositoryFactory.get().find(myUID.getType());

            myRepos.find(this, myUID.getOID(), null);
        }
    }

    public synchronized boolean wouldBlock() {
        return (theCompletion == null);
    }

    private synchronized boolean haveCompleted() {
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

    private class SearchVisitorAdapter implements SearchVisitor {

        public boolean isDeleter() {
            return FifoSearchVisitorImpl.this.isDeleter();
        }

        public int offer(SearchOffer anOffer) {
            if (theLogger.isLoggable(Level.FINE))
                theLogger.log(Level.FINE, "Offer");

            synchronized (FifoSearchVisitorImpl.this) {
                if (haveCompleted()) {
                    if (theLogger.isLoggable(Level.FINE))
                        theLogger.log(Level.FINE, theTxnState.getId() +
                            " Have completed");
                    return STOP;
                }
            }

            OpInfo myInfo = anOffer.getInfo();

            if (!Types.isSubtype(theTemplate.getType(), myInfo.getType())) {
                if (theLogger.isLoggable(Level.FINE))
                    theLogger.log(Level.FINE, "Not subtype");

                return TRY_AGAIN;
            }

            MangledEntry myEntry = anOffer.getEntry();

            if (theTemplate.match(myEntry)) {
                return FifoSearchVisitorImpl.this.offer(anOffer);
            } else
                return TRY_AGAIN;
        }
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
            /*
             Queue the write for later consideration unless we're done.
             Later consideration will only be after we've invoked getEntry
             which will only occur after we've performed searching of storage
            */
            synchronized (FifoSearchVisitorImpl.this) {
                if (haveCompleted())
                    return;

                QueueEvent.Context myContext = anEvent.getContext();
                MangledEntry myEntry = myContext.getEntry();
                OID myOID = myContext.getOID();

                theNewWrites.add(new SpaceEntryUID(myEntry.getType(), myOID));

                if (needsWakeup)
                    FifoSearchVisitorImpl.this.notify();
            }
        }

        public EventGeneratorState getMemento() {
            throw new RuntimeException(
                "Shouldn't be happening - we're transient");
        }
    }
}
