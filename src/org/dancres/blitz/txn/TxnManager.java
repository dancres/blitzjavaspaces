package org.dancres.blitz.txn;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.config.ConfigurationException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.server.ServerTransaction;
import net.jini.core.transaction.server.TransactionConstants;
import net.jini.core.transaction.server.TransactionManager;

import org.dancres.blitz.Logging;
import org.dancres.blitz.config.ConfigurationFactory;
import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.task.Tasks;
import org.prevayler.Command;
import org.prevayler.SnapshotContributor;
import org.prevayler.implementation.SnapshotPrevayler;
import org.prevayler.implementation.Snapshotter;

/**
   Responsible for tracking/managing transactions.  This responsiblity is split
   across two classes.  TxnManager handles control aspects whilst
   TxnManagerState tracks the transactional information. <P>

   We make our lives a little easier by making null transactions look like a
   normal transaction which keeps things consistent and clean bar an implied
   prepare/commit cycle in the operation code of SpaceImpl.  It also assists
   us in that the transaction code can be largely tested without an external
   transaction manager because a lot of it is exercised during the use of null
   operations. <P>

   The transaction subsystems logging and snapshot/checkpointing requirements
   are handled by holding all state in TxnManagerState and making it a
   <code>PrevalentSystem</code>.  Snapshots are treated as the equivalent of
   checkpoints and are triggered in a separate thread.  <P>

   Note that snapshots cannot be performed alongside the processing of commands
   thus, we take a write on a readerswriter lock during snapshot and release it
   after.  All other operations are performed under a readlock.  Note that the
   write lock must be asserted BEFORE invoking snapshot to avoid deadlock.
   The snapshot is only written to disk after Disk.sync completes.<P>

   The actual workings are a little different from the norm with TxnManager
   generating commands which act upon TxnManagerState.  Certain methods don't
   generate commands at all because they don't represent a true state change.
   Typically these methods are related to introducing new initial state into
   TxnManagerState such as a transaction which doesn't need to be made
   durable (saved to log etc.) until it reaches the prepared state. <P>

   Prepare records a durable record of the transaction's operations whilst
   commit and abort result in those operations being applied. <P>

   @see org.prevayler.PrevalentSystem
   @see org.prevayler.Command
   @see org.dancres.blitz.txn.TxnManagerState

   @todo Micro-optimization - we don't need to write an abort record if
   the txn isn't prepared - just clear it out!
 */
public class TxnManager {

    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.TxnManager", Level.INFO);

    private static boolean LOG_CKPTS;

    static {
        try {
            LOG_CKPTS =
                ((Boolean)
                 ConfigurationFactory.getEntry("logCkpts",
                                               Boolean.class,
                                               new Boolean(false))).booleanValue();
        } catch (ConfigurationException aCE) {
            theLogger.log(Level.SEVERE, "Couldn't load config", aCE);
        }
    }

    private static TxnManager theManager;

    private TxnManagerState theManagerState;

    private ReentrantReadWriteLock theLock = new ReentrantReadWriteLock();

    private SnapshotPrevayler thePrevayler;

    private CheckpointTrigger theCheckpointTrigger;

    private long theCheckpointCount;

    private TxnGateway theGateway;

    private boolean isRecovery;

    public static synchronized void init(TxnGateway aGateway)
        throws Exception {

        if (theManager != null) {
            return;
        } else {
            theManager = new TxnManager(aGateway);
            theManager.recover();
        }
    }

    public static synchronized void halt() {
        theManager = null;
    }
    
    public static synchronized TxnManager get() {
        return theManager;
    }

    private TxnManager(TxnGateway aGateway) {
        theGateway = aGateway;
    }

    private void recover() throws Exception {
        theCheckpointCount = 0;
        isRecovery = true;

        StoragePersonality myPersonality = 
            StoragePersonalityFactory.getPersonality();

        theLogger.log(Level.INFO, "Doing log recovery...");

        thePrevayler = myPersonality.getPrevayler(new TxnManagerState());

        theLogger.log(Level.INFO, "Log Recovery complete...");
        theLogger.log(Level.INFO, "Using prevayler: " + thePrevayler.getClass());
        
        theManagerState = (TxnManagerState) thePrevayler.system();

        theCheckpointTrigger =
            myPersonality.getCheckpointTrigger(new CheckpointerImpl(this));

        StatsBoard.get().add(new TxnStatGenerator());

        issueCheckpoint(true);

        isRecovery = false;

        // Startup the transaction pinger
        new TxnPinger(theManagerState);
    }

