package org.dancres.blitz.txn;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.server.TransactionConstants;

import org.prevayler.AlarmClock;
import org.prevayler.PrevalentSystem;
import org.prevayler.SnapshotContributor;

/**
 * Responsible for tracking/managing transactions.  This responsiblity is split
 * across two classes.  TxnManager handles control aspects whilst
 * TxnManagerState tracks the transactional information. <P>
 *
 * @see org.dancres.blitz.txn.TxnManager
 */
class TxnManagerState implements PrevalentSystem {
    static final long serialVersionUID = -5650181362477845180L;

    private static boolean UPGRADE = false;

    /*
      These member variables are serialized as part of writeObject and
      de-serialized as part of readObject (which also initializes appropriate
      state).  i.e.  We don't just serialize this object directly.
     */
    private AlarmClock theClock;

    private ConcurrentHashMap theTxns = new ConcurrentHashMap();

    private Serializable[] theSnapshotContributions = new Serializable[0];

    private ArrayList theSnapshotContributors = new ArrayList();

    static void enableUpgrade() {
        UPGRADE = true;
    }

    public void clock(AlarmClock aClock) {
        theClock = aClock;
    }

    public AlarmClock clock() {
        return theClock;
    }

    public List getActiveTxnIds() {
        ArrayList myTxnIds = new ArrayList();

        Iterator myTxns = theTxns.keySet().iterator();

        while (myTxns.hasNext()) {
            TxnId myId = (TxnId) myTxns.next();
            myTxnIds.add(myId);
        }

        return myTxnIds;
    }

    public void add(SnapshotContributor aContributor) {
        synchronized (theSnapshotContributors) {
            if (!theSnapshotContributors.contains(aContributor))
                theSnapshotContributors.add(aContributor);
        }
    }

    public void remove(SnapshotContributor aContributor) {
        synchronized (theSnapshotContributors) {
            theSnapshotContributors.remove(aContributor);
        }
    }

    public Serializable[] getSnapshotContributions() {
        return theSnapshotContributions;
    }

    private void writeObject(ObjectOutputStream anOut) throws IOException {
        anOut.writeObject(LogVersion.VERSION);

        /*
          We only save PREPARED transactions, ignoring ACTIVES because
          they are transient and their state changes won't be applied
          until we've issued prepare and then commit or abort.  The ACTIVES
          will either die due to failure or, post the sync, add operations
          to the log.  Note that, whilst a transaction is active, it generates
          no log records at all hence the reason we don't need to save them.
          Commited or aborted updates in cache which need flushing to disk
          should have already been sync'd before we get this far.
        */
        ArrayList myPrepared = new ArrayList();

        // Write out clock
        //
        anOut.writeObject(theClock);

        Iterator myTxns = theTxns.keySet().iterator();

        while (myTxns.hasNext()) {
            TxnId myId = (TxnId) myTxns.next();

            TxnState myState = getState(myId);

            try {
                int myStatus = myState.getStatus();

                if (myStatus == TransactionConstants.PREPARED) {
                    myPrepared.add(myState);
                }

            } catch (TransactionException aTE) {
                // Whoops, got nailed checking status, logged in the call
                // nothing to do.
            }
        }

        anOut.writeInt(myPrepared.size());

        for (int i = 0; i < myPrepared.size(); i++) {
            anOut.writeObject(myPrepared.get(i));
        }

        /*
          Write out any user-code snapshot contributions
         */
        ArrayList myContributions = new ArrayList();

        synchronized (theSnapshotContributors) {
            for (int i = 0; i < theSnapshotContributors.size(); i++) {
                myContributions.add(((SnapshotContributor) theSnapshotContributors.get(i)).getContribution());
            }
        }

        Serializable[] myUserData = new Serializable[myContributions.size()];
        myUserData = (Serializable[]) myContributions.toArray(myUserData);

        anOut.writeObject(myUserData);
    }

    private void readObject(ObjectInputStream anIn)
            throws IOException, ClassNotFoundException {

        boolean isUpgrade = false;

        theTxns = new ConcurrentHashMap();
        theSnapshotContributors = new ArrayList();

        Object myFirst = anIn.readObject();

        /*
          If there's no LogVersion, chances are we're looking at a pre 1.13
          log format - upgrade is simple as there's no LogVersion and there
          will be no user checkpoint data so we just ignore those fields.
         */
        if (!(myFirst instanceof LogVersion)) {
            TxnManager.theLogger.log(Level.SEVERE, "Upgrading old transaction log");
            isUpgrade = true;
            theClock = (AlarmClock) myFirst;
        } else {
            LogVersion myVersion = (LogVersion) myFirst;

            if (!myVersion.equals(LogVersion.VERSION))
                throw new IOException("Yikes - log versions don't match - upgrade?" + myVersion);

            theClock = (AlarmClock) anIn.readObject();
        }

        int myNumRecords = anIn.readInt();

        for (int i = 0; i < myNumRecords; i++) {
            TxnState myState = (TxnState) anIn.readObject();

            try {
                myState.prepare(true);
            } catch (UnknownTransactionException aUTE) {
                IOException anIOE = new IOException("Failed to recover prepare");
                anIOE.initCause(aUTE);
                throw anIOE;
            }

            theTxns.put(myState.getId(), myState);
        }

        if (isUpgrade)
            theSnapshotContributions = new Serializable[0];
        else
            theSnapshotContributions = (Serializable[]) anIn.readObject();
    }

