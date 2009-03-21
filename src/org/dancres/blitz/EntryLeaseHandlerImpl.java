package org.dancres.blitz;

import java.io.IOException;

import java.util.logging.Level;

import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lease.LeaseDeniedException;

import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.disk.DiskTxn;

import org.dancres.blitz.lease.SpaceUID;
import org.dancres.blitz.lease.LeaseHandler;
import org.dancres.blitz.lease.LeaseBounds;

import org.dancres.blitz.entry.EntryRepositoryFactory;
import org.dancres.blitz.entry.EntryRepository;

import org.dancres.blitz.txn.TxnId;
import org.dancres.blitz.txn.TxnOp;
import org.dancres.blitz.txn.TxnState;
import org.dancres.blitz.txn.TxnManager;

import org.dancres.blitz.txnlock.*;

import org.dancres.blitz.task.Task;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.util.Time;

public class EntryLeaseHandlerImpl implements LeaseHandler {
    public boolean recognizes(SpaceUID aUID) {
        return (aUID instanceof SpaceEntryUID);
    }

    public long renew(SpaceUID aUID, long aLeaseDuration)
        throws UnknownLeaseException, LeaseDeniedException, IOException {

        long myDuration = LeaseBounds.boundWrite(aLeaseDuration);
        long myExpiry = Time.getAbsoluteTime(myDuration);

        boolean myResult;

        String myType = ((SpaceEntryUID) aUID).getType();
        OID myOID = ((SpaceEntryUID) aUID).getOID();

        DiskTxn myTxn = DiskTxn.newTxn();

        try {
            myResult =
                EntryRepositoryFactory.get().find(myType).renew(myOID,
                                                                myExpiry);
        } finally {
            myTxn.commit();
        }

        if (!myResult)
            throw new UnknownLeaseException();

        LeaseRenewal myRenewal = new LeaseRenewal(myType, myOID, myExpiry);

        // Now figure out when we're gonna log this change
        LockMgr myMgr = TxnLocks.getLockMgr(myType);
        TxnLock myLock = myMgr.getLock(myOID);
        TxnId myWriter = null;

        synchronized(myLock) {
            myWriter = myLock.getWriter();
        }

        /*
          If there is a writer present, the lease renewal should only be issued
          on completion of the associated transaction.

          This ordering is critical for correct recovery.  If we immediately
          write the lease renewal it will appear in the logs before the
          transaction that write's the Entry.  Thus, if the Entry was never
          flushed to disk, it won't be present in the cache and the lease
          renewal will fail whilst apparently having succeeded from the user's
          perspective.
        */
        if (myWriter == null) {
            log(myRenewal);
        } else {
            TxnState myState = null;

            try {
                myState = TxnManager.get().getTxnFor(myWriter);
            } catch (Exception anE) {
                /*
                  It's either UnknownTransaction or Remote - in this case
                  as there's a writer, it can't be Remote so it can only be
                  UnknownTransaction which means the transaction completed
                  before we tag the lease renewal onto it.
                */
            }
            
            if (myState == null) {
                // Too late, transaction is gone
                log(myRenewal);
            } else {
                try {
                    myState.add(myRenewal);
                } catch (TransactionException aTE) {
                    // Couldn't tag it onto the transaction - must have
                    // resolved
                    log(myRenewal);
                }
            }
        }

        return myDuration;
    }

    public void cancel(SpaceUID aUID)
        throws UnknownLeaseException, IOException {

        boolean myResult;

        String myType = ((SpaceEntryUID) aUID).getType();
        OID myOID = ((SpaceEntryUID) aUID).getOID();

        DiskTxn myTxn = DiskTxn.newTxn();

        try {
            myResult =
                EntryRepositoryFactory.get().find(myType).cancel(myOID);
        } finally {
            myTxn.commit();
        }

        if (!myResult)
            throw new UnknownLeaseException();

        LeaseCancel myCancel = new LeaseCancel(myType, myOID);

        // Now figure out when we're gonna log this change
        LockMgr myMgr = TxnLocks.getLockMgr(myType);
        TxnLock myLock = myMgr.getLock(myOID);
        TxnId myWriter = null;

        synchronized(myLock) {
            myWriter = myLock.getWriter();
        }

        /*
          If there is a writer present, the lease cancel should only be issued
          on completion of the associated transaction.

          This ordering is critical for correct recovery.  If we immediately
          write the lease cancel it will appear in the logs before the
          transaction that write's the Entry.  Thus, if the Entry was never
          flushed to disk, it won't be present in the cache and the lease
          cancel will fail whilst apparently having succeeded from the user's
          perspective.
        */
        if (myWriter == null) {
            log(myCancel);
        } else {
            TxnState myState = null;

            try {
                myState = TxnManager.get().getTxnFor(myWriter);
            } catch (Exception anE) {
                /*
                  It's either UnknownTransaction or Remote - in this case
                  as there's a writer, it can't be Remote so it can only be
                  UnknownTransaction which means the transaction completed
                  before we tag the lease cancel onto it.
                */
            }
            
            if (myState == null) {
                // Too late, transaction is gone
                log(myCancel);
            } else {
                try {
                    myState.add(myCancel);
                } catch (TransactionException aTE) {
                    // Couldn't tag it onto the transaction - must have
                    // resolved
                    log(myCancel);
                }
            }
        }
    }

    private void log(TxnOp anAction) throws IOException {
        try {
            TxnManager.get().log(anAction);
        } catch (TransactionException aTE) {
            throw new IOException("Failed to log action");
        }
    }

    private static final class LeaseRenewal implements TxnOp {

        private String theType;
        private OID theOID;
        private long theExpiry;

        LeaseRenewal(String aType, OID aOID, long anExpiry) {
            theType = aType;
            theOID = aOID;
            theExpiry = anExpiry;
        }

        public void restore(TxnState aState) throws IOException {
            EntryRepository myRepos =
                EntryRepositoryFactory.get().get(theType);
                
            DiskTxn myTxn = DiskTxn.newTxn();

            try {
                myRepos.renew(theOID, theExpiry);
            } catch (IOException anIOE) {
            } finally {
                myTxn.commit();
            }
        }

        public void commit(TxnState aState) throws IOException {
            // Nothing to do  - already applied
        }

        public void abort(TxnState aState) throws IOException {
            // Never called
        }

        public String toString() {
            return " ER : " + theType + " : " + theOID + " : " + theExpiry;
        }
    }

    private static final class LeaseCancel implements TxnOp {
        private String theType;
        private OID theOID;

        LeaseCancel(String aType, OID aOID) {
            theType = aType;
            theOID = aOID;
        }

        public void restore(TxnState aState) throws IOException {
            EntryRepository myRepos =
                EntryRepositoryFactory.get().get(theType);
            
            DiskTxn myTxn = DiskTxn.newTxn();

            try {
                myRepos.cancel(theOID);
            } catch (IOException anIOE) {
            } finally {
                myTxn.commit();
            }
        }

        public void commit(TxnState aState) throws IOException {
            // Nothing to do  - already applied
        }

        public void abort(TxnState aState) throws IOException {
            // Never called
        }

        public String toString() {
            return " EC : " + theType + " : " + theOID;
        }
    }

    private static final class LogTask implements Task {
        private TxnOp theAction;

        LogTask(TxnOp anAction) {
            theAction = anAction;
        }

        public void run() {
            try {
                TxnManager.get().log(theAction);
            } catch (Exception anException) {
                SpaceImpl.theLogger.log(Level.SEVERE,
                                        "Failed to log lease action",
                                        anException);
            }
        }
    }
}