    /**
       @return <code>true</code> if the TransactionManager is performing
       recovery.  When we are performing recovery the log files cannot be
       written to by user code (that is code outside of the transaction
       package).
     */
    public boolean isRecovery() {
        return isRecovery;
    }

    public void add(SnapshotContributor aContributor) {
        theManagerState.add(aContributor);
    }
    
    public void remove(SnapshotContributor aContributor) {
        theManagerState.remove(aContributor);
    }

    public TxnGateway getGateway() {
        return theGateway;
    }

    /**
       @return the number of transactions currently being processed.  Includes
       ACTIVE, PREPARED, COMMITTED and ABORTED (where the last two states may be
       present depending on state with respect to log records etc).
     */
    int getActiveTxnCount() {
        int myActiveTxnCount;

        theLock.readLock().lock();

        myActiveTxnCount = theManagerState.getNumActiveTxns();

        theLock.readLock().unlock();

        return myActiveTxnCount;
    }

    public TxnState getTxnFor(TxnId anId)
        throws UnknownTransactionException, RemoteException {
        return theManagerState.getTxnFor(anId, theGateway, true);
    }

    /**
       Resolve a JINI transaction using this method before calling any of
       <code>prepare</code>, <code>commit</code>, <code>abort</code> or
       <code>prepareAndCommit</code>.
     */
    public TxnState getTxnFor(Transaction aTransaction, boolean mustExist) 
        throws UnknownTransactionException, RemoteException {

        TxnId myId = convertToId(aTransaction);
        return theManagerState.getTxnFor(myId, theGateway, mustExist);
    }

    /**
       Invoked as part of the path for incoming transaction control via
       the remote TransactionManager
     */
    public TxnState getTxnFor(TransactionManager aMgr, long anId)
        throws UnknownTransactionException, RemoteException {

        TxnId myId = convertToId(aMgr, anId);
        return theManagerState.getTxnFor(myId, theGateway, true);
    }

    /**
       This method is slightly more flexible then getTxnFor in that it will
       accept a <code>null</code> and return a new null transaction as well
       as the usual mapping of JINI txn to internal txn state.
     */
    public TxnState resolve(Transaction aTransaction)
        throws UnknownTransactionException, RemoteException {

        if (aTransaction == null)
            return newNullTxn();
        else
            return getTxnFor(aTransaction, false);
    }

    /**
       In cases where no explicit transaction has been passed in by a caller,
       create a null transaction which is an internal, fully transactional
       replacement which can be used for the duration of the operation
       in question.
     */
    public TxnState newNullTxn() throws RemoteException {
        return theManagerState.newNullTxn();
    }

    /**
       In cases where no state will be changed (no Entry's taken or written),
       create an instance of this transaction which, when commited or aborted
       will be undone but not logged.
     */
    public TxnState newIdentityTxn() throws RemoteException {
        return theManagerState.newIdentityTxn();
    }

    /**
       Doesn't throw a DbException because it just writes to disk.  Restore
       is coped with inside TxnManagerState when we're doing recovery and
       an Exception could be thrown then.
     */
    public int prepare(TxnState aState) throws UnknownTransactionException {

        try {
            theLock.readLock().lock();

            aState.vote();

            boolean dontLog = ((aState.isIdentity()) || (aState.hasNoOps()));

            Integer myResult = (Integer)
                execute(new PrepCommand(aState), dontLog);
            
            theLock.readLock().unlock();

            theCheckpointTrigger.loggedCommand();

            return myResult.intValue();

        } catch (InterruptedException anIE) {
            theLogger.log(Level.SEVERE, "Failed to log prepare", anIE);
            throw new UnknownTransactionException();
        } catch (Exception anE) {
            theLock.readLock().unlock();
            theLogger.log(Level.SEVERE, "Failed to log prepare", anE);
            throw new UnknownTransactionException();
        }
    }

