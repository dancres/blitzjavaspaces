package org.dancres.blitz.txnlock;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

import org.dancres.blitz.entry.OpInfo;

import org.dancres.blitz.task.Task;
import org.dancres.blitz.task.Tasks;

import org.dancres.blitz.txn.TxnId;
import org.dancres.blitz.txn.TxnState;

import org.dancres.blitz.Logging;

/**
   <p> Every transaction (null or otherwise) must secure a lock for an
   EntrySleeve before it can deemed to have succeeded.  When the transaction is
   commited these locks are then released. </p>

   <p> Things are slightly more complicated than this because we must handle
   lock conflict and integrate that with the dispatching of blocked takes or
   reads such that we can wake up those blocked matches when the blocking
   transaction is commited. </p>
 */
public class TxnLock {
    private static final Logger theLogger =
        Logging.newLogger("org.dancres.blitz.txnlock.TxnLock");

    public static final int WRITE = -1;
    public static final int READ = -2;
    public static final int DELETE = -3;

    public static final int SUCCESS = 1;
    public static final int CONFLICT = 2;
    public static final int FAIL = 3;

    private List theConflicts;

    private ArrayList theLockStates = new ArrayList();

    /*
      DEBUG BITS
     */
    private static final boolean DEBUG = false;
    private static Object theIdLock = new Object();
    private static long theNextId = 0;
    private long theId = -1;

    private static class LockState {
        private int theType;
        private TxnId theOwner;

        LockState(int aType, TxnId anOwner) {
            theType = aType;
            theOwner = anOwner;
        }

        int getType() {
            return theType;
        }

        TxnId getOwner() {
            return theOwner;
        }

        public String toString() {
            return theOwner + ":" + theType;
        }
    }

    TxnLock() {
        if (DEBUG) {
            synchronized(theIdLock) {
                theId = ++theNextId;
            }
        }
    }

    public boolean isActive() {
        return (theLockStates.size() != 0);
    }

    public int test(TxnState anAcquirer, int aDesiredOp) {
        TxnId myTxnId = anAcquirer.getId();

        if (aDesiredOp == WRITE) {
            // Always succeeds
            return SUCCESS;
        }

        switch(aDesiredOp) {
            case READ : {
                boolean deleterIsMe = hasDeleterThatsMe(myTxnId);
                TxnId aDeleter = hasDeleter();
                TxnId aWriter = hasWriterOtherThan(myTxnId);

                if (deleterIsMe) {
                    return FAIL;
                } else if ((aDeleter != null) || (aWriter != null)) {
                    return CONFLICT;
                } else {
                    return SUCCESS;
                }
            }
            case DELETE : {
                boolean deleterIsMe = hasDeleterThatsMe(myTxnId);
                TxnId aDeleter = hasDeleter();
                TxnId aWriter = hasWriterOtherThan(myTxnId);
                TxnId aReader = hasReaderOtherThan(myTxnId);

                if (deleterIsMe)
                    return FAIL;
                else if ((aDeleter != null) ||
                         (aReader != null) ||
                         (aWriter != null)) {
                    return CONFLICT;
                } else {
                    return SUCCESS;
                }
            }
            default : {
                throw new RuntimeException("Unrecognised acquire op");
            }
        }
    }

