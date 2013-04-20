package org.dancres.blitz.remote;

import java.rmi.RemoteException;

import java.io.Serializable;

import net.jini.core.transaction.server.TransactionParticipant;
import net.jini.core.transaction.server.TransactionManager;

import net.jini.core.transaction.UnknownTransactionException;

import net.jini.id.Uuid;
import net.jini.id.ReferentUuid;
import net.jini.id.ReferentUuids;

public class TxnParticipantProxy implements Serializable, ReferentUuid,
                                            TransactionParticipant {

    TransactionParticipant theStub;
    Uuid theUuid;

    TxnParticipantProxy(TransactionParticipant aStub, Uuid aUuid) {
        theStub = aStub;
        theUuid = aUuid;
    }

    public Uuid getReferentUuid() {
        return theUuid;
    }

    public int prepare(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        return theStub.prepare(aMgr, anId);
    }

    public void commit(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        theStub.commit(aMgr, anId);
    }

    public void abort(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        theStub.abort(aMgr, anId);
    }

    public int prepareAndCommit(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        return theStub.prepareAndCommit(aMgr, anId);
    }

    public boolean equals(Object anObject) {
        return ReferentUuids.compare(this, anObject);
    }

    public int hashCode() {
        return theUuid.hashCode();
    }
}