    private TxnState getState(TxnId anId) {
        return (TxnState) theTxns.get(anId);
    }

    /**
     * Resolve a transaction using this method before calling any of
     * <code>prepare</code>, <code>commit</code>, <code>abort</code> or
     * <code>prepareAndCommit</code>.
     *
     * @todo Add Janitor/Checker thread to clear out dead transaction
     * state - see comments in method
     */
    TxnState getTxnFor(TxnId anId, TxnGateway aGateway, boolean mustExist)
            throws UnknownTransactionException {

        TxnState myState = null;

        if (mustExist) {
            myState = getState(anId);
        } else {
            myState = (TxnState) theTxns.get(anId);

            /*
              If state doesn't exist, we need to join and update the state
             */
            if (myState == null) {
                try {
                    aGateway.join(anId);
                } catch (Exception anException) {
                    TxnManager.theLogger.log(Level.SEVERE,
                            "Failed to join txn" +
                                    anId, anException);

                    throw new UnknownTransactionException();
                }

                /*
                  There's a race condition here where the transaction
                  could be prepared before we get our state updated.
                  If that happens, the prepare method will bounce
                  the prepare call blowing the transaction out.
                      
                  This will leave us with a bit of dead txn state which
                  we ought to cleanup by invoking getState etc.  This
                  could be done by a janitor thread in the future.

                  In reality:

                  (a) This is unlikely
                  (b) Should this happen, we're going to be the least of
                  the problems because someone somewhere thinks
                  the transaction is active (this thread is acting on
                  their behalf) and someone else is closing it out.
                  When the client associated with this thread invokes
                  commit there'll be a big nasty mess.
                */

                /*
                  Up till now, we race to create/join the transaction
                  (see above).  Now we must put it right....
                */
                myState = new TxnState(anId);
                TxnState myExistingState = (TxnState) theTxns.putIfAbsent(anId, myState);

                if (myExistingState != null)
                    myState = myExistingState;
            }
        }

        if (myState == null)
            throw new UnknownTransactionException();
        else
            return myState;
    }

    /**
     * In cases where no explicit transaction has been passed in by a caller,
     * create a null transaction which is an internal, fully transactional
     * replacement which can be used for the duration of the operation
     * in question.
     */
    TxnState newNullTxn() throws RemoteException {
        TxnId myId = TxnId.newNullTxn();
        TxnState myState = new TxnState(myId);

        theTxns.put(myId, myState);

        return myState;
    }

    /**
     * In cases where no state will be changed (no Entry's taken or written),
     * create an instance of this transaction which, when commited or aborted
     * will be undone but not logged.
     */
    TxnState newIdentityTxn() throws RemoteException {
        TxnId myId = TxnId.newNullTxn();
        TxnState myState = new TxnState(myId, true);

        theTxns.put(myId, myState);

        return myState;
    }

    /**
     * Do not call this method directly - it should only be invoked from
     * a Prevayler command.
     */
    int prepare(TxnState aState)
            throws UnknownTransactionException, IOException {

        /*
          Do we know about this transaction?

          If we don't we've failed and are now doing recovery so we must
          re-insert the state.
        */
        boolean needsRestore = (getState(aState.getId()) == null);

        if (needsRestore) {
            theTxns.put(aState.getId(), aState);
        }

        return aState.prepare(needsRestore);
    }

    /**
     * Do not call this method directly - it should only be invoked from
     * a Prevayler command.
     */
    void commit(TxnId anId)
            throws UnknownTransactionException, IOException {

        TxnState myState = getTxnFor(anId, null, true);

        myState.commit();

        removeTxn(anId);
    }

    /**
     * Do not call this method directly - it should only be invoked from
     * a Prevayler command.
     */
    void abort(TxnId anId)
            throws UnknownTransactionException, IOException {

        TxnState myState = getTxnFor(anId, null, true);

        myState.abort();
        removeTxn(anId);
    }

    void abortAll() throws IOException {
        Iterator myTxns = theTxns.keySet().iterator();

        while (myTxns.hasNext()) {
            TxnId myId = (TxnId) myTxns.next();

            TxnState myState = getState(myId);

            try {
                int myStatus = myState.getStatus();

                if ((myStatus == TransactionConstants.PREPARED) ||
                        (myStatus == TransactionConstants.ACTIVE)) {

                    /*
                    *  AbortAll is a naive operation in that it has no
                    *  awareness of a specific transaction thus it cannot
                    *  explicitly vote one of them off so we must do it
                    *  ourselves
                    */
                    myState.vote();
                    myState.abort();
                    myTxns.remove();
                }

            } catch (TransactionException aTE) {
                // Whoops, got nailed checking status, logged in the call
                // nothing to do.
            }
        }
    }

    private void removeTxn(TxnId anId) {
        TxnState myState;

        myState = (TxnState) theTxns.remove(anId);
    }

    int getNumActiveTxns() {
        return theTxns.size();
    }
}