    /**
       One calls this method to assert an operation lock on a particular
       EntryUID.  On success <code>true</code> is returned and the acquire
       is complete.  In the case of failure, the caller is not blocked.
       Rather, we register a callback against the transaction which will
       be invoked after it relinquishes it's lock.  Note that there might
       well be more than one blocking transaction but we only bother
       registering against one of them.  This might result in a few false
       wakeups but they are unlikely to be performance inhibiting.  This
       method should be called in a synchronized block. <P>

       Note that writes always succeed so there's no need to pass a callback
       for such acquire requests.

       @param anAcquirer TxnId of the transaction which wishes to assert the
       lock.
       @param aDesiredOp should be one of DELETE, READ,
       WRITE
       @param aParty called should a blocking transaction be encountered and
       a second time when the blocking transaction has relinquished the lock
       @param aHandback passed to aParty to allow multiplexing of conflicts
       @param isRecovery when <code>true</code> causes a lock to be asserted
       without performing checks.

       @return One of, SUCCESS, CONFLICT or FAIL.
     */
    public int acquire(TxnState anAcquirer, int aDesiredOp, BaulkedParty aParty,
                       Object aHandback, boolean isRecovery) {

        TxnId myTxnId = anAcquirer.getId();

        if (aDesiredOp == WRITE) {
            // Always succeeds
            theLockStates.add(new LockState(WRITE, myTxnId));
            return SUCCESS;
        }

        if (isRecovery) {
            switch (aDesiredOp) {
                case READ : {
                    theLockStates.add(new LockState(READ, myTxnId));
                    break;
                }
                case DELETE: {
                    theLockStates.add(new LockState(DELETE, myTxnId));
                    break;
                }
            }

            return SUCCESS;
        }

        switch(aDesiredOp) {
            case READ : {
                boolean deleterIsMe = hasDeleterThatsMe(myTxnId);
                TxnId aDeleter = hasDeleter();
                TxnId aWriter = hasWriterOtherThan(myTxnId);

                if (deleterIsMe) {
                    return FAIL;
                } else if ((aDeleter != null) || (aWriter != null)) {
                    addConflict(aParty, aHandback,
                                (aDeleter == null) ? aWriter : aDeleter);
                    return CONFLICT;
                } else {
                    theLockStates.add(new LockState(READ, myTxnId));
                    return SUCCESS;
                }
            }
            case DELETE : {
                boolean deleterIsMe = hasDeleterThatsMe(myTxnId);
                TxnId aDeleter = hasDeleter();
                TxnId aWriter = hasWriterOtherThan(myTxnId);
                TxnId aReader = hasReaderOtherThan(myTxnId);

                if (deleterIsMe)
                    return FAIL;
                else if ((aDeleter != null) ||
                         (aReader != null) ||
                         (aWriter != null)) {
                    if (aDeleter != null)
                        addConflict(aParty, aHandback, aDeleter);
                    else
                        addConflict(aParty, aHandback,
                                    (aReader == null) ? aWriter : aReader);
                    return CONFLICT;
                } else {
                    theLockStates.add(new LockState(DELETE,
                                                    myTxnId));
                    return SUCCESS;
                }
            }
            default : {
                throw new RuntimeException("Unrecognised acquire op");
            }
        }
    }

    private void addConflict(BaulkedParty aParty, Object aHandback,
                             TxnId aConflicter) {
        if (aParty == null)
            return;

        if (theConflicts == null)
            theConflicts = new ArrayList();
        // theConflicts = new LinkedList();

        theConflicts.add(new Callback(aConflicter, aParty, aHandback));
    }

    private static class Callback implements Task {
        private TxnId theBlocker;
        private BaulkedParty theParty;
        private Object theHandback;

        Callback(TxnId aBlocker, BaulkedParty aParty, Object aHandback) {
            theBlocker = aBlocker;
            theParty = aParty;
            theHandback = aHandback;
            theParty.blocked(aHandback);
        }

        TxnId getBlocker() {
            return theBlocker;
        }

        BaulkedParty getBaulked() {
            return theParty;
        }

        public void run() {
            theParty.unblocked(theHandback);
        }
    }

