package org.dancres.blitz.entry;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.*;

import java.util.ArrayList;

import com.sleepycat.je.*;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.mangler.MangledField;
import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.util.BytePacker;

import org.dancres.blitz.Logging;

import org.dancres.util.ObjectTransformer;

import org.dancres.blitz.meta.RegistryFactory;
import org.dancres.blitz.meta.Registry;

import org.dancres.blitz.entry.ci.CacheIndexer;

import org.dancres.blitz.disk.DiskTxn;
import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.RetryingUpdate;
import org.dancres.blitz.disk.RetryableOperation;

import org.dancres.blitz.oid.Allocator;
import org.dancres.blitz.oid.AllocatorFactory;
import org.dancres.blitz.oid.OID;
import org.dancres.blitz.oid.OIDFactory;

import org.dancres.blitz.cache.Identifiable;
import org.dancres.blitz.cache.Identifier;

import org.dancres.blitz.config.Fifo;
import org.dancres.blitz.config.ReadAhead;
import org.dancres.blitz.config.EntryConstraints;

import org.dancres.blitz.stats.StatsBoard;
import org.dancres.blitz.stats.FieldsStat;

/**
   <p>Used to maintain all Entry instances of a particular type but does not
   include "java.lang.Object".  "java.lang.Object" is considered the root
   node for internal implementation purposes.  Storage requirements associated
   with this Entry type are handled by RootStorage.</p>

   <p>MetaDB contains a list of known sub-types, a set of KeyIndex instances.
   </p>

   @see org.dancres.blitz.entry.RootStorage
 */
class EntryStorage implements Storage, EntryEditor {
    /*
      Used in searching to indicate various results
     */

    /**
       Indicates a particular search field was null and therefore wildcard
       and thus doesn't filter matches
     */
    private static final int WAS_NULL = -1;

    /**
       Indicates a field was not wildcard and thus filters matches but
       when queries produced no hits
     */
    private static final int NO_HITS = -2;

    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.disk.Storage");

    private String theType;

    /**
       Duplicate keys not allowed in the main database
     */
    private Database theMainDb;

    private Registry theMetaData;

    private ArrayList theSubtypes = new ArrayList();
    private Set<String> theCurrentSubtypes = new HashSet<String>();

    private KeyIndex[] theIndexes;

    private boolean noSchemaDefined = false;

    private Allocator theAllocator;
    
    private WriteScheduler theWriteScheduler;

    private CacheIndexer theIndexer;

    private LeaseTracker theTracker;

    private EntryConstraints theConstraints;

    EntryStorage(String aType) {
        theType = aType;
    }

    public String getType() {
        return theType;
    }

    public String getName() {
        return theType;
    }

    /**
       Used for debug purposes
     */
    public String toString() {
        return theType;
    }

    public TupleLocator findCached(MangledEntry anEntry) {
        return null;
        
        // return theIndexer.find(anEntry);
    }

    public boolean init(boolean mustExist) throws IOException {
        if (mustExist) {
            if (! Disk.dbExists(theType)) {
                return false;
            }
        }

        try {
            theConstraints = EntryConstraints.getConstraints(theType);
        } catch (ConfigurationException aCE) {
            IOException myIOE = new IOException("Failed to load constraints");
            myIOE.initCause(aCE);
            throw myIOE;
        }


        theWriteScheduler = new WriteScheduler((EntryEditor) this);

        theIndexer =
            CacheIndexer.newIndexer(theType, theConstraints);

        try {
            theMetaData = RegistryFactory.get(theType, null);

            DatabaseConfig myConfig = new DatabaseConfig();
            myConfig.setAllowCreate(true);

            theMainDb = Disk.newDb(null, theType, myConfig);

            /*
              If there are no index elements, we've not been created
              explicitly previously.  We may have been implicitly created
              whilst modelling inheritance hierarchy but that doesn't count
            */
            DiskTxn myTxn = DiskTxn.newStandalone();

            try {
                byte[] myIndexInfo =
                    theMetaData.getAccessor(myTxn).loadRaw(FixedOIDs.INDEXES_KEY);

                if (myIndexInfo == null) {
                    noSchemaDefined = true;
                } else {
                    loadIndexes(myIndexInfo);
                }
            } finally {
                myTxn.commit();
            }

            myTxn = DiskTxn.newStandalone();

            try {
                byte[] mySubtypeInfo =
                    theMetaData.getAccessor(myTxn).loadRaw(FixedOIDs.SUBTYPES_KEY);
                    
                if (mySubtypeInfo != null) {
                    theSubtypes =(ArrayList)
                        ObjectTransformer.toObject(mySubtypeInfo);
                    theCurrentSubtypes = new HashSet<String>(theSubtypes);
                }
            } finally {
                myTxn.commit();
            }

        } catch (FileNotFoundException aFNFE) {
            theLogger.log(Level.SEVERE, "Couldn't open type db",  aFNFE);
            throw new IOException("Couldn't open type db");
        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
            throw new IOException("Dbe");
        }

        // Initialize Allocator
        //
        if (theConstraints.get(Fifo.class) != null)
            theAllocator = AllocatorFactory.get(theType, true);
        else
            theAllocator = AllocatorFactory.get(theType, false);

        // Initialize LeaseTracker
        theTracker =
            LeaseTrackerFactory.getTracker(theType, theAllocator);

        return true;
    }

