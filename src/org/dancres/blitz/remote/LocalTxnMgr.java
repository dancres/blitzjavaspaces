package org.dancres.blitz.remote;

import java.io.Serializable;

import java.rmi.RemoteException;

import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import org.dancres.blitz.remote.txn.LoopBackMgr;
import org.dancres.blitz.TxnControl;
import org.dancres.blitz.remote.txn.TxnTicket;

public class LocalTxnMgr implements TransactionManager, Serializable {
    private long theId;

    /*
     * We only need this for the leases we provide, and if we're deserialized it's to handle transaction
     * resolution only, no need to worry about leases. So we can keep this transient (good thing as it's not
     * serializable).
     */
    private transient LocalSpace theSpace;
    private transient LoopBackMgr theLoopBackMgr;

    public LocalTxnMgr(long anId, LocalSpace aSpace) {
        theId = anId;
        theLoopBackMgr = new LoopBackMgr(this);
        theSpace = aSpace;
    }

    public LocalTxnMgr(long anId, TxnControl aControl) {
        theId = anId;
        theLoopBackMgr = new LoopBackMgr(this);
    }

    public ServerTransaction newTxn() {
        synchronized(this) {
            try {
                TxnTicket myTicket = theLoopBackMgr.create(Lease.FOREVER);
                return new ServerTransaction(this, myTicket.getUID().getId());
            } catch (Exception anE) {
                throw new RuntimeException("Failed to create txn", anE);
            }
        }
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof LocalTxnMgr) {
            LocalTxnMgr myOther = (LocalTxnMgr) anObject;

            return (myOther.theId == theId);
        }

        return false;
    }

    public int hashCode() {
        return (int) (theId ^ (theId >>> 32));
    }

    public Created create(long leaseTime) throws LeaseDeniedException,
                                             RemoteException {
        assert(theSpace != null);

        TxnTicket myTicket = theLoopBackMgr.create(leaseTime);

        Lease myLease =
                ProxyFactory.newLeaseImpl(theSpace, theSpace.getServiceUuid(),
                        myTicket.getUID(), myTicket.getLeaseTime());

        return new TransactionManager.Created(myTicket.getUID().getId(), myLease);
    }
 
    public void join(long id, TransactionParticipant part, long crashCount)
        throws UnknownTransactionException, CannotJoinException,
               CrashCountException, RemoteException {
        // Go silently so we don't break anything
    }
 

    public int getState(long id) throws UnknownTransactionException,
                                        RemoteException {
        return TransactionConstants.COMMITTED;
    }
 
    public void commit(long id)
        throws UnknownTransactionException, CannotCommitException,
               RemoteException {
        theLoopBackMgr.commit(id);
    }
 
    public void commit(long id, long waitFor)
        throws UnknownTransactionException, CannotCommitException,
               TimeoutExpiredException, RemoteException {
        theLoopBackMgr.commit(id, waitFor);
    }
 

    public void abort(long id)
        throws UnknownTransactionException, CannotAbortException,
               RemoteException {
        theLoopBackMgr.abort(id);
    }
 
    public void abort(long id, long waitFor)
        throws UnknownTransactionException, CannotAbortException,
               TimeoutExpiredException, RemoteException {
        theLoopBackMgr.abort(id, waitFor);
    }
}

