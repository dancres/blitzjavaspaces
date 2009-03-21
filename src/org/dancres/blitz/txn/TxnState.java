package org.dancres.blitz.txn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.server.TransactionConstants;

import org.dancres.blitz.disk.DiskTxn;
import org.dancres.blitz.notify.EventQueue;
import org.dancres.blitz.notify.QueueEvent;
import org.dancres.blitz.task.Task;
import org.dancres.blitz.task.Tasks;

/*
 * We only need to post QueueEvent.TRANSACTION_ENDED during live work, not
 * during recovery as it will only be relevant to active notifies.  When
 * we restart the only active notifies left would be those not interested in
 * the event because they were registered with a null transaction.
 *
 * Given the above, we can post the event from doFinalize which will be
 * called in TxnManager as part of "live" operations.  This neatly ensures
 * we will only do this work outside of recovery.
 *
 * We will post TRANSACTION_ENDED even for null transactions and for each
 * such event scan the generators and kill any that are anchored against
 * the transaction - this should be done via a new method to test this.
 *
 * EventGenerators should only profess interest if they aren't already tainted
 * as they do with canSee etc.
 *
 * Note we needn't call kill we could just pass the event into all generators
 * and have them taint and then kill themselves (assuming they no longer
 * dispatch CleanTask).  We could then strip out TxnNotify's but we would need
 * to add support for TRANSACTION_ENDED to each canSee method.
 */

/**
   Contains all state associated with a particular transaction identified
   by TxnId.
 
   @todo Deal with TRANSACTION_ENDED etc as above
 */
public class TxnState implements java.io.Serializable {
    static final long serialVersionUID = 2855252304868267667L;

    private int theState = TransactionConstants.ACTIVE;

    private TxnId theId;

    private boolean nonDestructive;

    /*
      Typical transaction might be take/write or a few ops more, so we
      allocate accordingly

      @todo Decide if this is appropriate/worth doing
     */
    private ArrayList theOperations = new ArrayList(5);

    TxnState(TxnId anId) {
        theId = anId;
    }

    TxnState(TxnId anId, boolean isIdentity) {
        theId = anId;
        nonDestructive = isIdentity;
    }

    public TxnId getId() {
        return theId;
    }

    int getStatus() throws TransactionException {
        synchronized(this) {
            return theState;
        }
    }

    public void add(TxnOp anOp) throws TransactionException {
        synchronized (this) {
            if (theState != TransactionConstants.ACTIVE) {
                TxnManager.theLogger.log(Level.SEVERE,
                    "Txn not in active state at addOp");
                throw new TransactionException("Transaction no longer active");
            } else {
                theOperations.add(anOp);
            }
        }
    }

    /**
     * A move to voting state indicates this transaction is going to be resolved
     * and thus cannot have any further actions assigned to it.  Note that
     * voting state will only be asserted if the transaction is currently
     * active.  Voting is a transient state and only exists temporarily whilst
     * we move to a resolved state.  It's only benefit is that it renders
     * a transaction's state unchangeable and allows us to make binding
     * decisions such as whether we need to log to disk.
     */
    int vote() throws TransactionException {
        int myState;

        synchronized (this) {

            if (theState == TransactionConstants.ACTIVE) {
                theState = TransactionConstants.VOTING;
            }

            myState = theState;
        }

        return myState;
    }

    /**
     * @todo Decide if ths DiskTxn is required here - probably not?
     */
    int prepare(boolean needsRestore)
        throws UnknownTransactionException, IOException {

        if (needsRestore) {
            /*
              If we're doing restore, we're single threaded and don't need
              the protection of the lock (getLock())
             */
            // DiskTxn myTxn = DiskTxn.newTxn();

            for (int i = (theOperations.size() - 1); i > -1; i--) {
                TxnOp myOp = (TxnOp) theOperations.get(i);
                myOp.restore(this);
            }

            // myTxn.commit();
        }

        int myStatus;

        synchronized(this) {
            if (theState == TransactionConstants.VOTING) {
                theState = TransactionConstants.PREPARED;
            }

            /*
             The state may not have been ACTIVE, we might already have
             been prepared or even got to commited/aborted and we need to
             reflect that in the status we return.
            */
            myStatus = theState;

        }

        return myStatus;
    }

