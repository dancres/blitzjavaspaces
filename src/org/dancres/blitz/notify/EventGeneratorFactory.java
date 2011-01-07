package org.dancres.blitz.notify;

import java.rmi.MarshalledObject;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import net.jini.core.event.RemoteEventListener;

import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.disk.DiskTxn;
import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.Syncable;

import org.dancres.blitz.meta.Registry;
import org.dancres.blitz.meta.RegistryAccessor;
import org.dancres.blitz.meta.RegistryFactory;
import org.dancres.blitz.meta.MetaIterator;
import org.dancres.blitz.meta.MetaEntry;

import org.dancres.blitz.oid.AllocatorFactory;
import org.dancres.blitz.oid.Allocator;
import org.dancres.blitz.oid.OID;
import org.dancres.blitz.oid.OIDFactory;

import org.dancres.blitz.txn.TxnState;
import org.dancres.blitz.txn.TxnManager;
import org.dancres.blitz.txn.TxnOp;

import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.stats.StatGenerator;
import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.EventQueueStat;

/**
   Handles all the storage aspects associated with EventGenerators including
   allocation, deletion and sync'ing to disk.  It maintains an up-to-date
   list of active EventGenerators and allows them to be transient (never
   saved) or persistent as required by the caller.  Transient EventGenerators
   are used for transaction related notify requests whilst persistent
   EventGenerators have no associated transaction.
 */
class EventGeneratorFactory implements Syncable {
    static final String STORE_NAME = "BlitzEventGens";

    private static EventGeneratorFactory theFactory
        = new EventGeneratorFactory();

    /**
       Store for persistent EventGenerators
     */
    private Registry theStore;

    /**
       OID Allocator for EventGenerator instances
     */
    private Allocator theAllocator;

    private ConcurrentSkipListSet<EventGenerator> theGens = new ConcurrentSkipListSet<EventGenerator>();

    /**
     * Because ConcurrentSkipListSet.size is not a constant time operation, we track size independently.
     */
    private AtomicInteger theNumGens = new AtomicInteger();

    static EventGeneratorFactory get() {
        return theFactory;
    }

    private EventGeneratorFactory() {
        StatsBoard.get().add(new GeneratorImpl());
    }

    private class GeneratorImpl implements StatGenerator {
        private long _id;


        public long getId() {
            return _id;
        }

        public void setId(long anId) {
            _id = anId;
        }

        public Stat generate() {
            try {
                Iterator myGens = getGenerators();

                int myTransient = 0, myPersistent = 0;

                while (myGens.hasNext()) {
                    EventGenerator myGen = (EventGenerator) myGens.next();
                    if (myGen.isPersistent())
                        ++myPersistent;
                    else
                        ++myTransient;
                }

                return new EventQueueStat(_id, myPersistent, myTransient);
            } catch (Exception anE) {
                return new EventQueueStat(_id, -1, -1);
            }
        }
    }

    Iterator getGenerators() throws IOException {
        loadBarrier();

        return theGens.iterator();
    }

    int getCount() {
        return theNumGens.get();
    }
    
    private void loadAllocator() throws IOException {
        theAllocator = AllocatorFactory.get(STORE_NAME, 64, false);
    }

    private synchronized void loadBarrier() throws IOException {
        if (theStore == null) {
            theStore = RegistryFactory.get(STORE_NAME, null);

            DiskTxn myTxn = DiskTxn.newTxn();

            MetaIterator myState = theStore.getAccessor().readAll();

            MetaEntry myEntry;

            while ((myEntry = myState.fetch()) != null) {
                EventGeneratorState myGenState =
                    (EventGeneratorState) myEntry.getData();
                EventGenerator myGenerator = myGenState.getGenerator();
                    
                theGens.add(myGenerator);
                theNumGens.incrementAndGet();
            }

            myState.release();
            
            myTxn.commit();

            loadAllocator();

            Disk.add(this);
        }
    }

    EventGenerator newPersistentGenerator(MangledEntry aTemplate,
                                          RemoteEventListener aListner,
                                          long aLeaseTime,
                                          MarshalledObject aHandback)
        throws IOException {

        EventGenerator myGenerator =
            new EventGeneratorImpl(aTemplate, aHandback,
                                   aListner, 0, aLeaseTime);

        addReg(myGenerator);

        return myGenerator;
    }

    EventGenerator newPersistentVisibility(MangledEntry[] aTemplates,
                                           RemoteEventListener aListner,
                                           long aLeaseTime,
                                           MarshalledObject aHandback,
                                           boolean visibleOnly)
        throws IOException {

        EventGenerator myGenerator =
            new VisibilityImpl(aTemplates, aHandback,
                               aListner, 0, aLeaseTime,
                               visibleOnly);

        addReg(myGenerator);

        return myGenerator;
    }

    /**
       @todo Do we need to worry about logging these in light of the
       changeover to SyncBarrier based recovery?
     */
    EventGenerator newTransientGenerator(MangledEntry aTemplate,
                                         RemoteEventListener aListner,
                                         long aLeaseTime,
                                         MarshalledObject aHandback,
                                         TxnState aTxn)
        throws IOException {

        EventGenerator myGenerator =
            new EventGeneratorImpl(aTemplate, aHandback, aListner,
                                   0, aLeaseTime,
                                   aTxn.getId());

        addReg(myGenerator);

        return myGenerator;
    }

