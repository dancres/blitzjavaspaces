package org.dancres.blitz;

import java.io.IOException;

import java.rmi.MarshalledObject;

import java.util.List;
import java.util.ArrayList;

import java.util.Set;
import java.util.logging.*;

import net.jini.core.event.RemoteEventListener;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.entry.EntryRepositoryFactory;
import org.dancres.blitz.entry.EntryRepository;
import org.dancres.blitz.entry.OpInfo;

import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.DiskTxn;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.util.Time;

import org.dancres.blitz.notify.EventQueue;
import org.dancres.blitz.notify.QueueEvent;

import org.dancres.blitz.lease.SpaceUID;
import org.dancres.blitz.lease.LeaseBounds;

import org.dancres.blitz.txn.TxnManager;
import org.dancres.blitz.txn.TxnState;
import org.dancres.blitz.txn.TxnGateway;

import org.dancres.blitz.config.ConfigurationFactory;
import org.dancres.blitz.config.Fifo;

import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.stats.Stat;

/**
   <p>The core back-end implementation of a space. </p>

   <p>One of the more subtle responsibilities of this class is that it handles
   the negotiation of lease durations.  This has implications for remote layers
   etc. which should expect to pass down an unadulterated lease duration
   (including <code>Lease.FOREVER</code> and <code>Lease.ANY</code>) and have
   the space return the actual resultant lease time.  This is true for
   both initial writes of entry's or notify registrations and future
   renews.</p>

   <p>Another responsibility of this class is to ensure that it only
   returns <code>SpaceUID</code>s which are space-global unique identifiers.
   There are a number of different resources managed by the space core which
   each have their own locally unique identifiers.  Thus the core wraps these
   in appropriate <code>SpaceUID</code> implementations before returning
   them.</p>

   <p>The various <code>SpaceUID</code> implementations can be used as the
   target of lease renewal and cancel operations.  Each implementation,
   typically has it's own <code>LeaseHandler</code> implementation which is
   registered with <code>LeaseHandlers</code> and has the renewal/cancel
   operations delegated to it.</p>

   <p><code>Entry</code>s are managed by EntryRepository instances.</p>

   <p><code>notify</code>s are delegated to EventQueue.</p>

   @see org.dancres.blitz.lease.SpaceUID
   @see org.dancres.blitz.lease.LeaseHandler
   @see org.dancres.blitz.lease.LeaseHandlers
   @see org.dancres.blitz.entry.EntryRepository
   @see org.dancres.blitz.notify.EventQueue
 */
public class SpaceImpl {

    static final Logger theLogger =
        Logging.newLogger("org.dancres.blitz.SpaceImpl", Level.INFO);

    private TxnControl theTxnController = new TxnControlImpl();
    private LeaseControl theLeaseController = new LeaseControlImpl();

    private long theStartTime;

    private boolean isSynchronousNotify = false;

    public SpaceImpl(TxnGateway aGateway) throws Exception {
        VersionInfo.dump();

        theStartTime = System.currentTimeMillis();

        long myDebugCycle =
            ((Long)
             ConfigurationFactory.getEntry("statsDump",
                                           long.class,
                                           new Long(0))).longValue();

        isSynchronousNotify = ((Boolean)
                ConfigurationFactory.getEntry("syncNotifyOnWrite",
                                              boolean.class,
                                              new Boolean(false))).booleanValue();

        theLogger.info("Synchrounous Notifies: " + isSynchronousNotify);

        LifecycleRegistry.init();

        TxnManager.init(aGateway);

        EventQueue.get();
        
        // Activate threads etc. only after TxnManager is initialised so
        // state has been recovered/is stable.
        //
        ActiveObjectRegistry.startAll();

        // HACK: Ensure root repository is loaded because that publishes
        // the known types stats (we shouldn't know this!)
        EntryRepositoryFactory.get().get(EntryRepository.ROOT_TYPE);

        StatsDumper.start(myDebugCycle);
    }

