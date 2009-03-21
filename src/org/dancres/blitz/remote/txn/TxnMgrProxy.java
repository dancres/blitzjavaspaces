package org.dancres.blitz.remote.txn;

import java.rmi.RemoteException;
import java.io.Serializable;

import net.jini.core.transaction.server.TransactionManager;
import net.jini.core.transaction.server.TransactionParticipant;
import net.jini.core.transaction.server.CrashCountException;
import net.jini.core.transaction.*;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.id.Uuid;
import net.jini.id.ReferentUuids;
import net.jini.id.ReferentUuid;

/**
 */
public class TxnMgrProxy implements TransactionManager,
    Serializable, ReferentUuid {
    private TransactionManager theStub;
    private Uuid theUuid;

    public TxnMgrProxy(TransactionManager aStub, Uuid aUuid) {
        theStub = aStub;
        theUuid = aUuid;
    }

    public Created create(long l) throws LeaseDeniedException, RemoteException {
        return theStub.create(l);
    }

    public void join(long l, TransactionParticipant transactionParticipant, long l1)
        throws UnknownTransactionException, CannotJoinException,
            CrashCountException, RemoteException {

        /*
            Do nothing - this is a loopback transaction and join is never done
            because we internally inject a transaction via the loopback txnmgr
            on the server-side so we can simply return.
         */
    }

    public int getState(long l) throws UnknownTransactionException, RemoteException {
        /*
            Do nothing - this is a loopback transaction and getState is only
            done internally and will be routed via the stub which was injected
            via the loopback txnmgr on the server-side
         */

        throw new RemoteException("Shouldn't ever be called - we're a loopback proxy");
    }

    public void commit(long l) throws UnknownTransactionException,
        CannotCommitException, RemoteException {
        theStub.commit(l);
    }

    public void commit(long l, long l1) throws UnknownTransactionException,
        CannotCommitException, TimeoutExpiredException, RemoteException {
        theStub.commit(l, l1);
    }

    public void abort(long l) throws UnknownTransactionException,
        CannotAbortException, RemoteException {
        theStub.abort(l);
    }

    public void abort(long l, long l1) throws UnknownTransactionException,
        CannotAbortException, TimeoutExpiredException, RemoteException {
        theStub.abort(l, l1);
    }

    public Uuid getReferentUuid() {
        return theUuid;
    }

    public boolean equals(Object anObject) {
        return ReferentUuids.compare(this, anObject);
    }

    public int hashCode() {
        return theUuid.hashCode();
    }

    public String toString() {
        return theStub.toString() + ", " + theUuid.toString();
    }
}
