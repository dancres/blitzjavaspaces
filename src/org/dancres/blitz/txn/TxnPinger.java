package org.dancres.blitz.txn;

import java.rmi.RemoteException;
import java.rmi.NoSuchObjectException;

import java.util.List;
import java.util.Iterator;

import java.util.logging.Level;

import net.jini.core.transaction.UnknownTransactionException;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.ActiveObject;
import org.dancres.blitz.ActiveObjectRegistry;

import org.dancres.blitz.config.ConfigurationFactory;

/**
   <p>An instance of this class is created by <code>TransactionManager</code>
   once recovery has been completed.  It's purpose is to regularly check known
   remote transactions status and abort any that can be declared "dead" as
   a result of specific results from invoking the remote
   <code>TransactionManager</code>'s <code>getState</code> method.</p>
 */
class TxnPinger implements ActiveObject, Runnable {
    private static final String PING_PAUSE = "txnPingInterval";

    static final long NO_PAUSE = -1;

    private long thePause = NO_PAUSE;
    private Thread theThread;
    private TxnManagerState theState;

    TxnPinger(TxnManagerState aState) {
        try {
            thePause =
                ((Long)
                 ConfigurationFactory.getEntry(PING_PAUSE,
                                               long.class,
                                               new Long(NO_PAUSE))).longValue();
        } catch (ConfigurationException anE) {
            TxnManager.theLogger.log(Level.SEVERE,
                                     "Failed to load txn ping interval",
                                     anE);
        }

        if (thePause != NO_PAUSE) {
            TxnManager.theLogger.log(Level.INFO, "Txn Pinger active with: " +
                                     thePause + " ms");
            ActiveObjectRegistry.add(this);
        }
    }

    public void begin() {
        theThread = new Thread(this, "TxnPinger");
        theThread.setDaemon(true);
        theThread.start();
    }

    public void halt() {
        theThread.interrupt();
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(thePause);
            } catch (InterruptedException anIE) {
                return;
            }


            Iterator myTxnIds = theState.getActiveTxnIds().iterator();

            while (myTxnIds.hasNext()) {
                TxnId myId = (TxnId) myTxnIds.next();

                // Skip local transactions
                //
                if (myId.isNull())
                    continue;

                /*
                   NoSuchObjectException or UnknownTransactionException means
                   we can kill this transaction.
                 */
                try {
                    TxnGateway myGateway = TxnManager.get().getGateway();

                    myGateway.getState(myId);
                } catch (RemoteException anRE) {
                    if (anRE instanceof NoSuchObjectException) {
                        attemptAbort(myId);
                    }
                } catch (UnknownTransactionException aUTE) {
                        attemptAbort(myId);
                }
            }
        }
    }

    private void attemptAbort(TxnId anId) {
        TxnManager myManager = TxnManager.get();

        try {
            myManager.abort(myManager.getTxnFor(anId));
        } catch (Exception anE) {
            // Nothing more to do, try again next time
            TxnManager.theLogger.log(Level.SEVERE,
                                     "Attempted to abort dead transaction: " +
                                     anId, anE);
        }
    }
}