    public WriteTicket write(MangledEntry anEntry, Transaction aTxn,
                             long aLeaseTime)
        throws IOException, TransactionException {

        TxnState myJiniTxn = TxnManager.get().resolve(aTxn);

        long myLeaseTime =
            Time.getAbsoluteTime(LeaseBounds.boundWrite(aLeaseTime));

        EntryRepository myRepos =
            EntryRepositoryFactory.get().get(anEntry.getType());

        if (myRepos.noSchemaDefined()) {
            DiskTxn myTxn = DiskTxn.newTxn();

            myRepos.setFields(anEntry.getFields());

            /*
              Update parent repositories - each parent needs to know about
              this subtype
            */
            String[] myParents = anEntry.tearOffParents();

            for (int i = 0; i < myParents.length; i++) {
                EntryRepository myParentRepos =
                    EntryRepositoryFactory.get().get(myParents[i]);
                myParentRepos.addSubtype(anEntry.getType());
            }

            myTxn.commit();
        }

        /*
          Invoke write on the repository and then bundle up the OID into
          an appropriate Lease form?
        */
        OID myOID;

        WriteEscortImpl myEscort = new WriteEscortImpl(myJiniTxn);

        myRepos.write(anEntry, myLeaseTime, myEscort);

        OpInfo myResult = myEscort.getInfo();

        myOID = myResult.getOID();

        /*
          Post an event for notifies and blockers
        */
        QueueEvent myEvent =
            new QueueEvent(QueueEvent.ENTRY_WRITE,
                myJiniTxn,
                new QueueEvent.Context(anEntry, myOID));

        if (isSynchronousNotify)
            EventQueue.get().add(myEvent, true);
        else
            EventQueue.get().add(myEvent);

        // For null transactions we must issue the commit
        if (myJiniTxn.isNull())
            TxnManager.get().prepareAndCommit(myJiniTxn);

        SpaceUID mySUID = new SpaceEntryUID(anEntry.getType(), myOID);
        return new WriteTicketImpl(mySUID, myLeaseTime);
    }

    private MangledEntry find(MangledEntry anEntry, Transaction aTxn,
                              long aWaitTime, boolean doTake, boolean ifExists)
        throws IOException, TransactionException {

        MangledEntry myEntry = null;

        TxnState myJiniTxn = TxnManager.get().resolve(aTxn);

        EntryRepository myRepos =
            EntryRepositoryFactory.get().find(anEntry.getType());

        SingleMatchTask myTask;
        VisitorBaulkedPartyFactory myFactory;

        if (ifExists)
            myFactory = new ExistsFactory();
        else
            myFactory = new SearchFactory();

        if ((myRepos == null) ||
            (myRepos.getConstraints().get(Fifo.class) == null)) {

            /*
              If repos doesn't yet exist, we're running on a first come
              first served basis - it only makes sense to perform enforce
              fifo when we've accrued some live Entry's in storage where
              we wish to select those ahead of incoming matches.

              In terms of priority for incoming matches, SearchTasks natural
              order will ensure that the oldest blocking task gets the incoming
              matches before anyone else
            */
            myTask = new SearchVisitorImpl(anEntry, doTake,
                                           myJiniTxn, myFactory);
        } else {
            myTask = new FifoSearchVisitorImpl(anEntry, doTake,
                                               myJiniTxn, myFactory);
        }

        if (myRepos != null) {
            myRepos.find(anEntry, myTask.getVisitor());

            if (myTask.wouldBlock()) {

                // Try subtypes
                Set<String> mySubtypes = myRepos.getSubtypes();

                for (String t: mySubtypes) {
                    myRepos = EntryRepositoryFactory.get().find(t);

                    if (myRepos != null) {
                        myRepos.find(anEntry, myTask.getVisitor());

                        // If we got a match from the search (or it could
                        // have come from a recent write)
                        if (!myTask.wouldBlock())
                            break;
                    }
                }
            }
        } else {
            // Don't bother as the only sensible thing to do is wait
            // which will happen below
        }

        try {
            // Result waiting?
            if (!myTask.wouldBlock())
                myEntry = myTask.getEntry(0);
            else {
                // Will optionally force early exit if there were no conflicts
                myFactory.enableResolutionSignal();

                myEntry =
                    myTask.getEntry(aWaitTime);
            }
        } catch (InterruptedException anIE) {
            throw new TransactionException("Search interrupted");
        } catch (TransactionException aTE) {
            if (myJiniTxn.isNull())
                TxnManager.get().abort(myJiniTxn);

            throw aTE;
        }

        // If we started the txn we MUST finish it, entry or not
        if (myJiniTxn.isNull()) {
            TxnManager.get().prepareAndCommit(myJiniTxn);
        }

        return myEntry;
    }

    public MangledEntry take(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime)
        throws IOException, TransactionException {

        return find(anEntry, aTxn, aWaitTime, true, false);
    }

    public MangledEntry read(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime)
        throws IOException, TransactionException {

        return find(anEntry, aTxn, aWaitTime, false, false);
    }

    public MangledEntry takeIfExists(MangledEntry anEntry, Transaction aTxn,
                                     long aWaitTime)
        throws IOException, TransactionException {

        return find(anEntry, aTxn, aWaitTime, true, true);
    }

