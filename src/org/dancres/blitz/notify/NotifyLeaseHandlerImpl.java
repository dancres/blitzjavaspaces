package org.dancres.blitz.notify;

import java.io.IOException;

import java.util.logging.Level;

import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lease.LeaseDeniedException;

import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.disk.DiskTxn;

import org.dancres.blitz.lease.SpaceUID;
import org.dancres.blitz.lease.LeaseHandler;
import org.dancres.blitz.lease.LeaseBounds;

import org.dancres.blitz.txn.TxnOp;
import org.dancres.blitz.txn.TxnState;
import org.dancres.blitz.txn.TxnManager;

import org.dancres.blitz.task.Task;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.util.Time;

public class NotifyLeaseHandlerImpl implements LeaseHandler {
    public boolean recognizes(SpaceUID aUID) {
        return (aUID instanceof SpaceNotifyUID);
    }

    public long renew(SpaceUID aUID, long aLeaseDuration)
        throws UnknownLeaseException, LeaseDeniedException, IOException {

        long myDuration = LeaseBounds.boundNotify(aLeaseDuration);
        long myExpiry = Time.getAbsoluteTime(myDuration);

        boolean myResult;

        OID myOID = ((SpaceNotifyUID) aUID).getOID();

        DiskTxn myTxn = DiskTxn.newTxn();

        try {
            myResult = EventQueue.get().renew(myOID, myExpiry);
        } finally {
            myTxn.commit();
        }

        if (!myResult)
            throw new UnknownLeaseException();

        log(new LeaseRenewal(myOID, myExpiry));

        return myDuration;
    }

    public void cancel(SpaceUID aUID)
        throws UnknownLeaseException, IOException {

        boolean myResult;

        OID myOID = ((SpaceNotifyUID) aUID).getOID();

        DiskTxn myTxn = DiskTxn.newTxn();

        try {
            myResult = EventQueue.get().cancel(myOID);
        } finally {
            myTxn.commit();
        }

        if (!myResult)
            throw new UnknownLeaseException();

        log(new LeaseCancel(myOID));
    }

    private void log(TxnOp anAction) throws IOException {
        try {
            TxnManager.get().log(anAction);
        } catch (TransactionException aTE) {
            throw new IOException("Failed to log action");
        }
    }

    private static final class LeaseRenewal implements TxnOp {

        private OID theOID;
        private long theExpiry;

        LeaseRenewal(OID aOID, long anExpiry) {
            theOID = aOID;
            theExpiry = anExpiry;
        }

        public void restore(TxnState aState) throws IOException {
            EventQueue myQueue = EventQueue.get();

            DiskTxn myTxn = DiskTxn.newTxn();

            try {
                myQueue.renew(theOID, theExpiry);
            } catch (IOException anIOE) {
            } finally {
                myTxn.commit();
            }
        }

        public void commit(TxnState aState) throws IOException {
            // Nothing to do  - already applied
        }

        public void abort(TxnState aState) throws IOException {
            // Nothing to do  - already applied
        }

        public String toString() {
            return " NR : " + theOID + " : " + theExpiry;
        }
    }

    private static final class LeaseCancel implements TxnOp {
        private OID theOID;

        LeaseCancel(OID aOID) {
            theOID = aOID;
        }

        public void restore(TxnState aState) throws IOException {
            EventQueue myQueue = EventQueue.get();

            DiskTxn myTxn = DiskTxn.newTxn();

            try {
                myQueue.cancel(theOID);
            } catch (IOException anIOE) {
            } finally {
                myTxn.commit();
            }
        }

        public void commit(TxnState aState) throws IOException {
            // Nothing to do  - already applied
        }

        public void abort(TxnState aState) throws IOException {
            // Nothing to do  - already applied
        }

        public String toString() {
            return " NC : " + theOID;
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
                EventQueue.theLogger.log(Level.SEVERE,
                                         "Failed to log lease action",
                                         anException);
            }
        }
    }
}
