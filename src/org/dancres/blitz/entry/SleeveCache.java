package org.dancres.blitz.entry;

import java.io.IOException;

import java.util.ArrayList;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.*;

import net.jini.core.transaction.TransactionException;
import net.jini.config.ConfigurationException;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.Logging;
import org.dancres.blitz.stats.StatGenerator;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.StoreStat;
import org.dancres.blitz.stats.StatsBoard;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.arc.ArcCache;
import org.dancres.blitz.arc.CacheBlockDescriptor;
import org.dancres.blitz.arc.RecoverySummary;

import org.dancres.blitz.txn.TxnManager;

import org.dancres.blitz.entry.ci.CacheIndexer;

import org.dancres.blitz.config.CacheSize;
import org.dancres.blitz.config.Fifo;
import org.dancres.blitz.config.EntryConstraints;

import org.dancres.blitz.config.ConfigurationFactory;

/**
   The organization of the space implementation can be viewed as being
   similar to a memory hierarchy with EntryReposImpl/SpaceImpl being where
   central processing happens.  SleeveCache can be viewed as level 1 cache
   whilst storage is main memory.  Thus all "traffic" goes through SleeveCache
   which interacts appropriately with Storage when it cannot satisfy the
   demand itself. <P>

   Is responsible for indexing and caching of unpacked EntrySleeveImpls. 
   Implemented using an ArcCache.<P>

   @see org.dancres.blitz.arc.ArcCache

   @todo One way to split caches down further in the face of concurrency
   demands would be to start maintaining multiple caches scoped or hashed on
   the zone id or even the uid itself.
 */
class SleeveCache implements StatGenerator {
    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.disk.SleeveCache");

    private static int DESIRED_ENTRIES_PER_PARTITION = 0;

    static {
        try {
            DESIRED_ENTRIES_PER_PARTITION =
                    ((Integer) ConfigurationFactory.getEntry("cacheEntriesPerPartition", int.class,
                    new Integer(128))).intValue();

            theLogger.log(Level.SEVERE, "Loaded config: " +
                    ((Integer) ConfigurationFactory.getEntry("cacheEntriesPerPartition", int.class,
                    new Integer(32))).intValue());
            
        } catch (ConfigurationException aCE) {
            theLogger.log(Level.SEVERE, "Failed to source partition setup", aCE);
        }
    }

    private final ArcCache[] theStoreCaches;
    private final CacheSize theCacheSize;
    private final int theNumPartitions;
    private final int thePartitionsMask;

    private Storage theStore;
    private CountersImpl theCounters;
    private EntryConstraints theConstraints;
    private CacheIndexer theIndexer;

    private long theId = StatGenerator.UNSET_ID;

    private static class OfferTracker {
        private String theType;
        private AtomicLong theMissed = new AtomicLong();
        private AtomicLong theDeld = new AtomicLong();

        OfferTracker(String aType) {
            theType = aType;
        }

        void incMissed() {
            theMissed.incrementAndGet();
        }

        void incDeld() {
            theDeld.incrementAndGet();
        }

        public String getTitle() {
            return theType;
        }

        public long getMisses() {
            return theMissed.get();
        }

        public long getDeld() {
            return theDeld.get();
        }
    }

    private static final int CACHED_TRACKER = 0;
    private static final int DIRTY_TRACKER = 1;
    private static final int STORAGE_TRACKER = 2;

    private OfferTracker[] theTrackers = new OfferTracker[] {
        new OfferTracker("Cached"), new OfferTracker("Dirty"),
        new OfferTracker("Storage")
    };