    public void commit(TxnState aState) throws UnknownTransactionException {

        try {
            theLock.readLock().lock();

            boolean dontLog = ((aState.isIdentity()) || (aState.hasNoOps()));

            execute(new CommitCommand(aState.getId()), dontLog);
            
            theLock.readLock().unlock();

            aState.doFinalize();

            theCheckpointTrigger.loggedCommand();

        } catch (InterruptedException anIE) {
            theLogger.log(Level.SEVERE, "Failed to log commit", anIE);
            throw new UnknownTransactionException();
        } catch (Exception anE) {
            theLock.readLock().unlock();
            theLogger.log(Level.SEVERE, "Failed to log commit", anE);
            throw new UnknownTransactionException();
        }
    }

    public void abort(TxnState aState) throws UnknownTransactionException {

        try {
            theLock.readLock().lock();

            int myResultingState = aState.vote();

            /*
                We're in at least voting state and possibly prepared state.
                If at this point the txn is an identity transaction or
                it has no operations we needn't log it.  We also needn't log
                it in the case where we've not yet written a prepare record
                to disk which would be indicated by vote() returning
                VOTING as opposed to PREPARED.
             */
            boolean dontLog =
                ((aState.isIdentity()) || (aState.hasNoOps()) ||
                    (myResultingState == TransactionConstants.VOTING));

            execute(new AbortCommand(aState.getId()), dontLog);
            
            theLock.readLock().unlock();

            aState.doFinalize();

            theCheckpointTrigger.loggedCommand();

        } catch (InterruptedException anIE) {
            theLogger.log(Level.SEVERE, "Failed to log abort", anIE);
            throw new UnknownTransactionException();
        } catch (Exception anE) {
            theLock.readLock().unlock();
            theLogger.log(Level.SEVERE, "Failed to log abort", anE);
            throw new UnknownTransactionException();
        }
    }

    public int prepareAndCommit(TxnState aState)
        throws UnknownTransactionException {

        try {
            theLock.readLock().lock();

            aState.vote();

            boolean dontLog = ((aState.isIdentity()) || (aState.hasNoOps()));

            Integer myResult = (Integer)
                execute(new PrepCommitCommand(aState), dontLog);

            theLock.readLock().unlock();

            aState.doFinalize();

            theCheckpointTrigger.loggedCommand();

            return myResult.intValue();
            
        } catch (InterruptedException anIE) {
            theLogger.log(Level.SEVERE, "Failed to log prepCommit", anIE);
            throw new UnknownTransactionException();
        } catch (Exception anE) {
            theLock.readLock().unlock();
            theLogger.log(Level.SEVERE, "Failed to log prepCommit", anE);
            throw new UnknownTransactionException();
        }
    }

    /**
       Log a specific single action in a transaction of its own.  This is
       typically used by elements of Blitz that need to record some state
       transition that would need to be re-applied at recovery.
     */
    public void log(TxnOp anOp) throws TransactionException {
        tryLog(anOp, Long.MAX_VALUE);
    }

    /**
       Attempt to log an action.  The attempt is bounded by the specified
       timeout such that if the action cannot be logged within the time, it
       will be abandoned.

       @return <code>true</code> if the action was written, <code>false</code>
       otherwise.
    */
    public boolean tryLog(TxnOp anOp, long aTimeout)
        throws TransactionException {

        try {
            TxnState myEnclosing = newNullTxn();

            myEnclosing.add(anOp);

            if (theLock.readLock().tryLock(aTimeout, TimeUnit.MILLISECONDS)) {
                myEnclosing.vote();
                
                // Given the contract of timeout, we can make this loss'y'
                thePrevayler.executeCommand(new PrepCommitCommand(myEnclosing),
                        false);
                theLock.readLock().unlock();
                theCheckpointTrigger.loggedCommand();
                return true;
            } else
                return false;
        } catch (InterruptedException anIE) {
            theLogger.log(Level.SEVERE, "Failed to log Action", anIE);
            throw new TransactionException();
        } catch (Exception anE) {
            theLock.readLock().unlock();
            theLogger.log(Level.SEVERE, "Failed to log Action", anE);
            throw new TransactionException();
        }
    }

    public void abortAll() throws IOException {

        try {
            theLock.readLock().lock();

            // Has to be logged
            //
            thePrevayler.executeCommand(new AbortAllCommand());
            
            theLock.readLock().unlock();

            theCheckpointTrigger.loggedCommand();

        } catch (InterruptedException anIE) {
            theLogger.log(Level.SEVERE, "Failed to log abortAll", anIE);
            throw new IOException();
        } catch (Exception anE) {
            theLock.readLock().unlock();
            theLogger.log(Level.SEVERE, "Failed to log abortAll", anE);
            throw new IOException();
        }
    }