    void commit()
        throws UnknownTransactionException, IOException {

        synchronized(this) {
            if (theState != TransactionConstants.PREPARED) {
                TxnManager.theLogger.log(Level.SEVERE,
                    "Txn not prepared before commit");
                throw new UnknownTransactionException();
            }

            /*
            * We can afford to set the state beforehand because if this doesn't
            * work we have serious problems and it's very unlikely a second attempt
            * will succeed/do any good.
            */
            theState = TransactionConstants.COMMITTED;

            /*
            * Having changed the state we can be sure no one else will do the
            * same thing again and it's better to not hold the lock across the
            * below which might require I/O
            */
        }

        /*
         * MUST BE DONE IN REVERSE ORDER - WE WANT A TAKE TO BE APPLIED
         * BEFORE WE HIT THE PREVIOUS WRITE.  THIS ALLOWS US TO GENERATE
         * APPROPRIATE EVENTS FOR WRITES READY FOR NOTIFY - i.e. we
         * want the disk system to be aware that something has been
         * deleted so that we can test that state and abandon generating
         * a write event in those cases.  This saves adding code/structure
         * to search other operations to verify whether a write generates
         * an event or not.
         */

         // StringBuffer myTxn = new StringBuffer(theId + " ");

        for (int i = (theOperations.size() - 1); i > -1; i--) {
            TxnOp myOp = (TxnOp) theOperations.get(i);
            myOp.commit(this);

            // myTxn = myTxn.append(myOp.toString() + " ");
        }

        // TxnManager.theLogger.log(Level.INFO, myTxn.toString());
    }

    void abort()
        throws UnknownTransactionException, IOException {

        synchronized(this) {
            /*
             Transaction pinger may attempt abort on a just finalized transaction
             cos the transaction manager has forgotten about it.  We need to
             handle that by refusing to abort when we're finalized (committed
             or aborted).
            */
            if ((theState == TransactionConstants.COMMITTED) ||
                (theState == TransactionConstants.ABORTED)) {
                TxnManager.theLogger.log(Level.SEVERE,
                    "Txn already finalized");
                throw new UnknownTransactionException();
            }

            /*
            * We can afford to set the state beforehand because if this doesn't
            * work we have serious problems and it's very unlikely a second attempt
            * will succeed/do any good.
            */
            theState = TransactionConstants.ABORTED;

            /*
            * Having changed the state we can be sure no one else will do the
            * same thing again and it's better to not hold the lock across the
            * below which might require I/O
            */
        }

        Iterator myOps = theOperations.iterator();

        // No events generated so we can use whatever order we like
        while (myOps.hasNext()) {
            TxnOp myOp = (TxnOp) myOps.next();
            myOp.abort(this);
        }
    }

    public boolean isIdentity() {
        return nonDestructive;
    }

    public boolean isNull() {
        return theId.isNull();
    }

    public boolean hasNoOps() {
        return (theOperations.size() == 0);
    }

    public String toString() {
        StringBuffer myOps = new StringBuffer();

        for (int i = 0; i < theOperations.size(); i++) {
            // For the first operation, we adopt the time of the
            // enclosing command so drop the hyphen
            if (i == 0)
                myOps.append(" " + theOperations.get(i).toString() + " : " +
                             theId + "\n");
            else 
                myOps.append("- : " + theOperations.get(i).toString() + " : " +
                             theId + "\n");
        }

        return myOps.toString();
    }

    void doFinalize() {
        /*
         * Signal any associated listeners - we only want to do this during live
         * operations
         */
        QueueEvent myEvent =
                new QueueEvent(QueueEvent.TRANSACTION_ENDED,
                this, null);
        EventQueue.get().add(myEvent);
    }

    public void test() throws TransactionException {
        synchronized (this) {
            if (theState != TransactionConstants.ACTIVE) {
                TxnManager.theLogger.log(Level.SEVERE,
                    "Txn not in active state at addOp");
                throw new TransactionException("Transaction no longer active");
            }
        }
    }
}
