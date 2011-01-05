package org.dancres.blitz.entry;

import java.io.IOException;

import java.util.Set;
import java.util.logging.*;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.mangler.MangledField;
import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.Logging;
import org.dancres.blitz.config.EntryConstraints;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.arc.CacheBlockDescriptor;
import org.dancres.blitz.arc.RecoverySummary;

import org.dancres.blitz.lease.Reapable;
import org.dancres.blitz.lease.ReapFilter;

import org.dancres.blitz.txn.TxnManager;

class EntryReposImpl implements EntryReposRecovery, Reapable {
    private static Logger theLogger =
        Logging.newLogger("org.dancres.disk.EntryRepository");

    private Storage theStore;
    private SleeveCache theSleeveCache;

    private boolean haveLoggedCount = false;

    private EntryConstraints theConstraints;

    EntryReposImpl(Storage aStore) throws IOException {
        try {
            theConstraints = EntryConstraints.getConstraints(aStore.getType());
        } catch (ConfigurationException aCE) {
            IOException myIOE = new IOException("Couldn't load constraints");
            myIOE.initCause(aCE);
            throw myIOE;
        }

        theStore = aStore;
        theSleeveCache = new SleeveCache(theStore);

        // Don't configure for reaping if it's configured off
        if (EntryRepositoryFactory.getReaper().isActive())
            EntryRepositoryFactory.getReaper().add(this);
    }

    /**
       This method is designed to log a record of the number of instances of
       this repository's type the first time the repository is updated.

       It's expected that checkpoints will store instance counts for all
       repositories loaded at that time.  This code ensures that any
       repositories that come into being between checkpoints also have an
       appropriate record to which following logged actions can be applied.

       We only need to log the record once because at the next checkpoint
       it's counts will be in the checkpoint record.  This step is the
       responsibility of <code>EntryRepositoryFactory</code>
     */
    private void logInstanceBarrier() {
        if (TxnManager.get().isRecovery()) {
            return;
        }

        if (EntryRepositoryFactory.get().isDebugLogging()) {
            synchronized(this) {
                if (haveLoggedCount)
                    return;

                try {
                    /*
                      We must use the live stats for this count as the
                      storage's count may not be up-to-date due to actions
                      performed during recovery still sitting in cache.
                      I _think_ the fact that we checkpoint should prevent
                      this from actually happening but better safe than sorry.

                      We only attempt to log this - we can fail due to being
                      blocked on the transaction log lock for too long.
                      If that has happened, chances are we've done a checkpoint
                      and a record will have been emitted anyways.
                    */
                    TxnManager.get().tryLog(new CountAction(theStore.getType(),
                                                            theSleeveCache.getCounters().getInstanceCount()), 100);
                } catch (Exception anE) {
                    theLogger.log(Level.SEVERE, "Failed to log instance count",
                                  anE);
                }

                haveLoggedCount = true;
            }
        }
    }

    public String getType() {
        return theStore.getName();
    }

    public int getTotalStoredEntries() throws IOException {
        return theStore.getNumEntries();
    }

    public int getTotalLiveEntries() {
        return theSleeveCache.getCounters().getInstanceCount();
    }

    public void reap(ReapFilter aFilter) {
        logInstanceBarrier();

        try {
            theStore.bringOutTheDead(new CleanerImpl(theSleeveCache, aFilter));
        } catch (IOException anIOE) {
            theLogger.log(Level.SEVERE, "Couldn't bring out the dead",
                          anIOE);
        }
    }

    public boolean renew(OID aOID, long anExpiry) throws IOException {
        logInstanceBarrier();

        return theSleeveCache.renew(aOID, anExpiry);
    }

    public boolean cancel(OID aOID) throws IOException {
        logInstanceBarrier();

        return theSleeveCache.cancel(aOID);
    }

    /*
      These methods are only used during commit/abort and thus there will
      have been activity previously generating a log entry for instance counts.

      If there hasn't been activity, this operation is coming from the log
      file which means there's been activity in the previous run and so
      either we have an instance count in the checkpoint record or we'll have
      logged one ourselves as part of activity prior to logging the transaction
      which has generated these operations.
     */
    public CacheBlockDescriptor load(OID aOID) throws IOException {
        return theSleeveCache.load(aOID);
    }

    public void flush(CacheBlockDescriptor aCBD) throws IOException {
        theSleeveCache.forceSync(aCBD);
    }

    CacheBlockDescriptor add(EntrySleeveImpl aSleeve) throws IOException {
        return theSleeveCache.add(aSleeve);
    }

    public RecoverySummary recover(EntrySleeveImpl aSleeve)
        throws IOException {
        return theSleeveCache.recover(aSleeve);
    }

    public Counters getCounters() {
        return theSleeveCache.getCounters();
    }

    /*
      These methods indicate new activity and we want to make sure we have
      an up-to-date instance count
     */
    public void write(MangledEntry anEntry, long anExpiry,
                      WriteEscort anEscort)
        throws IOException {
        logInstanceBarrier();
        theSleeveCache.write(anEntry, anExpiry, anEscort);
    }

    public void find(MangledEntry aTemplate, SearchVisitor aVisitor)
        throws IOException {
        logInstanceBarrier();
        theSleeveCache.find(aTemplate, aVisitor);
    }

    public boolean find(SearchVisitor aVisitor, OID aOID, MangledEntry aPreload)
        throws IOException {
        logInstanceBarrier();
        return theSleeveCache.find(aVisitor, aOID, aPreload);
    }

    public LongtermOffer getOffer(OID anOID) throws IOException {
        logInstanceBarrier();
        return theSleeveCache.getOffer(anOID);
    }
    
    public EntryConstraints getConstraints() {
        return theConstraints;
    }

    public void setFields(MangledField[] aListOfFields)
        throws IOException {
        theStore.setFields(aListOfFields);
    }

    public boolean noSchemaDefined() {
        return theStore.noSchemaDefined();
    }

    public void addSubtype(String aType) throws IOException {
        theStore.addSubtype(aType);
    }

    public Set<String> getSubtypes() {
        return theStore.getSubtypes();
    }

    void deleteAllEntrys() throws IOException {
        theSleeveCache.deleteAll();
    }

    void delete() throws IOException {
        theStore.delete();
    }

    void sync() throws IOException {
        theLogger.log(Level.FINE, "Syncing to disk: " + theStore.getType());
        // System.err.println("Syncing to disk: " + theStore.getType());
        theSleeveCache.sync();
    }

    void close() throws IOException {
        theLogger.log(Level.FINE, "Closing: " + theStore.getType());
        // System.err.println("Closing: " + theStore.getType());
        theSleeveCache.close();
        theStore.close();
    }

    public String toString() {
        return "EntryReposImpl: " + theStore.getType() + ", " 
            + theStore.getClass();
    }
}