    SleeveCache(Storage aStore) throws IOException {
        theStore = aStore;

        try {
            theConstraints =
                EntryConstraints.getConstraints(theStore.getType());
        } catch (ConfigurationException aCE) {
            thePartitionsMask = 0;
            theNumPartitions = 0;
            theStoreCaches = new ArcCache[0];
            theLogger.log(Level.SEVERE,
                "Couldn't load constraints for type " +
                    theStore.getType(), aCE);
            IOException myIOE =
                new IOException("Couldn't load constraints for type " +
                    theStore.getType());
            myIOE.initCause(aCE);
            throw myIOE;
        }
        
        theCacheSize = (CacheSize) theConstraints.get(CacheSize.class);

        int myNumPartitions;
        int myEntriesPerCache;

        if (DESIRED_ENTRIES_PER_PARTITION == -1) {
            myNumPartitions = 1;
            myEntriesPerCache = theCacheSize.getSize();
        } else {
            myNumPartitions = (theCacheSize.getSize() / DESIRED_ENTRIES_PER_PARTITION);
            myEntriesPerCache = DESIRED_ENTRIES_PER_PARTITION;
        }

        // Find nearest power of 2 > or =
        //
        int myPower;
        for (myPower = 1; myPower < myNumPartitions; myPower = myPower << 1);
        theNumPartitions = myPower;

        thePartitionsMask = theNumPartitions - 1;
        
        theLogger.log(Level.INFO, aStore.getType() + " cache size = "
                      + theCacheSize.getSize() + " partitions = " + theNumPartitions +
                      " mask = " + Integer.toHexString(thePartitionsMask) + " partition size = " + myEntriesPerCache);

        theStoreCaches = new ArcCache[theNumPartitions];

        for (int i = 0; i < theNumPartitions; i++) {
            theStoreCaches[i] = new ArcCache(aStore, myEntriesPerCache);

            theIndexer = CacheIndexer.getIndexer(theStore.getType());

            theStoreCaches[i].add(new CacheListenerImpl(theIndexer));
        }

        try {
            theCounters = new CountersImpl(theStore.getType(),
                                           theStore.getNumEntries());
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE,
                "Couldn't read instance count from storage: " +
                    theStore.getType() +
                    " statistics will be inaccurate", anIOE);

            theCounters = new CountersImpl(theStore.getType(), 0);
        }