    public int getNumEntries() throws IOException {
        if (theMainDb == null)
            return 0;

        try {
            StatsConfig myConfig = new StatsConfig();
            myConfig.setFast(false);
            
            BtreeStats myStats = (BtreeStats) theMainDb.getStats(myConfig);
            return (int) myStats.getLeafNodeCount();
        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Couldn't read num entries from Db",
                          aDbe);
            throw new IOException("Dbe");
        }
    }

    public synchronized void addSubtype(String aType) throws IOException {
        if (! theSubtypes.contains(aType)) {
            theSubtypes.add(aType);
            theCurrentSubtypes = new HashSet<String>(theSubtypes);

            theMetaData.getAccessor().save(FixedOIDs.SUBTYPES_KEY,
                                           theSubtypes);
        }
    }

    public synchronized Set<String> getSubtypes() {
        return theCurrentSubtypes;
    }

    public synchronized void setFields(MangledField[] aSetOfFields) 
        throws IOException {

        /*
          It's possible that a couple of threads may have seen the undefined
          schema state - in this case, they'll all rush to sort out the
          schema - in these cases, we ignore those duplicate requests.
         */
        if (noSchemaDefined == false)
            return;

        ArrayList myFields = new ArrayList();

        theIndexes = new KeyIndex[aSetOfFields.length];

        // Fields are always in the same order
        for (int i = 0; i < aSetOfFields.length; i++) {
            myFields.add(aSetOfFields[i].getName());
            KeyIndex myIndex = newIndex(aSetOfFields[i].getName(), i);
            theIndexes[i] = myIndex;
        }

        theMetaData.getAccessor().save(FixedOIDs.INDEXES_KEY, theIndexes);

        noSchemaDefined = false;

        StatsBoard.get().add(new FieldsStat(theType, myFields));
    }

    public boolean noSchemaDefined() {
        return noSchemaDefined;
    }

    public void close() throws IOException {
        try {

            theTracker.close();

            if (theIndexes != null) {
                for (int i = 0; i < theIndexes.length; i++) {
                    theIndexes[i].close();
                }
            }

            theMainDb.close();

        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Couldn't close maindb", aDbe);
            throw new IOException("Dbe");
        }

        theMetaData.close();
    }

    public void delete() throws IOException {
        theTracker.delete();

        RegistryFactory.delete(theType);

        AllocatorFactory.delete(theType);

        if (theIndexes != null) {
            for (int i = 0; i < theIndexes.length; i++) {
                theIndexes[i].delete();
            }
        }

        Disk.deleteDb(theType);
    }

    public OID getNextId() throws IOException {
        return theAllocator.getNextId();
    }

    public void bringOutTheDead(EntryReaper aReaper) throws IOException {
        theTracker.bringOutTheDead(aReaper);
    }

    private KeyIndex newIndex(String aName, int anOffset) throws IOException {
        KeyIndex myIndex = new KeyIndex(theType, aName, anOffset);
        myIndex.init();

        return myIndex;
    }

    private void loadIndexes(byte[] aSetOfIndexes)
        throws IOException {

        ArrayList myFields = new ArrayList();

        theIndexes = (KeyIndex[]) ObjectTransformer.toObject(aSetOfIndexes);

        for (int i = 0; i < theIndexes.length; i++) {
            myFields.add(theIndexes[i].getName());
            theIndexes[i].init();
        }

        StatsBoard.get().add(new FieldsStat(theType, myFields));
    }

    /* *******************************************************************
     * BackingStore impl
     * *******************************************************************/

    public void save(Identifiable anIdentifiable) throws IOException {
        // Not done here, delegated to the WriteScheduler for asynch write
        //
        theWriteScheduler.add((EntrySleeveImpl) anIdentifiable);
    }

    public Identifiable load(Identifier anId) throws IOException {
        // Check there isn't a dirty copy in write cache before hitting disk
        Identifiable myIdentifiable =
            theWriteScheduler.dirtyRead((OID) anId);

        if (myIdentifiable == null) {
            byte[] myPackage = load((OID) anId);
            
            if (myPackage != null) {
                // System.out.println("Got from disk: " + anId);
                return new EntrySleeveImpl(myPackage);
            } else return null;
        } else {
            // System.out.println("Got from dirty: " + anId);
            return myIdentifiable;
        }
    }

    private byte[] load(final OID anId) throws IOException {
        if (theLogger.isLoggable(Level.FINEST))
            theLogger.log(Level.FINEST, "Sload: " + anId);

        // System.err.println("Sload: " + anId);

        final DatabaseEntry myKey =
            new DatabaseEntry(OIDFactory.getKey(anId));

        RetryableOperation myOp =
            new RetryableOperation() {
                public Object perform(DiskTxn aTxn) throws DatabaseException {
                    DatabaseEntry myData = new DatabaseEntry();

                    OperationStatus myStatus;

                    myStatus = theMainDb.get(aTxn.getDbTxn(), myKey, myData,
                                             null);

                    if (! myStatus.equals(OperationStatus.NOTFOUND)) {
                        return myData.getData();
                    }

                    return null;
                }

                public String toString() {
                    return "Load: " + anId;
                }
            };

        Object myResult = new RetryingUpdate(myOp).commit();

        return (myResult == null) ? null : (byte[]) myResult;
    }

    public void delete(PersistentEntry anEntry) throws IOException {
        final OID myId = anEntry.getOID();
        MangledField[] myKeys = anEntry.getEntry().getFields();

        if (theLogger.isLoggable(Level.FINEST))
            theLogger.log(Level.FINEST, "Sdelete: " + myId);

        // System.err.println("Sdelete: " + myId);

        theTracker.delete(anEntry);

        // Delete from indexes in reverse
        for (int i = (theIndexes.length - 1); i >= 0; i--) {
            theIndexes[i].unIndex(myId, myKeys);
        }

        RetryableOperation myOp =
            new RetryableOperation() {
                public Object perform(DiskTxn aTxn) throws DatabaseException {
                    OperationStatus myStatus =
                        theMainDb.delete(aTxn.getDbTxn(),
                                         new DatabaseEntry(OIDFactory.getKey(myId)));

                    if (myStatus.equals(OperationStatus.NOTFOUND))
                        theLogger.log(Level.SEVERE,
                                      "Warning failed to delete key from maindb");

                    return null;
                }

                public String toString() {
                    return "Delete: " + myId;
                }
            };

        new RetryingUpdate(myOp).commit();
    }

    public void update(PersistentEntry anEntry) throws IOException {
        final OID myId = anEntry.getOID();
        final byte[] myPackage = anEntry.flatten();

        if (theLogger.isLoggable(Level.FINEST))
            theLogger.log(Level.FINEST, "Supdate: " + myId);

        // System.err.println("Supdate: " + myId);

        theTracker.update(anEntry);

        RetryableOperation myOp =
            new RetryableOperation() {
                public Object perform(DiskTxn aTxn) throws DatabaseException {
                    theMainDb.put(aTxn.getDbTxn(),
                                  new DatabaseEntry(OIDFactory.getKey(myId)),
                                  new DatabaseEntry(myPackage));

                    return null;
                }

                public String toString() {
                    return "Update: " + myId;
                }
            };

        new RetryingUpdate(myOp).commit();
    }

    public void write(PersistentEntry anEntry) throws IOException {
        final OID myId = anEntry.getOID();
        final MangledField[] myKeys = anEntry.getEntry().getFields();
        final byte[] myPackage = anEntry.flatten();

        if (theLogger.isLoggable(Level.FINEST))
            theLogger.log(Level.FINEST, "Swrite: " + myId);

        // System.err.println("Swrite: " + myId);

        theTracker.write(anEntry);

        RetryableOperation myOp =
            new RetryableOperation() {
                public Object perform(DiskTxn aTxn) throws DatabaseException {

                    /*
                      This must be an all or nothing operation - we cannot have
                      some indexes with an inserted entry and others without or
                      a missing main db entry.
                    */        

                    // Index in reverse
                    for (int i = (theIndexes.length - 1); i >= 0; i--) {
                        theIndexes[i].index(myId, myKeys, aTxn);
                    }

                    theMainDb.put(aTxn.getDbTxn(),
                                  new DatabaseEntry(OIDFactory.getKey(myId)),
                                  new DatabaseEntry(myPackage));

                    return null;
                }

                public String toString() {
                    return "Write: " + myId;
                }
            };

        new RetryingUpdate(myOp).commit();
    }

    /* *********************************************************************
       Search code starts here
    *********************************************************************/

    public TupleLocator find(MangledEntry anEntry) throws IOException {
        if (noSchemaDefined())
            return null;

        theLogger.log(Level.FINEST, "find");

        /*
          For a full maindb search we pass empty key and data with the
          DBNEXT flag.

          For a secondary index search we pass a specified key and empty data
          with DBSET.
        */
        try {
            if ((anEntry == null) || (anEntry.isWildcard())) {
                theLogger.log(Level.FINEST, "wildcard");

                Cursor myCursor =
                    theMainDb.openCursor(null, null);

                OperationStatus myStatus =
                    myCursor.getNext(new DatabaseEntry(),
                                     new DatabaseEntry(), null);

                if (myStatus.equals(OperationStatus.NOTFOUND)) {
                    myCursor.close();
                    return null;
                } else {
                    ReadAhead myRead =
                            (ReadAhead) theConstraints.get(ReadAhead.class);

                    return new PrimaryLocatorImpl(myCursor, anEntry,
                        myRead.getSize());
                }
            } else {
                theLogger.log(Level.FINEST, "index");
            
                // Locate the smallest search set
                Cursor myCursor = null;
                MangledField[] myFields = anEntry.getFields();
                byte[] myPackedKey = new byte[4];
                BytePacker myPacker = BytePacker.getMSBPacker(myPackedKey);
            
                int[] mySizes = new int[myFields.length];

                if ((anEntry.getType().equals(theType)) &&
                    (myFields.length != theIndexes.length))
                    theLogger.log(Level.WARNING, "Possible schema change detected - matching may fail" + theType);

                // Do the analysis in reverse
                for (int i = (myFields.length - 1); i >= 0; i--) {
                    MangledField myField = myFields[i];
                
                    if (myField.isNull())
                        mySizes[i] = WAS_NULL;
                    else {
                        theLogger.log(Level.FINEST, "scanning: " +
                                      myField.getName());
                    
                        int myHashCode = myField.hashCode();
                        myPacker.putInt(myHashCode, 0);
                    
                        DiskTxn myStandalone = DiskTxn.newStandalone();

                        myCursor = theIndexes[i].newCursor(null);

                        OperationStatus myStatus =
                            myCursor.getSearchKey(new DatabaseEntry(myPackedKey), new DatabaseEntry(), null);

                        // If we get results back we can do a compare
                        if (! myStatus.equals(OperationStatus.NOTFOUND)) {
                        
                            theLogger.log(Level.FINEST, "Got: " +
                                          myCursor.count() + " entries");
                            
                            mySizes[i] = myCursor.count();
                        } else {
                            theLogger.log(Level.FINEST, "Got 0 entries");
                            mySizes[i] = NO_HITS;
                        }
                    
                        myCursor.close();
                        myStandalone.commit();
                    }
                }
            
                int mySmallestSize = Integer.MAX_VALUE;
                int myChoice = -1;
            
                for (int i = 0; i < mySizes.length; i++) {
                    /*
                      If the field contributed no filtering because it was
                      wildcard, ignore it.  Note that _at least one_ of the
                      fields must be non-null because we aren't wildcard
                      matching (caught and handled above)
                     */
                    if (mySizes[i] != WAS_NULL) {
                        /*
                          If any contributing index returned no hits, we know
                          there is going to be no match so we can stop right
                          now
                         */
                        if (mySizes[i] == NO_HITS) {
                            theLogger.log(Level.FINEST, "Aborting search, one field didn't match");
                            return null;
                        } else if (mySizes[i] < mySmallestSize) {
                            /*
                              The index under consideration produced fewer
                              matches than our previous choice
                             */
                            myChoice = i;
                            mySmallestSize = mySizes[i];
                        }
                    }
                }
            
                /*
                  If an Entry has all null fields, it's a wildcard which is
                  handled above.  If the Entry is not wildcard and any one
                  of the indexes yielded no hits, we will have already
                  exited above.

                  Thus we are left with whichever index yielded the lowest
                  number of hits.  So we now load up that index and return
                  it.
                 */
                theLogger.log(Level.FINEST, "Searching: " +
                                   mySmallestSize + " from " +
                                   theIndexes[myChoice].getName());

                myPacker.putInt(myFields[myChoice].hashCode(), 0);

                /*
                  Now we've decided on the cursor we wish to use, we can
                  open it in the main txn.
                */
                myCursor = theIndexes[myChoice].newCursor(null);

                DatabaseEntry myHashKey = new DatabaseEntry(myPackedKey);

                OperationStatus myStatus = 
                    myCursor.getSearchKey(myHashKey, new DatabaseEntry(),
                                          null);

                if (! myStatus.equals(OperationStatus.NOTFOUND)) {
                        
                    theLogger.log(Level.FINEST, "Got: " +
                                       myCursor.count() + " entries");
                        
                    ReadAhead myRead =
                            (ReadAhead) theConstraints.get(ReadAhead.class);

                    return new IndexLocatorImpl(myCursor, myHashKey,
                            anEntry, myRead.getSize());
                } else {
                    myCursor.close();
                    return null;
                }
            }
        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
            throw new IOException("Dbe");
        }
    }
}
