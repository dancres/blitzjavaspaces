package org.dancres.blitz.entry;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.*;

import java.util.ArrayList;
import org.dancres.blitz.entry.ci.CacheIndexer;

import org.dancres.blitz.mangler.MangledField;
import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.util.BytePacker;

import org.dancres.blitz.Logging;
import org.dancres.blitz.config.EntryConstraints;

import org.dancres.util.ObjectTransformer;

import org.dancres.blitz.meta.RegistryFactory;
import org.dancres.blitz.meta.Registry;

import org.dancres.blitz.disk.DiskTxn;
import org.dancres.blitz.disk.Disk;

import org.dancres.blitz.oid.Allocator;
import org.dancres.blitz.oid.AllocatorFactory;
import org.dancres.blitz.oid.OID;

import org.dancres.blitz.arc.BackingStore;

import org.dancres.blitz.cache.Identifiable;
import org.dancres.blitz.cache.Identifier;
import org.dancres.blitz.cache.CacheListener;

import org.dancres.blitz.lease.ReapFilter;

import org.dancres.blitz.stats.TypesStat;
import org.dancres.blitz.stats.StatsBoard;

/**
   <p>Handles all responsibilities associated with the root Entry type
   "java.lang.Object".  This is an internal implementation detail.  We
   maintain a repository of java.lang.Object for the purposes of tracking
   subtype information which is used when the JavaSpaces programmer invokes a
   <code>take</code> or <code>read</code> on the proxy with a
   <code>null</code> template.  The <code>null</argument>, of course, means
   find an entry of any type.</p>

   Storage of concrete instances of Entry are handled by EntryStorage.

   @see org.dancres.blitz.entry.EntryStorage
 */
class RootStorage implements Storage {
    private String theType;

    /**
       Required because we still store type information
     */
    private Registry theMetaData;

    private ArrayList theSubtypes = new ArrayList();
    private Set<String> theCurrentSubtypes = new HashSet<String>();

    private boolean noSchemaDefined = false;

    private TypesStat theTypesStat;

    private EntryConstraints theConstraints = EntryConstraints.MINIMUM;

    RootStorage() {
        theType = EntryRepository.ROOT_TYPE;
        CacheIndexer.newIndexer(theType, theConstraints);
    }

    public int getNumEntries() throws IOException {
        return 0;
    }

    public EntryConstraints getConstraints() {
        return theConstraints;
    }

    void add(CacheListener aListener) {
    }

    public String getType() {
        return theType;
    }

    public String getName() {
        return theType;
    }

    public TupleLocator findCached(MangledEntry anEntry) {
        return null;
    }

    /**
       @return <code>true</code> if initialization succeeded in accordance
       with the mustExist flag.  i.e. If mustExist is true and the databases
       couldn't be opened one will receive <code>false</code> NOT an
       exception.
     */
    public boolean init(boolean mustExist) throws IOException {
        if (mustExist) {
            if (! RegistryFactory.exists(theType))
                return false;
        }

        try {
            theMetaData = RegistryFactory.get(theType, null);

            DiskTxn myTxn = DiskTxn.newStandalone();

            try {
                byte[] mySubtypeInfo =
                    theMetaData.getAccessor(myTxn).loadRaw(FixedOIDs.SUBTYPES_KEY);
                    
                if (mySubtypeInfo != null) {
                    theSubtypes =(ArrayList)
                        ObjectTransformer.toObject(mySubtypeInfo);
                    updateCurrentTypes();
                }
            } finally {
                myTxn.commit();
            }

        } catch (FileNotFoundException aFNFE) {
            EntryStorage.theLogger.log(Level.SEVERE,
                                       "Couldn't open type db",  aFNFE);
            throw new IOException("Couldn't open type db");
        }

        return true;
    }

    /**
       Tells this Repository about a subtype which has just been created
       and would need to be search if this type were the specified template.
     */
    public synchronized void addSubtype(String aType) throws IOException {
        if (! theSubtypes.contains(aType)) {
            theSubtypes.add(aType);
            updateCurrentTypes();
            theMetaData.getAccessor().save(FixedOIDs.SUBTYPES_KEY,
                                           theSubtypes);
        }
    }

    public synchronized Set<String> getSubtypes() {
        return theCurrentSubtypes;
    }

    /**
       Called to setup schema information, create indexes etc - should only
       be called once in response to <code>true</code> being returned from
       <code>didntExist()</code>
     */
    public synchronized void setFields(MangledField[] aSetOfFields) 
        throws IOException {
        throw new RuntimeException("Shouldn't ever be called - noSchemaDefined is always false!");
    }

    /**
       Indicates if we were created for the first time as the result of
       a call to <code>EntryRepositoryFactory.get()</code>.  Note that this
       flag is reset after <code>setFields()</code> is called.  Thus, even
       if an EntryRepository has been informed of children, it will still
       return <code>true</code> until <code>setFields</code> is called.
     */
    public boolean noSchemaDefined() {
        return noSchemaDefined;
    }

    public void close() throws IOException {
        theMetaData.close();
    }

    public OID getNextId() throws IOException {
        throw new IOException("Shouldn't be called");
    }

    public void delete() throws IOException {
        RegistryFactory.delete(theType);
    }

    public void bringOutTheDead(EntryReaper aReaper) throws IOException {
        return;
    }

    private synchronized void updateCurrentTypes() {
        if (theTypesStat == null) {
            theTypesStat = new TypesStat();
            StatsBoard.get().add(theTypesStat);
        }

        theCurrentSubtypes = new HashSet<String>(theSubtypes);

        String[] myTypes = new String[theSubtypes.size()];
        theTypesStat.setTypes((String[]) theSubtypes.toArray(myTypes));
    }

    private KeyIndex newIndex(String aName, int anOffset) throws IOException {
        throw new IOException("Shouldn't be called");
    }

    private void loadIndexes(byte[] aSetOfIndexes)
        throws IOException {
    }

    /* *******************************************************************
     * BackingStore impl
     * *******************************************************************/

    public void save(Identifiable anIdentifiable) throws IOException {
        throw new IOException("Shouldn't be called");
    }

    public Identifiable load(Identifier anId) throws IOException {
        throw new IOException("Shouldn't be called");
    }

    /* *********************************************************************
       Search code starts here
    *********************************************************************/

    /**
       Return a set of potential matches based on the passed template.  Note
       one must test each returned tuple for an exact match across ALL keys
       because this method is "speculative".  It will return likely matches
       it does not guarentee to have located an exact subset based on the
       passed template.  As a full match is necessary to avoid hash-collisions
       this shouldn't be a problem.

       @param anEntry a template to use to locate matches.

       @return TupleLocator instance of <code>null</code> if there are no
       possible matches.
     */
    public TupleLocator find(MangledEntry anEntry) throws IOException {
        return null;
    }
}