        StatsBoard.get().add(this);
    }

    public void setId(long anId) {
        theId = anId;
    }

    public long getId() {
        return theId;
    }

    public Stat generate() {
        String[] myTitles = new String[theTrackers.length];
        long[] myMisses = new long[theTrackers.length];
        long[] myDeld = new long[theTrackers.length];

        for (int i = 0; i < theTrackers.length; i++) {
            myTitles[i] = theTrackers[i].getTitle();
            myMisses[i] = theTrackers[i].getMisses();
            myDeld[i] = theTrackers[i].getDeld();
        }

        int myActiveCache = 0;

        for (int i = 0; i < theStoreCaches.length; i++)
            myActiveCache += theStoreCaches[i].getActiveSize();

        return new StoreStat(theId, theStore.getType(), myTitles,
            myMisses, myDeld, myActiveCache, theCacheSize.getSize());
    }

    private int getPartition(CacheBlockDescriptor aCBD) {
        OID myOID = (OID) aCBD.getId();

        return getPartition(myOID);
    }

    private int getPartition(OID anOID) {
        return anOID.hashCode() & thePartitionsMask;
    }

    private int getPartition(EntrySleeveImpl aSleeve) {
        return getPartition(aSleeve.getOID());
    }

    void forceSync(CacheBlockDescriptor aCBD) throws IOException {
        theStoreCaches[getPartition(aCBD)].forceSync(aCBD);
    }

    CacheBlockDescriptor load(OID aOID) throws IOException {
        return theStoreCaches[getPartition(aOID)].find(aOID);
    }

    CacheBlockDescriptor add(EntrySleeveImpl aSleeve) throws IOException {
        return theStoreCaches[getPartition(aSleeve)].insert(aSleeve);
    }

    RecoverySummary recover(EntrySleeveImpl aSleeve)
        throws IOException {
        return theStoreCaches[getPartition(aSleeve)].recover(aSleeve);
    }

    boolean renew(OID aOID, long anExpiry) throws IOException {
        CacheBlockDescriptor myCBD = theStoreCaches[getPartition(aOID)].find(aOID);

        if (myCBD == null) {
            return false;
        }

        EntrySleeveImpl mySleeve = (EntrySleeveImpl) myCBD.getContent();

        if (!mySleeve.hasExpired(System.currentTimeMillis()) &&
            !mySleeve.getState().test(SleeveState.DELETED)) {

            if (anExpiry == 0) {
                // Don't reset expiry - it's useful to storage to know
                // what the lease was last
                mySleeve.getState().set(SleeveState.DELETED);

                // Update stats
                theCounters.didPurge();
            } else {
                mySleeve.setExpiry(anExpiry);
            }

            mySleeve.markDirty();
            myCBD.release();

            return true;
        }

        myCBD.release();

        return false;
    }

    boolean cancel(OID aOID) throws IOException {
        return renew(aOID, 0);
    }

    void sync() throws IOException {
        for (int i = 0; i < theNumPartitions; i++) {
            theStoreCaches[i].sync();
        }
    }

    Counters getCounters() {
        return theCounters;
    }

    void close() {
        // Nothing to do here - we could destroy the counters but we put that
        // in deleteAll which makes more sense as close doesn't mean deletion
        // theStoreCache.dump();
    }

    void write(MangledEntry anEntry, long anExpiry, WriteEscort anEscort)
        throws IOException {


        OID myID = theStore.getNextId();
        EntrySleeveImpl mySleeve = null;

        if (theLogger.isLoggable(Level.FINE))
            theLogger.log(Level.FINE, "Written: " + myID + ", " +
                          anExpiry + ", " +
                          (anExpiry - System.currentTimeMillis()));

        mySleeve = new EntrySleeveImpl(myID, anEntry, anExpiry);

        // Ready to write but tell the escort first
        OpInfo myInfo = new WriteEntryOpInfo(mySleeve);

        if (!anEscort.writing(myInfo))
            return;

        // Now make it visible
        CacheBlockDescriptor myCBD = theStoreCaches[getPartition(mySleeve)].insert(mySleeve);
        myCBD.release();

        if (theLogger.isLoggable(Level.FINE))
            theLogger.log(Level.FINE, "Unwritten: " + mySleeve.getOID());
    }

    void find(MangledEntry anEntry, SearchVisitor aVisitor)
        throws IOException {

        if (theConstraints.get(Fifo.class) != null)
            fifoFind(anEntry, aVisitor);
        else
            fastFind(anEntry, aVisitor);
    }

    /**
       <p>If we're in FIFO mode, we know that each TupleLocator is sorted into
       FIFO order.  Thus we can merge across all locators on the fly
       and obtain a fully ordered, non-duplicate set of tuple id's for
       matching using a simply algorithm.</p>

       <p>By sorting across all locators and applying global FIFO order
       we can maximize use of the cache and avoid excessive disk hits if
       we have sufficient of the FIFO ordering in cache.  Thus we get
       graceful degradation rather than consulting disk for the definitive
       ordering regardless.</p>
     */
    private void fifoFind(MangledEntry anEntry, SearchVisitor aVisitor)
        throws IOException {

        long mySearchStart = System.currentTimeMillis();

        ArrayList myLocators = new ArrayList();

        TupleLocator myLocator = theStore.find(anEntry);

        if (myLocator != null)
            myLocators.add(myLocator);

        myLocator = theStore.findCached(anEntry);

        if (myLocator != null)
            myLocators.add(myLocator);

        myLocator = theIndexer.find(anEntry);

        if (myLocator != null)
            myLocators.add(myLocator);

        if (myLocators.size() == 0)
            return;

        TupleLocator[] mySortedSources = new TupleLocator[myLocators.size()];

        mySortedSources = (TupleLocator[]) myLocators.toArray(mySortedSources);

        myLocator = new SortingLocator(mySortedSources);

        offerAndReleaseLocator(myLocator, aVisitor, mySearchStart,
            theTrackers[CACHED_TRACKER]);
    }

    private void fastFind(MangledEntry anEntry, SearchVisitor aVisitor)
        throws IOException {

        long mySearchStart = System.currentTimeMillis();

        /*
          Basic approach is to send the template to the CacheIndexer and
          ask it to return suitable IDs which we will then pin and try.
          Note we should set a flag which boycotts a load from disk so that
          flushed or deleted entries are not loaded more than once.

          If we cannot satisfy the Visitor that way, we repeat the exercise
          with storage which returns OID/byte[] pairs which we can then
          pin.

          Storage and CacheIndexer are now free to plan searches as they
          see fit based on template.
         */

        TupleLocator myLocator = theIndexer.find(anEntry);

        if (theLogger.isLoggable(Level.FINE))
            theLogger.log(Level.FINE, "Searching[cache]: " + myLocator);

        if (myLocator != null) {
            if (offerAndReleaseLocator(myLocator, aVisitor, mySearchStart,
                theTrackers[CACHED_TRACKER])) {
                /*
                  System.err.println("Cache search time: " +
                                     (System.currentTimeMillis() -
                                      mySearchStart));
                */
                return;
            }
        }

        if (theLogger.isLoggable(Level.FINE))
            theLogger.log(Level.FINE, "Searching[StoreCache]: " + myLocator);

        myLocator = theStore.findCached(anEntry);

        if (myLocator != null) {
            if (offerAndReleaseLocator(myLocator, aVisitor, mySearchStart,
                theTrackers[DIRTY_TRACKER]))
                return;
        }

        myLocator = theStore.find(anEntry);

        if (myLocator == null) {
            if (theLogger.isLoggable(Level.FINE))
                theLogger.log(Level.FINE, "Got no matches on disk");
            return;
        }

        offerAndReleaseLocator(myLocator, aVisitor, mySearchStart,
            theTrackers[STORAGE_TRACKER]);
    }

    LongtermOffer getOffer(OID anOID) throws IOException {

        CacheBlockDescriptor myCBD = theStoreCaches[getPartition(anOID)].find(anOID);

        if (myCBD != null) {
            return new LongtermOfferImpl(myCBD);
        } else
            return null;
    }

    boolean find(SearchVisitor aVisitor, OID aOID, MangledEntry aPreload)
        throws IOException {

        CacheBlockDescriptor myCBD = theStoreCaches[getPartition(aOID)].find(aOID);

        long myStartTime = System.currentTimeMillis();

        boolean offered = false;

        if (myCBD != null) {
            try {
                EntrySleeveImpl mySleeve =
                    (EntrySleeveImpl) myCBD.getContent();

                /*
                  If the JS specification is changed to cope with the issues
                  discussed in http://archives.java.sun.com/cgi-bin/wa?A2=ind0311&L=javaspaces-users&F=&S=&P=4599 and http://archives.java.sun.com/cgi-bin/wa?A2=ind0311&L=javaspaces-users&F=&S=&P=3590 then we need to do two things:

                  (1) Allow the SearchVisitor to see Sleeve's even if they've
                  expired.
                  (2) Having "shown" it to the SearchVisitor we'd need to
                  query the ReapFilters and if they don't boycott, mark the
                  item deleted.

                  These two steps have the effect of allowing a *ifExists to
                  conflict on lease-expired entries that have been locked
                  by a transaction and ensures we only delete such entries
                  when no transactions have posession of them anymore.  Of
                  course, this is somewhat slower and less efficient as
                  there's never a circumstance under which we can be assured
                  that a SearchVisitor *never* sees a particular entry again.

                  If we must implement the strategy of flunking a transaction
                  owing to a lock on a lease expired object, this would be
                  best dealt with by having TxnOp's check expiries at prepare
                  or commit time.  However this is much less appealing as what
                  is basically a pessimistic transaction API becomes
                  optimistic in this case and only in this case.

                  All this applies to the similar statement in offer() below.
                 */
                if (! mySleeve.getState().test(SleeveState.DELETED)) {

                    // If it's expired, mark it deleted, subject to filters
                    //
                    if (mySleeve.hasExpired(myStartTime)) {

                        if (! EntryRepositoryFactory.getReaper().filter(mySleeve)) {
                            mySleeve.getState().set(SleeveState.DELETED);
                            mySleeve.markDirty();

                            // Update stats
                            theCounters.didPurge();
                        }
                    } else {
                        OpInfo myInfo =
                            new FindEntryOpInfo(theStore.getType(),
                                                mySleeve.getOID(),
                                                aVisitor.isDeleter());

                        SearchOffer myOffer;

                        if (aPreload != null) {
                            myOffer = new SearchOfferImpl(aPreload, myInfo);
                        } else {
                            theLogger.log(Level.FINE, "NOT using preload");

                            myOffer = new SearchOfferImpl(mySleeve.getEntry(),
                                                          myInfo);
                        }

                        aVisitor.offer(myOffer);

                        offered = true;
                    }
                } else {
                    theTrackers[CACHED_TRACKER].incDeld();
                }
            } finally {
                myCBD.release();
            }
        } else {
            theTrackers[CACHED_TRACKER].incMissed();
        }

        return offered;
    }

    private boolean offerAndReleaseLocator(TupleLocator aLocator,
                                           SearchVisitor aVisitor,
                                           long aStartTime,
                                           OfferTracker aTracker)
        throws IOException {

        try {
            return offer(aLocator, aVisitor, aStartTime, aTracker);
        } finally {
            aLocator.release();
        }
    }

    /**
       @return <code>true</code> means offering can stop for better or worse.
       <code>false</code> indicates that the search should continue if there
       are other sources of offers.
     */
    private boolean offer(TupleLocator aLocator, SearchVisitor aVisitor,
                          long aStartTime, OfferTracker aTracker)
        throws IOException {

        int myVisitorResponse = SearchVisitor.TRY_AGAIN;

        OID myId = null;
        EntrySleeveImpl mySleeve = null;
        String myType = theStore.getType();
        boolean isDeletion = aVisitor.isDeleter();

        // long myStart = System.currentTimeMillis();

        while (aLocator.fetchNext()) {

            myId = aLocator.getOID();

            CacheBlockDescriptor myCBD = theStoreCaches[getPartition(myId)].find(myId);

            if (myCBD != null) {
                mySleeve = (EntrySleeveImpl) myCBD.getContent();

                if (! mySleeve.getState().test(SleeveState.DELETED)) {

                    // If it's expired, mark it deleted, subject to filters
                    //
                    if (mySleeve.hasExpired(aStartTime)) {
                        if (! EntryRepositoryFactory.getReaper().filter(mySleeve)) {
                            mySleeve.getState().set(SleeveState.DELETED);
                            mySleeve.markDirty();

                            // Update stats
                            theCounters.didPurge();
                        }
                    } else {

                        OpInfo myInfo =
                            new FindEntryOpInfo(myType, mySleeve.getOID(),
                                isDeletion);

                        SearchOfferImpl myOffer =
                            new SearchOfferImpl(mySleeve.getEntry(), myInfo);

                        myVisitorResponse =
                            aVisitor.offer(myOffer);
                    }
                } else {
                    aTracker.incDeld();;
                }

                myCBD.release();

                if ((myVisitorResponse == SearchVisitor.STOP) ||
                    (myVisitorResponse == SearchVisitor.ACCEPTED)) {
                    break;
                }
            } else {
                aTracker.incMissed();
            }
        }

        /*
          System.out.println("Time to offer: " +
                             (System.currentTimeMillis() -
                              myStart));
        */

        if (myVisitorResponse != SearchVisitor.TRY_AGAIN)
            return true;
        else
            return false;
    }

    void deleteAll() throws IOException {
        theCounters.destroy();

        /*
          Basic approach is to send the template to the CacheIndexer and
          ask it to return suitable IDs which we will then pin and try.
          Note we should set a flag which boycotts a load from disk so that
          flushed or deleted entries are not loaded more than once.

          If we cannot satisfy the Visitor that way, we repeat the exercise
          with storage which returns OID/byte[] pairs which we can then
          pin.

          Storage and CacheIndexer are now free to plan searches as they
          see fit based on template.
         */

        TupleLocator myLocator = theIndexer.find(MangledEntry.NULL_TEMPLATE);

        if (theLogger.isLoggable(Level.FINE))
            theLogger.log(Level.FINE, "Searching[cache]: " + myLocator);

        if (myLocator != null) {
            delete(myLocator);
        }

        if (theLogger.isLoggable(Level.FINE))
            theLogger.log(Level.FINE, "Searching[StoreCache]: " + myLocator);

        myLocator = theStore.findCached(MangledEntry.NULL_TEMPLATE);

        if (myLocator != null) {
            delete(myLocator);
        }

        myLocator = theStore.find(MangledEntry.NULL_TEMPLATE);

        if (myLocator == null) {
            if (theLogger.isLoggable(Level.FINE))
                theLogger.log(Level.FINE, "Got no matches on disk");
            return;
        }

        delete(myLocator);
    }

    private void delete(TupleLocator aLocator) throws IOException {
        try {
            while (aLocator.fetchNext()) {

                OID myId = aLocator.getOID();

                CacheBlockDescriptor myCBD = theStoreCaches[getPartition(myId)].find(myId);

                OpInfo myInfo = null;

                if (myCBD != null) {
                    EntrySleeveImpl mySleeve =
                        (EntrySleeveImpl) myCBD.getContent();

                    if (! mySleeve.getState().test(SleeveState.DELETED)) {
                        mySleeve.getState().set(SleeveState.DELETED);
                        mySleeve.markDirty();

                        myInfo =
                            new FindEntryOpInfo(theStore.getType(),
                                                mySleeve.getOID(),
                                                true);
                    }

                    myCBD.release();

                    if (myInfo != null) {
                        try {
                            TxnManager.get().log(new ForcedCommit(myInfo));
                        } catch (TransactionException aTE) {
                            IOException myIOE = new IOException("Eeek failed to delete Entry");
                            myIOE.initCause(aTE);
                            throw myIOE;
                        }
                    }
                }
            }
        } finally {
            aLocator.release();
        }
    }

    public String toString() {
        return "SC: " + theStore.getType();
    }
}
