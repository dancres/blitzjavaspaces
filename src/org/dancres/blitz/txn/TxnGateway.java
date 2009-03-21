package org.dancres.blitz.txn;

import java.rmi.RemoteException;

import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.CannotJoinException;
import net.jini.core.transaction.server.CrashCountException;

/**
   <p>TxnManager has no direct link with the remote layer, thus it must rely
   on a third party to handle the remote communication necessary.  The object
   reponsible for this task should implement this interface. </p>

   <p>TxnGateway is reponsible for generating appropriate crashcount value.</p>

   <p>Various implementations are possible including:

   <ul>
   <li>Full remote communication with TransactionManager remote objects</li>
   <li>Communication with embedded local transaction manager for optimization
   or embedded space applications</li>
   </ul>
 */
public interface TxnGateway {
    public void join(TxnId anId)
        throws UnknownTransactionException, CannotJoinException,
               CrashCountException, RemoteException;

    public int getState(TxnId anId)
        throws UnknownTransactionException, RemoteException;
}
