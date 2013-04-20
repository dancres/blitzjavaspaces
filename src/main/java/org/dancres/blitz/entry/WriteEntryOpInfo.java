package org.dancres.blitz.entry;

import java.io.IOException;

import java.util.logging.*;

import net.jini.entry.AbstractEntry;
import org.dancres.blitz.disk.DiskTxn;

import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.mangler.EntryMangler;

import org.dancres.blitz.txn.TxnState;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.arc.CacheBlockDescriptor;
import org.dancres.blitz.arc.RecoverySummary;

class WriteEntryOpInfo implements OpInfo {
    static final long serialVersionUID = -6227846450032402193L;

    private OID theOID;
    private MangledEntry theEntry;
    private long theInitialExpiry;

    private transient boolean wasOnDisk;

    WriteEntryOpInfo(EntrySleeveImpl aSleeve) {
        theOID = aSleeve.getOID();
        theInitialExpiry = aSleeve.getExpiry();
        theEntry = aSleeve.getEntry();
    }

    public boolean isDebugOp() {
        // Write's are never false though they can be aborted
        //
        return false;
    }

    public void restore() throws IOException {
        EntryReposRecovery myRepos =
            EntryRepositoryFactory.get().getAdmin(theEntry.getType());

        if (myRepos == null) {
            SleeveCache.theLogger.log(Level.SEVERE, "Yikes couldn't getAdmin");
            throw new IOException("Couldn't getAdmin");
        }

        if (myRepos.noSchemaDefined()) {
            DiskTxn myTxn = DiskTxn.newTxn();
            
            myRepos.setFields(theEntry.getFields());

            /*
              Update parent repositories - each parent needs to know about
              this subtype
            */
            String[] myParents = theEntry.tearOffParents();

            for (int i = 0; i < myParents.length; i++) {
                EntryRepository myParentRepos =
                    EntryRepositoryFactory.get().get(myParents[i]);
                myParentRepos.addSubtype(theEntry.getType());
            }
            
            myTxn.commit();
        }

        EntrySleeveImpl mySleeve =
            new EntrySleeveImpl(theOID, theEntry, theInitialExpiry);

        RecoverySummary mySummary = myRepos.recover(mySleeve);

        // If the entry was on disk, instance counts are already up-to-date
        // so we don't want to count the entry twice
        //
        wasOnDisk = mySummary.wasOnDisk();

        mySummary.getCBD().release();
    }

    public MangledEntry commit(TxnState aState) throws IOException {
        MangledEntry myEntry = null;

        EntryReposRecovery myRepos =
            EntryRepositoryFactory.get().getAdmin(theEntry.getType());

        CacheBlockDescriptor myCBD = myRepos.load(theOID);

        if (myCBD != null) {
            EntrySleeveImpl mySleeve = (EntrySleeveImpl) myCBD.getContent();

            mySleeve.getState().clear(SleeveState.PINNED);

            /**
             * If Entry is deleted, lease has expired or we've taken it in
             * the same transaction as this write.  In those cases, we don't
             * want to generate any events so we return null.  Note that
             * ENTRY_WRITE is generated elsewhere and thus writes remain
             * visible event wise within transaction scope (i.e. for notify's
             * on the transaction).
             */
            if (! mySleeve.getState().test(SleeveState.DELETED)) {
                myEntry = theEntry;
                // System.err.println("Wrote: " + theOID);
            }

            /*
              If it wasOnDisk we needn't mark it dirty otherwise we must to
              ensure the data reaches disk.  We needn't mark it dirty any
              earlier because if we crash prior to this stage, the Entry
              shouldn't be written anyways.
             */
            if (!wasOnDisk)
                mySleeve.markDirty();

            myCBD.release();
        }

        // Update counters outside of lock
        if (!wasOnDisk)
            myRepos.getCounters().didWrite();

        return myEntry;
    }

    public MangledEntry abort(TxnState aState) throws IOException {
        EntryReposRecovery myRepos =
            EntryRepositoryFactory.get().getAdmin(theEntry.getType());

        CacheBlockDescriptor myCBD = myRepos.load(theOID);

        if (myCBD != null) {
            EntrySleeveImpl mySleeve = (EntrySleeveImpl) myCBD.getContent();

            mySleeve.getState().set(SleeveState.DELETED);
            mySleeve.getState().clear(SleeveState.PINNED);

            /*
              Write has been aborted, we need to ensure we do appropriate
              cleanup.
             */
            mySleeve.markDirty();

            myCBD.release();
        }

        return null;
    }

    public OID getOID() {
        return theOID;
    }

    public String getType() {
        return theEntry.getType();
    }

    public String toString() {
        String myEntryRep = "Unusable";

        try {
            myEntryRep =
                AbstractEntry.toString(
                    EntryMangler.getMangler().unMangle(theEntry));
        } catch (Exception anE) {
            // Nothing to do
            myEntryRep = myEntryRep + ": " + anE.getClass();
        }

        return "W : " + theEntry.getType() + " : " + theOID + " : " +
            theInitialExpiry + " : " + theEntry.sizeOf() + " : " +
            theEntry.getCodebase() + " [ " + myEntryRep + " ]";
    }
}