    /**
       @todo Do we need to worry about logging these in light of the
       changeover to SyncBarrier based recovery?
     */
    EventGenerator newTransientVisibility(MangledEntry[] aTemplates,
                                          RemoteEventListener aListner,
                                          long aLeaseTime,
                                          MarshalledObject aHandback,
                                          TxnState aTxn, boolean visibleOnly)
        throws IOException {

        EventGenerator myGenerator =
            new VisibilityImpl(aTemplates, aHandback, aListner,
                               0, aLeaseTime,
                               aTxn.getId(), visibleOnly);

        addReg(myGenerator);

        return myGenerator;
    }

    public void addTemporary(EventGenerator anEventGenerator)
        throws IOException {

        if (anEventGenerator.isPersistent())
            throw new IOException("Generator should be reporting transient!");
        
        anEventGenerator.assign(newOID());
        insert(anEventGenerator);
    }

    private OID newOID() throws IOException {
        OID myOID = theAllocator.getNextId();

        return myOID;
    }

    private void addReg(EventGenerator aGen) throws IOException {
        loadBarrier();

        aGen.assign(newOID());

        /*
            If this generator is transient we log purely so that we can reset
            the Allocator during recovery - we don't actually restore the
            registration
         */
        try {
            TxnManager.get().log(new RegistrationOp(aGen.getMemento()));
        } catch (TransactionException anE) {
            IOException myException =
                new IOException("Failed to log registration");
            myException.initCause(anE);
            throw myException;
        }

        insert(aGen);
    }

    private void insert(EventGenerator aGen) {
        // synchronized (this) {
            theGens.add(aGen);
            theNumGens.incrementAndGet();
        // }
    }

    private EventGenerator find(OID anOID) {
        for (EventGenerator g: theGens) {
            if (g.getId().equals(anOID))
                return g;
        }

        return null;
    }

    boolean renew(OID aOID, long anExpiry)
        throws IOException {

        loadBarrier();

        // synchronized(this) {
            EventGenerator myGen = find(aOID);

            if (myGen == null)
                return false;
            else
                return myGen.renew(anExpiry);
        // }
    }

    boolean cancel(OID aOID)
        throws IOException {

        loadBarrier();

        // synchronized(this) {
            EventGenerator myGen = find(aOID);
            
            if (myGen == null)
                return false;
            else {
                // Prevent further event processing
                //
                myGen.taint();
                killTemplate(aOID);
                return true;
            }
        // }
    }

    /**
       Invoked to restore a registration.  Re-insert EventGenerator if it's
       not present.
     */
    void recover(EventGeneratorState aState) throws IOException {
        loadBarrier();

        // synchronized(this) {
            /*
              We record all registrations but only fully restore the
              non-transactional ones.
            */
            if (aState.isPersistent()) {
                if (find(aState.getOID()) == null) {
                    EventGenerator myGenerator = aState.getGenerator();
                    insert(myGenerator);
                }
            }
        // }
    }

    /**
       Invoked to restore a sequence number increment.  Only applied if
       the generator already exists from a previous checkpoint or registration
       restore.
     */
    void recover(OID aOID, long aSeqNum) throws IOException {
        loadBarrier();

        // synchronized(this) {
            EventGenerator myGen = find(aOID);

            if (myGen != null)
                myGen.recover(aSeqNum);
        // }
    }

    /**
       Moved all available EventGenerator sequence numbers forward by
       RESTART_JUMP
     */
    void jumpSequenceNumbers() throws IOException {
        loadBarrier();

        ArrayList myJumps = new ArrayList();

        // synchronized(this) {
            Iterator myGenerators = theGens.iterator();

            while (myGenerators.hasNext()) {
                EventGenerator myGenerator =
                    (EventGenerator) myGenerators.next();

                long mySeqNum = myGenerator.jumpSequenceNumber();

                myJumps.add(new SequenceJumpRecord(myGenerator.getId(),
                                                   mySeqNum));
            }
        // }

        for (int i = 0; i < myJumps.size(); i++) {
            TxnOp myAction = (TxnOp) myJumps.get(i);

            try {
                TxnManager.get().log(myAction);
            } catch (TransactionException aTE) {
                EventQueue.theLogger.log(Level.SEVERE,
                                         "Failed to log restart jump", aTE);
            }
        }
    }

    void jumpSequenceNumber(OID aOID, long aJump) throws IOException {
        loadBarrier();

        // synchronized(this) {
            EventGenerator myGen = find(aOID);
            
            if (myGen != null)
                myGen.jumpSequenceNumber(aJump);
        // }
    }

    void killTemplate(EventGenerator aGen) throws IOException {
        boolean removed = (aGen != null) ? theGens.remove(aGen) : false;

        if (removed) {
            theNumGens.decrementAndGet();

            if (aGen.isPersistent()) {

                DiskTxn myTxn = DiskTxn.newStandalone();

                OID myOID = aGen.getId();
                theStore.getAccessor(myTxn).delete(OIDFactory.getKey(myOID));
                myTxn.commit(true);
            }
        }
    }

    void killTemplate(OID aOID) throws IOException {
        killTemplate(find(aOID));
    }

    public void sync() throws Exception {
        if (theStore == null)
            return;

        DiskTxn myTxn = DiskTxn.newStandalone();

        RegistryAccessor myAccessor = theStore.getAccessor(myTxn);

        Iterator myGenerators = theGens.iterator();

        while (myGenerators.hasNext()) {
            EventGenerator myGenerator =
                (EventGenerator) myGenerators.next();

            if (myGenerator.isPersistent()) {
                synchronized(myGenerator) {
                    myAccessor.save(OIDFactory.getKey(myGenerator.getId()),
                                    myGenerator.getMemento());
                }
            }
        }

        myTxn.commit(true);
    }

    public synchronized void close() throws Exception {
        if (theStore == null)
            return;

        theStore.close();

        theStore = null;
        theGens.clear();
    }
}