    public MangledEntry readIfExists(MangledEntry anEntry, Transaction aTxn,
                                     long aWaitTime)
        throws IOException, TransactionException {

        return find(anEntry, aTxn, aWaitTime, false, true);
    }

    public RegTicket notify(MangledEntry aTemplate, Transaction aTxn,
                            RemoteEventListener aListener, long aLeaseTime,
                            MarshalledObject aHandback)
        throws IOException, TransactionException {

        TxnState myState = null;

        if (aTxn != null) {
            try {
                myState = TxnManager.get().getTxnFor(aTxn, false);
            } catch (UnknownTransactionException aUTE) {
                throw new TransactionException();
            }
        }

        long myLeaseTime =
            Time.getAbsoluteTime(LeaseBounds.boundNotify(aLeaseTime));

        RegTicketImpl myTicket =
            new RegTicketImpl(myLeaseTime);

        EventQueue.get().register(aTemplate, myState, aListener,
                                  myLeaseTime, aHandback, myTicket);
        return myTicket;
    }

    public RegTicket visibility(MangledEntry[] aTemplates, Transaction aTxn,
                                RemoteEventListener aListener, long aLeaseTime,
                                MarshalledObject aHandback, boolean visibleOnly)
        throws IOException, TransactionException {

        TxnState myState = null;

        if (aTxn != null) {
            try {
                myState = TxnManager.get().getTxnFor(aTxn, false);
            } catch (UnknownTransactionException aUTE) {
                throw new TransactionException();
            }
        }

        long myLeaseTime =
            Time.getAbsoluteTime(LeaseBounds.boundNotify(aLeaseTime));

        RegTicketImpl myTicket =
            new RegTicketImpl(myLeaseTime);

        EventQueue.get().registerVisibility(aTemplates, myState, aListener,
                                            myLeaseTime, aHandback, myTicket,
                                            visibleOnly);
        return myTicket;
    }

    /**
     * Call this method to obtain a collection of matches available within the
     * currently active instance against the specified templates.
     *
     * @param holdLocks if <code>true</code> indicates that read locks should
     * be held against the specified transaction rather than just tested
     * @param shouldUpdate if <code>true</code> will cause this view to be
     * dynamically updated with new writes after initial scan of contents
     * @param aMax is the maximum number of Entry's to return
     */
    public EntryView getView(MangledEntry[] aTemplates, Transaction aTxn,
                             boolean holdLocks, boolean shouldUpdate, long aMax)
        throws IOException, TransactionException {

        return new EntryViewImpl(aTxn, aTemplates, holdLocks,
            shouldUpdate, aMax);
    }

    /**
     * Call this method to obtain a collection of matches available within the
     * currently active instance against the specified templates.  The view
     * returned will be dynamically updated.
     *
     * @param holdLocks if <code>true</code> indicates that read locks should
     * be held against the specified transaction rather than just tested
     * @param aMax is the maximum number of Entry's to return
     *
     */
    public EntryView getView(MangledEntry[] aTemplates, Transaction aTxn,
                             boolean holdLocks, long aMax)
        throws IOException, TransactionException {

        return new EntryViewImpl(aTxn, aTemplates, holdLocks, true, aMax);
    }

    public List write(List aMangledEntries,
                      Transaction aTxn,
                      List aLeaseTimes)
        throws IOException, TransactionException {

        ArrayList myTickets = new ArrayList();

        TxnState myJiniTxn = TxnManager.get().resolve(aTxn);

        WriteEscortImpl myEscort = new WriteEscortImpl(myJiniTxn);

        int myLast = aMangledEntries.size() - 1;

        for (int i = 0; i < aMangledEntries.size(); i++) {
            MangledEntry myEntry = (MangledEntry) aMangledEntries.get(i);
            long myLongLease = ((Long) aLeaseTimes.get(i)).longValue();

            EntryRepository myRepos =
                EntryRepositoryFactory.get().get(myEntry.getType());

            if (myRepos.noSchemaDefined()) {
                DiskTxn myTxn = DiskTxn.newTxn();

                myRepos.setFields(myEntry.getFields());

                /*
                  Update parent repositories - each parent needs to know about
                  this subtype
                */
                String[] myParents = myEntry.tearOffParents();

                for (int j = 0; j < myParents.length; j++) {
                    EntryRepository myParentRepos =
                        EntryRepositoryFactory.get().get(myParents[j]);
                    myParentRepos.addSubtype(myEntry.getType());
                }

                myTxn.commit();
            }

            /*
              Invoke write on the repository and then bundle up the OID into
              an appropriate Lease form?
            */
            OID myOID;

            long myLeaseTime =
                Time.getAbsoluteTime(LeaseBounds.boundWrite(myLongLease));

            myRepos.write(myEntry, myLeaseTime, myEscort);

            OpInfo myResult = myEscort.getInfo();

            myOID = myResult.getOID();

            /*
              Post an event for notifies and blockers
            */
            QueueEvent myEvent =
                    new QueueEvent(QueueEvent.ENTRY_WRITE,
                            myJiniTxn,
                            new QueueEvent.Context(myEntry, myOID));

            /*
              Only need to post synchronously on the last write
            */
            if ((isSynchronousNotify) && (i == myLast))
                EventQueue.get().add(myEvent, true);
            else
                EventQueue.get().add(myEvent);

            SpaceUID mySUID = new SpaceEntryUID(myEntry.getType(), myOID);
            myTickets.add(new WriteTicketImpl(mySUID, myLeaseTime));
        }

        if (myJiniTxn.isNull()) {
            TxnManager.get().prepareAndCommit(myJiniTxn);
        }

        return myTickets;
     }

