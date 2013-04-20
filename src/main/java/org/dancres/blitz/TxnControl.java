package org.dancres.blitz;

import java.io.IOException;

import java.rmi.RemoteException;

import net.jini.core.transaction.server.TransactionManager;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.TransactionException;

/**
   Transactional operations are controlled via this interface.
 */
public interface TxnControl {
    public int prepare(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException;

    public void commit(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException;

    public void abort(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException;

    public int prepareAndCommit(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException;

    public void requestSnapshot() throws TransactionException, IOException;

    public void backup(String aDir) throws IOException;
}
