package org.dancres.blitz.remote.txn;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

import net.jini.core.transaction.server.ServerTransaction;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.core.transaction.*;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.config.ConfigurationException;

import org.dancres.blitz.txn.TxnManager;
import org.dancres.blitz.txn.TxnState;
import org.dancres.blitz.util.Time;
import org.dancres.blitz.lease.LeaseBounds;
import org.dancres.blitz.lease.Reapable;
import org.dancres.blitz.lease.ReapFilter;
import org.dancres.blitz.lease.LeaseReaper;
import org.dancres.blitz.Logging;
import org.dancres.blitz.config.ConfigurationFactory;

/**
 */
public class LoopBackMgr implements Reapable {

    private static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.remote.txn.LoopBackMgr");

    /**
     * This will need updating based on reading the log or reading
     * from checkpoint or both! NO IT WON'T we simply won't log these - they
     * either complete or they don't - they're one operation wonders which
     * don't need logging.
     */
    private long theNextKey = 0;
    private long theMagic = System.currentTimeMillis();

    private LeaseReaper theReaper;

    /**
     * Keeps track of transactions we have active in the core.  We hold lease
     * state here.
     */
    private HashMap theActiveTxns = new HashMap();

    private TransactionManager theStub;

    private static LoopBackMgr theMgr;

    static void init(TransactionManager aStub) {
        theMgr = new LoopBackMgr(aStub);
    }

    static LoopBackMgr get() {
        return theMgr;
    }

    private LoopBackMgr(TransactionManager aStub) {
        try {
            long myReapInterval =
                ((Long) ConfigurationFactory.getEntry("loopbackTxnReapInterval",
                    long.class,
                    new Long(5 * 60 * 1000))).longValue();
            theReaper = new LeaseReaper("LoopbackTxn", null, myReapInterval);

            theReaper.add(this);

        } catch (ConfigurationException aCE) {
            theLogger.log(Level.SEVERE, "Failed to load config", aCE);
        }

        theStub = aStub;
    }

    private long nextId() {
        synchronized (this) {
            return theNextKey++;
        }
    }

    public TxnTicket create(long aLeaseTime)
        throws LeaseDeniedException, RemoteException {

        long myLeaseTime =
            Time.getAbsoluteTime(LeaseBounds.boundTxn(aLeaseTime));

        long myId = nextId();

        SpaceTxnUID myUID = new SpaceTxnUID(myId, theMagic);

        TxnDetails myDetails = new TxnDetails(myLeaseTime);

        ServerTransaction myTxn = new ServerTransaction(theStub, myId);

        try {
            // Insert the transaction
            //
            TxnManager.get().getTxnFor(myTxn, false);
        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Failed to allocate loopback txn", anE);
            throw new LeaseDeniedException("Couldn't allocate txn");
        }

        synchronized(this) {
            theActiveTxns.put(myUID, myDetails);
        }

        return new TxnTicket(myUID, myLeaseTime);
    }

    private SpaceTxnUID validateTxn(long anId)
        throws UnknownTransactionException {

        SpaceTxnUID myUID = new SpaceTxnUID(anId, theMagic);

        synchronized (this) {
            TxnDetails myDetails = (TxnDetails) theActiveTxns.get(myUID);

            if ((myDetails == null) ||
                (myDetails.hasExpired(System.currentTimeMillis())))
                throw new UnknownTransactionException();
        }

        return myUID;
    }

    public void commit(long id)
        throws UnknownTransactionException, CannotCommitException,
        RemoteException {

        SpaceTxnUID myUID = validateTxn(id);

        TxnManager myMgr = TxnManager.get();

        TxnState myState = myMgr.getTxnFor(theStub, id);

        try {
            myMgr.prepareAndCommit(myState);
        } finally {
            synchronized (this) {
                theActiveTxns.remove(myUID);
            }
        }
    }

    public void commit(long id, long waitFor)
        throws UnknownTransactionException, CannotCommitException,
        TimeoutExpiredException, RemoteException {

        commit(id);
    }

    public void abort(long id)
        throws UnknownTransactionException, CannotAbortException,
        RemoteException {

        SpaceTxnUID myUID = validateTxn(id);

        TxnManager myMgr = TxnManager.get();

        TxnState myState = myMgr.getTxnFor(theStub, id);

        try {
            myMgr.abort(myState);
        } finally {
            synchronized(this) {
                theActiveTxns.remove(myUID);
            }
        }
    }

    public void abort(long id, long waitFor)
        throws UnknownTransactionException, CannotAbortException,
        TimeoutExpiredException, RemoteException {

        abort(id);
    }

    boolean renew(SpaceTxnUID aUID, long anExpiry) {
        synchronized (this) {
            TxnDetails myHolder = (TxnDetails) theActiveTxns.get(aUID);

            if (myHolder != null) {
                return myHolder.testAndSetExpiry(System.currentTimeMillis(),
                    anExpiry);
            }

            return false;
        }
    }

    boolean cancel(SpaceTxnUID aUID) {
        return (delete(aUID) != null);
    }


    public TxnDetails delete(SpaceTxnUID aUID) {

        synchronized (this) {
            TxnDetails myHolder = (TxnDetails) theActiveTxns.remove(aUID);

            if (myHolder != null) {
                try {
                    TxnState myState =
                        TxnManager.get().getTxnFor(theStub, aUID.getId());

                    TxnManager.get().abort(myState);
                } catch (Exception anE) {
                    // Nothing we cn do
                }

                return myHolder;
            }

            return null;
        }
    }

    public void reap(ReapFilter aFilter) {
        /*
          No reap filters will be configured so we can ignore those - see
          initialization in constructor
         */
        long myTime = System.currentTimeMillis();

        Object[] myKeys;

        synchronized (this) {
            myKeys = theActiveTxns.keySet().toArray();
        }

        for (int i = 0; i < myKeys.length; i++) {
            TxnDetails myHolder =
                (TxnDetails) theActiveTxns.get(myKeys[i]);

            if (myHolder.hasExpired(myTime)) {
                delete((SpaceTxnUID) myKeys[i]);
            }
        }
    }

    public String toString() {
        return "LoopBackMgr";
    }

    public int hashCode() {
        return 4095;
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof LoopBackMgr)
            return true;
        else
            return false;
    }
}
