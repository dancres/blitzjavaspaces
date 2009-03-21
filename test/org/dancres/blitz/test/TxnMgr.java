package org.dancres.blitz.test;

import java.io.Serializable;

import java.rmi.RemoteException;

import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import org.dancres.blitz.remote.LocalSpace;
import org.dancres.blitz.TxnControl;

public class TxnMgr implements TransactionManager, Serializable {
    private long theId;
    private long theNextId;
    private TxnControl theControl;

    public TxnMgr(long anId, LocalSpace aSpace) {
        theId = anId;
        theControl = aSpace.getTxnControl();
    }

    public TxnMgr(long anId, TxnControl aControl) {
        theId = anId;
        theControl = aControl;
    }

    public ServerTransaction newTxn() {
        synchronized(this) {
            return new ServerTransaction(this, theNextId++);
        }
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof TxnMgr) {
            TxnMgr myOther = (TxnMgr) anObject;

            return (myOther.theId == theId);
        }

        return false;
    }

    public int hashCode() {
        return (int) (theId ^ (theId >>> 32));
    }

    public Created create(long lease) throws LeaseDeniedException,
                                             RemoteException {
        throw new org.dancres.util.NotImplementedException();
    }
 
    public void join(long id, TransactionParticipant part, long crashCount)
        throws UnknownTransactionException, CannotJoinException,
               CrashCountException, RemoteException {
    }
 

    public int getState(long id) throws UnknownTransactionException,
                                        RemoteException {
        return TransactionConstants.COMMITTED;
    }
 
    public void commit(long id)
        throws UnknownTransactionException, CannotCommitException,
               RemoteException {
        theControl.prepareAndCommit(this, id);
    }
 
    public void commit(long id, long waitFor)
        throws UnknownTransactionException, CannotCommitException,
               TimeoutExpiredException, RemoteException {
        commit(id);
    }
 

    public void abort(long id)
        throws UnknownTransactionException, CannotAbortException,
               RemoteException {
        theControl.abort(this, id);
    }
 
    public void abort(long id, long waitFor)
        throws UnknownTransactionException, CannotAbortException,
               TimeoutExpiredException, RemoteException {
        abort(id);
    }
}