    public void release(TxnState anAcquirer, int anOp) {
        TxnId myId = anAcquirer.getId();

        int myIndex = 0;
        boolean haveReleasedLock = false;
        int myOutstanding = 0;

        /*
          Examine all lock states and:

          (1) Release the relevant lock state
          (2) Take note of any other lock states associated with this
          transaction.
        */
        ArrayList myDispatches = new ArrayList();

        synchronized(this) {
            while (myIndex < theLockStates.size()) {
                LockState myState = (LockState) theLockStates.get(myIndex);

                // If the lock state is associated with this transaction...
                if (myState.getOwner().equals(myId)) {
                    /*
                      If we've already released a lock of this type for this
                      transaction just note that we've found another lock held
                      by the transaction.  Otherwise, release the lock and note
                      we did that.
                    */
                    if ((myState.getType() == anOp) && (! haveReleasedLock)) {
                        theLockStates.remove(myIndex);
                        haveReleasedLock = true;
                        continue;
                    } else {
                        ++myOutstanding;
                    }
                }

                ++myIndex;
            }

            if (myOutstanding != 0) {
                // System.err.println("Still have outstanding: " + myOutstanding);
                // We haven't released all locks yet - no point waking up blockers
                return;
            } else {
                // System.err.println("Waking up conflicters");
            }

            // Now process any outstanding blockers
            myIndex = 0;
            while ((theConflicts != null) && (myIndex < theConflicts.size())) {
                Callback myCallback = (Callback) theConflicts.get(myIndex);

                if (myCallback.getBlocker().equals(myId)) {
                    theConflicts.remove(myCallback);

                    // System.err.println("Dispatching callback");
                    myDispatches.add(myCallback);
                } else {
                    ++myIndex;
                }
            }
        }

        if (myDispatches.size() > 0) {
            try {
                Tasks.queue(new DispatchTask(myDispatches));
            } catch (InterruptedException anIE) {
                theLogger.log(Level.SEVERE,
                              "Failed to queue Txn callback", anIE);
            }
        }
    }

    static class DispatchTask implements Task {
        private ArrayList theDispatches;

        DispatchTask(ArrayList aDispatches) {
            theDispatches = aDispatches;
        }

        public void run() {
            for (int i = 0; i < theDispatches.size(); i++) {
                ((Task) theDispatches.get(i)).run();
            }
        }
    }

    public TxnId getWriter() {
        for (int i = 0; i < theLockStates.size(); i++) {
            LockState myState = (LockState) theLockStates.get(i);

            if (myState.getType() == WRITE) {
                return myState.getOwner();
            }
        }

        return null;
    }

    public synchronized boolean hasWriter(TxnId anId) {
        for (int i = 0; i < theLockStates.size(); i++) {
            LockState myState = (LockState) theLockStates.get(i);

            if ((myState.getType() == WRITE) &&
                (myState.getOwner().equals(anId)))
                return true;
        }

        return false;
    }

    public synchronized boolean hasOnly(TxnId anId, int anOp) {
        if (theLockStates.size() != 1)
            return false;

        LockState myState = (LockState) theLockStates.get(0);

        return ((myState.getOwner().equals(anId)) &&
                (myState.getType() == anOp));
    }

    private TxnId hasDeleter() {
        for (int i = 0; i < theLockStates.size(); i++) {
            LockState myState = (LockState) theLockStates.get(i);

            if (myState.getType() == DELETE)
                return myState.getOwner();
        }

        return null;
    }

    private TxnId hasReaderOtherThan(TxnId anId) {
        for (int i = 0; i < theLockStates.size(); i++) {
            LockState myState = (LockState) theLockStates.get(i);

            if ((myState.getType() == READ) &&
                (! myState.getOwner().equals(anId)))
                return myState.getOwner();
        }

        return null;
    }

    private TxnId hasWriterOtherThan(TxnId anId) {
        for (int i = 0; i < theLockStates.size(); i++) {
            LockState myState = (LockState) theLockStates.get(i);

            if ((myState.getType() == WRITE) &&
                (! myState.getOwner().equals(anId)))
                return myState.getOwner();
        }

        return null;
    }

    private boolean hasDeleterThatsMe(TxnId anId) {
        for (int i = 0; i < theLockStates.size(); i++) {
            LockState myState = (LockState) theLockStates.get(i);

            if ((myState.getType() == DELETE) &&
                (myState.getOwner().equals(anId)))
                return true;
        }

        return false;
    }

    public String toString() {
        return "TLock: " + theId;
    }
}