     public List take(MangledEntry[] aTemplates,
                      Transaction aTxn,
                      long aWaitTime,
                      long aLimit)
         throws TransactionException, IOException {

         TxnState myJiniTxn = TxnManager.get().resolve(aTxn);

         BulkMatchTask myVisitor =
             new BulkTakeVisitor(aTemplates, myJiniTxn, aLimit,
                 new SearchFactory());

         EntryRepository myRepos;

         // For each template
         for (int i = 0; i < aTemplates.length; i++) {
             myRepos =
                 EntryRepositoryFactory.get().find(aTemplates[i].getType());

             // If we have a repository rooted at the type
             if (myRepos != null) {
                 myRepos.find(aTemplates[i], myVisitor.getVisitor());

                 // Did we satisfy the visitor already?
                 if (myVisitor.wantsMore()) {

                     // If not, try subtypes of the current type
                     Set<String> mySubtypes = myRepos.getSubtypes();

                     for (String t: mySubtypes) {
                         myRepos =
                             EntryRepositoryFactory.get().find(t);

                         if (myRepos != null) {
                             myRepos.find(aTemplates[i], myVisitor.getVisitor());

                             if (!myVisitor.wantsMore())
                                 break;
                         }
                     }
                 }
             }

             if (!myVisitor.wantsMore())
                 break;
         }

         try {
             if (!myVisitor.wouldBlock())
                 return myVisitor.getEntries(0);
             else {
                 return myVisitor.getEntries(aWaitTime);
             }
         } catch (InterruptedException anIE) {
             throw new TransactionException("Search interrupted");
         } finally {
             if (myJiniTxn.isNull())
                 TxnManager.get().prepareAndCommit(myJiniTxn);
         }
     }

    public LeaseControl getLeaseControl() {
        return theLeaseController;
    }

    public TxnControl getTxnControl() {
        return theTxnController;
    }

    public void stop() throws Exception {
        ActiveObjectRegistry.stopAll();

        theLogger.log(Level.INFO, "Dumping stats");
        Stat[] myStats = StatsBoard.get().getStats();
        for (int i = 0; i < myStats.length; i++) {
            theLogger.log(Level.INFO, myStats[i].getId() + ", " + myStats[i]);
        }

        TxnManager.halt();

        Disk.sync();

        Disk.stop();

        LifecycleRegistry.deinit();
        
        theLogger.log(Level.INFO, "Blitz core halted after: " +
                           (System.currentTimeMillis() - theStartTime) +
                           " ms");
    }

    /**
       Clear out all Entry's including schema information.
       This is necessarily very destructive as it aborts operations and
       open transactions and deletes a lot of underlying database state.

       @todo Need to replace the use of SearchTasks.destroy() - should be
       possible via the transaction abort all route - need to check
     */
    public void empty() throws IOException {
        // Kill outstanding search tasks because we plan to drop schema
        // and thus their templates are rendered invalid
        // SearchTasks.get().destroy();

        // Abort all transactions to release locks
        TxnManager.get().abortAll();

        EntryRepositoryFactory.get().deleteAllEntrys();

        // Checkpoint
        TxnManager.get().requestSyncCheckpoint();

        EntryRepositoryFactory.get().deleteAllRepos();
    }

    /**
       Triggers a manual reap in a new thread
     */
    public void reap() {
        Thread myReaper =
            new Thread(new Runnable() {
                    public void run() {
                        EntryRepositoryFactory.reap();
                    }
                });

        myReaper.start();
    }
}
