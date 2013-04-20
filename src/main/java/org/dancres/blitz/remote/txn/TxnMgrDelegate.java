package org.dancres.blitz.remote.txn;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

import net.jini.core.transaction.server.ServerTransaction;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.core.transaction.*;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.config.ConfigurationException;

import org.dancres.blitz.Lifecycle;
import org.dancres.blitz.LifecycleRegistry;
import org.dancres.blitz.lease.*;
import org.dancres.blitz.txn.TxnDispatcher;
import org.dancres.blitz.txn.TxnState;
import org.dancres.blitz.util.Time;
import org.dancres.blitz.Logging;
import org.dancres.blitz.config.ConfigurationFactory;

/**
 * If one is building a transaction manager around the Blitz core, this class provides most of the heavy lifting.
 * Wrapper classes need to present/convert the input/output into the appropriate universe (e.g. Jini or Local) and not
 * much else.
 */
public class TxnMgrDelegate implements Reapable {

    private static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.remote.txn.TxnMgrDelegate");

    static class Tracker implements Lifecycle {
        private List<TxnMgrDelegate> _activeMgrs = new CopyOnWriteArrayList<TxnMgrDelegate>();

        void add(TxnMgrDelegate aMgr) {
            _activeMgrs.add(aMgr);
        }

        public void init() {
        }

        public void deinit() {
            for (TxnMgrDelegate m : _activeMgrs)
                m.stop();

            _activeMgrs.clear();
        }
    }

    private static Tracker theTracker = new Tracker();

    static {
        LifecycleRegistry.add(theTracker);
    }

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
    private TxnLeaseHandlerImpl theLeaseHandler;

    public TxnMgrDelegate(TransactionManager aStub) {
        theTracker.add(this);

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

        theLeaseHandler = new TxnLeaseHandlerImpl(this);
        LeaseHandlers.add(theLeaseHandler);
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
            TxnDispatcher.get().getTxnFor(myTxn, false);
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

        TxnDispatcher myMgr = TxnDispatcher.get();

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

        TxnDispatcher myMgr = TxnDispatcher.get();

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
                        TxnDispatcher.get().getTxnFor(theStub, aUID.getId());

                    TxnDispatcher.get().abort(myState);
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
        return "TxnMgrDelegate";
    }

    public int hashCode() {
        return 4095;
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof TxnMgrDelegate)
            return true;
        else
            return false;
    }

    void stop() {
        LeaseHandlers.remove(theLeaseHandler);
    }
}