    private TxnId convertToId(TransactionManager aMgr, long anId)
        throws RemoteException {

        return new TxnId(aMgr, anId);
    }

    private TxnId convertToId(Transaction aTxn) throws RemoteException {
        ServerTransaction myTxn = (ServerTransaction) aTxn;
        
        return new TxnId(myTxn.mgr, myTxn.id);
    }

    /**
     * @todo There's a race condition here where state might change after we
     * check for in-memory execute or not leading to incorrect behaviour in
     * face of a crash.  This needs fixing whilst at the same time we should
     * address the issue of an abort on an ACTIVE transaction which therefore
     * needn't be logged.  What we need to do is modify each command to report
     * if it needs to be logged.  We then modify the prevayler to execute
     * the command and then determine if the outcome needs logging and log
     * if necessary.  Only the SnapshotPrevaylerImpl really cares about this
     * so only it needs tweaking.  This state should probably be deduced
     * by the TxnState itself or TxnManagerState - it certainly needs to be
     * done under the lock of the TxnState.  The decision points for whether
     * a transaction is idempotent are at initial prepare (where we discover
     * if there are no listeners or the txn is the identity txn) and at an
     * abort when the transaction it still active (and thus we haven't written
     * a log record).
     */
    private Serializable execute(Command aCommand, boolean dontLog)
        throws Exception {
        if (dontLog) {
            // System.err.println("Bypass");
            return aCommand.execute(theManagerState);
        } else {
            // System.err.println("Full log: " + thePrevayler.getClass());
            return thePrevayler.executeCommand(aCommand);
        }
    }

    /**
       Run a hot backup.  Basic contract is that we will catch all but the
       currently active transactions (whose effects will be confined to the
       cache anyway.

       @param aDestDir the mount point/directory to copy the files to.
     */
    public void hotBackup(String aDestDir) throws IOException {

        /*
          First do an unlocked sync to get most updates from the cache
          to disk.

          Then take the txn lock and perform the second sync part of
          which then does the file copy for backup purposes
         */
        try {
            Disk.sync();
        } catch (Exception anE) {
            IOException anIOE = new IOException("Failed to start backup (initial sync)");
            anIOE.initCause(anE);

            throw anIOE;
        }

        theLock.writeLock().lock();

        try {
            Disk.backup(aDestDir);
        } finally {
            theLock.writeLock().unlock();
        }
    }

    /**
       Used by external entities to request a snapshot which could be used
       by another Blitz instance - i.e. think of this as a copy type operation.

       @todo The checking of active transactions is not bullet proof because
       we could get a new transaction before we start the checkpoint.  Consider
       a barrier or some other fix (requires a decision on how to handle
       waiting for settling of transactions etc).  We're keeping it simple for
       now.
     */
    public void requestSnapshot() throws TransactionException, IOException {
        int myActiveTxnCount;

        theLock.readLock().lock();

        myActiveTxnCount = theManagerState.getNumActiveTxns();

        theLock.readLock().unlock();

        if (myActiveTxnCount != 0)
            throw new TransactionException(
                    "Cannot snapshot with active transactions it's bad for your data");

        CheckpointTask myTask = new CheckpointTask();

        try {
            Tasks.queue("checkpoints", myTask);
        } catch (InterruptedException anIE) {
            theLogger.log(Level.SEVERE, "Failed to queue checkpoint task",
                          anIE);
            throw new IOException("Failed to queue checkpoint task");
        }

        myTask.waitForCompletion();
    }

    /**
       Requests a checkpoint and blocks 'til completion.  This method is
       used internally by the Blitz core and unlike requestSnapshot makes
       no efforts to render the filesystem into a state which could be copied
       to another Blitz instance.
     */
    public void requestSyncCheckpoint() throws IOException {
        issueCheckpoint(true);
    }

