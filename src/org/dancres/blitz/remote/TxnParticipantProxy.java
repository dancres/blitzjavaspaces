package org.dancres.blitz.remote;

import java.rmi.RemoteException;

import java.io.Serializable;
import java.io.IOException;

import net.jini.core.transaction.server.TransactionParticipant;
import net.jini.core.transaction.server.TransactionManager;

import net.jini.core.transaction.UnknownTransactionException;

import net.jini.id.Uuid;
import net.jini.id.ReferentUuid;
import net.jini.id.ReferentUuids;

import org.dancres.blitz.remote.nio.FastSpace;

public class TxnParticipantProxy implements Serializable, ReferentUuid,
                                            TransactionParticipant {

    FastSpace theFast;
    TransactionParticipant theStub;
    Uuid theUuid;

    TxnParticipantProxy(TransactionParticipant aStub, Uuid aUuid) {
        theStub = aStub;
        theUuid = aUuid;
    }

    void enableFastIO(FastSpace aSpace) {
        theFast = aSpace;
    }

    synchronized FastSpace getFastChannel() {
        if (theFast != null) {
            if (! theFast.isInited()) {
                try {
                    theFast.init();
                } catch (IOException anIOE) {
                    throw new Error("Panic: fast io didn't init", anIOE);
                }
            }
        }

        return theFast;
    }

    public Uuid getReferentUuid() {
        return theUuid;
    }

    public int prepare(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        if (getFastChannel() != null)
            return getFastChannel().prepare(aMgr, anId);
        else
            return theStub.prepare(aMgr, anId);
    }

    public void commit(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        if (getFastChannel() != null)
            getFastChannel().commit(aMgr, anId);
        else
            theStub.commit(aMgr, anId);
    }

    public void abort(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        if (getFastChannel() != null)
            getFastChannel().abort(aMgr, anId);
        else
            theStub.abort(aMgr, anId);
    }

    public int prepareAndCommit(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        if (getFastChannel() != null)
            return getFastChannel().prepareAndCommit(aMgr, anId);
        else
            return theStub.prepareAndCommit(aMgr, anId);
    }

    public boolean equals(Object anObject) {
        return ReferentUuids.compare(this, anObject);
    }

    public int hashCode() {
        return theUuid.hashCode();
    }
}
