package org.dancres.blitz.remote;

import java.rmi.RemoteException;

import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.CannotJoinException;

import net.jini.core.transaction.server.TransactionManager;
import net.jini.core.transaction.server.TransactionParticipant;
import net.jini.core.transaction.server.CrashCountException;

import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;

/**
   "Glue" between the non-remote internals of Blitz and the outside (remote)
   world.
 */
class TxnGatewayImpl implements TxnGateway {
    private long theCrashCount = System.currentTimeMillis();
    private TransactionParticipant theParticipantStub;

    TxnGatewayImpl(TransactionParticipant aStub) {
        theParticipantStub = aStub;
    }

    public void join(TxnId anId)
        throws UnknownTransactionException, CannotJoinException,
               CrashCountException, RemoteException {

        anId.getManager().join(anId.getId(), theParticipantStub,
                               theCrashCount);
    }

    public int getState(TxnId anId)
        throws UnknownTransactionException, RemoteException {

        return anId.getManager().getState(anId.getId());
    }
}