    /**
       This method is used internally by the Blitz core and unlike
       requestSnapshot makes no efforts to render the filesystem into a state
       which could be copied to another Blitz instance.

       <ol>
       <li> Assert a write lock to prevent further commands from being
       logged. </li>
       <li> Snapshot the state of the PrevalentSystem (TxnManagerState) </li>
       <li> Snapshot results in changeover to a new log file.
       <li> Release the write lock to allow further commands to enter the new
       log file </li>
       <li> Invoke Disk.sync to flush dirty data to disk which will callback
       on completion </li>
       <li> Callback triggers writing of the snapshot which invalidates all
       log files previous to the one started above </li>
       </ol>

       <p>In the event of failure before sync'ing is complete - i.e. the
       snapshot has not been saved, log files from the previous snapshot
       onwards will be used to re-construct state.  If there's no previous
       snapshot, all log files will be replayed to reconstruct state. </p>

       @see org.dancres.blitz.disk.Disk
     */
    void requestAsyncCheckpoint() throws IOException {
        issueCheckpoint(false);
    }

    /**
       Internal implementation of the checkpoint operation used by snapshot
       and checkpoint methods above.

       @param isBlocking specifies whether to block the caller until the
       checkpoint completes
     */
    private void issueCheckpoint(boolean isBlocking) throws IOException {

        // Only checkpoint if the trigger says it's okay
        if (theCheckpointTrigger.checkpointsDisabled())
            return;

        try {
            long myCkptId;

            synchronized(this) {
                myCkptId = theCheckpointCount++;
            }

            if (LOG_CKPTS)
                theLogger.log(Level.INFO, "Checkpoint::start: " + myCkptId);

            theLock.writeLock().lock();

            // Issue tentative checkpoint - change over logs
            // and carry over prepared state from old log in snapshotter
            // which will be commited/aborted in the new log
            Snapshotter mySnapper = thePrevayler.takeSnapshot();

            theLock.writeLock().unlock();

            // Now sync disks and save snapshot at completion
            if (isBlocking) {
                BlockingSnapshotTask myTask =
                    new BlockingSnapshotTask(myCkptId, mySnapper);

                Disk.sync(myTask);
                myTask.waitForCompletion();
            } else {
                Disk.sync(new SnapshotTask(myCkptId, mySnapper));
            }

            if (LOG_CKPTS)
                theLogger.log(Level.INFO, "Checkpoint::end: " + myCkptId);

        } catch (InterruptedException anIE) {
            theLogger.log(Level.SEVERE, "Failed to get lock for ckpt", anIE);
            throw new IOException("Failed to lock for ckpt");
        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Failed to sync", anE);
            throw new IOException("Failed to sync");
        }
    }

    /**
       An instance of this object is passed to Disk.sync.  It's run method
       will be called once Disk has completed the sync task.  When called,
       it saves the snapshot to disk which obsoletes logs previous to the
       snapshot (where previous is defined as a log with a sequence number
       less than that of the snapshot).
     */
    class SnapshotTask implements Runnable {
        private Snapshotter theSnapper;
        private long theCkptId;

        SnapshotTask(long aCkptId, Snapshotter aSnapper) {
            theCkptId = aCkptId;
            theSnapper = aSnapper;
        }

        public void run() {
            try {
                if (LOG_CKPTS)
                    theLogger.log(Level.INFO, 
                                  "Disks sync'd - save snapshot: " +
                                  theCkptId);
                theSnapper.save();

                if (LOG_CKPTS)
                    theLogger.log(Level.INFO, "Snapshot saved: " + theCkptId);
            } catch (IOException anIOE) {
                theLogger.log(Level.SEVERE, "Failed to save snapshot on sync",
                              anIOE);
            }
        }
    }

    class BlockingSnapshotTask implements Runnable {
        private Snapshotter theSnapper;
        private long theCkptId;
        private boolean isComplete;

        BlockingSnapshotTask(long aCkptId, Snapshotter aSnapper) {
            theCkptId = aCkptId;
            theSnapper = aSnapper;
        }

        public void run() {
            try {
                if (LOG_CKPTS)
                    theLogger.log(Level.INFO, 
                                  "Disks sync'd - save snapshot: " +
                                  theCkptId);
                theSnapper.save();

                synchronized(this) {
                    isComplete = true;
                    notify();
                }

                if (LOG_CKPTS)
                    theLogger.log(Level.INFO, "Snapshot saved: " + theCkptId);
            } catch (IOException anIOE) {
                theLogger.log(Level.SEVERE, "Failed to save snapshot on sync",
                              anIOE);
            }
        }

        void waitForCompletion() {
            synchronized(this) {
                while(! isComplete) {
                    try {
                        wait();
                    } catch (InterruptedException anIE) {
                    }
                }
            }
        }
    }
}
