package org.dancres.blitz;

import java.io.IOException;
import java.io.Serializable;

import java.rmi.RemoteException;

import net.jini.core.transaction.server.TransactionManager;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.txn.TxnState;
import org.dancres.blitz.txn.TxnManager;

/**
   All of this could be done in-line within SpaceImpl this just keeps
   the method count down and separates the code a little.  Responsible
   for handling external requests against transactions and dispatching
   to the appropriate internal objects.

   @todo Serializable here is naughty and strictly to facilitate unit tests
   via TxnMgr class.
 */
class TxnControlImpl implements TxnControl, Serializable {

    public int prepare(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        TxnManager myManager = TxnManager.get();

        TxnState myState = myManager.getTxnFor(aMgr, anId);

        return myManager.prepare(myState);
    }

    public void commit(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        TxnManager myManager = TxnManager.get();

        TxnState myState = myManager.getTxnFor(aMgr, anId);

        myManager.commit(myState);
    }

    public void abort(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        TxnManager myManager = TxnManager.get();

        TxnState myState = myManager.getTxnFor(aMgr, anId);

        myManager.abort(myState);
    }

    public int prepareAndCommit(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        TxnManager myManager = TxnManager.get();

        TxnState myState = myManager.getTxnFor(aMgr, anId);

        return myManager.prepareAndCommit(myState);
    }

    public void requestSnapshot() throws TransactionException, IOException {
        TxnManager.get().requestSnapshot();
    }

    public void backup(String aDir) throws IOException {
        TxnManager.get().hotBackup(aDir);
    }
}
