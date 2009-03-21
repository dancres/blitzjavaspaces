package org.dancres.blitz.txn;

import java.io.Serializable;

import java.rmi.RemoteException;

import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

/**
   Provides uniformity in the way we handle transactions.  Where all
   transactions are seen to have a manager.  Thus, all null transactions
   have a transaction manager of type LocalTxnManager.
 */
class LocalTxnManager implements TransactionManager, Serializable {

    /**
     * This will need updating based on reading the log or reading
     * from checkpoint or both! NO IT WON'T we simply won't log these - they
     * either complete or they don't - they're one operation wonders which
     * don't need logging.
     */
    private long theNextNullKey = 0;

    long nextId() {
        synchronized(this) {
            return theNextNullKey++;
        }
    }

    public Created create(long lease)
        throws LeaseDeniedException, RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public void join(long id, TransactionParticipant part, long crashCount)
        throws UnknownTransactionException, CannotJoinException,
               CrashCountException, RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public int getState(long id)
        throws UnknownTransactionException, RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public void commit(long id)
        throws UnknownTransactionException, CannotCommitException,
               RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public void commit(long id, long waitFor)
        throws UnknownTransactionException, CannotCommitException,
               TimeoutExpiredException, RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public void abort(long id)
        throws UnknownTransactionException, CannotAbortException,
               RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public void abort(long id, long waitFor)
        throws UnknownTransactionException, CannotAbortException,
               TimeoutExpiredException, RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public String toString() {
        return "NullTxnMgr";
    }

    public int hashCode() {
        return 1;
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof LocalTxnManager)
            return true;
        else
            return false;
    }
}
