package org.dancres.blitz.entry;

import java.io.IOException;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.txn.TxnState;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.arc.CacheBlockDescriptor;

class FindEntryOpInfo implements OpInfo {
static final long serialVersionUID = -7864125234426980910L;

    private boolean isTake;
    private OID theOID;
    private String theType;

    FindEntryOpInfo(String aType, OID aOID, boolean doTake) {
        isTake = doTake;
        theType = aType;
        theOID = aOID;
    }

    public boolean isDebugOp() {
        // Find's are always non-null so never false though they can be aborted
        //
        return false;
    }

    public void restore() throws IOException {
        // Nothing to do - a previous write op info will restore the entry
        // or it's already deleted in which case we'll find out at commit
    }

    public MangledEntry commit(TxnState aState) throws IOException {
        MangledEntry myEntry = null;

        EntryReposRecovery myRepos =
            EntryRepositoryFactory.get().getAdmin(theType);

        // System.err.println("Looking for: " + theOID);
        
        CacheBlockDescriptor myCBD = myRepos.load(theOID);

        if (myCBD != null) {
            // System.err.println("Got CBD: " + theOID);

            EntrySleeveImpl mySleeve = (EntrySleeveImpl) myCBD.getContent();

            boolean isDeleted = mySleeve.getState().test(SleeveState.DELETED);

            if (isTake) {
                // System.err.println("Deleted: " + theOID);
                mySleeve.getState().set(SleeveState.DELETED);
                mySleeve.markDirty();

                /*
                  We could leave this dead entry sitting in cache and wait
                  for it to be aged out but it's better to force the state
                  to disk early so as to clean out the database.  Of course,
                  we only do this for stuff that's come from disk - there's
                  no point in doing it for something that's never been on
                  disk.

                  The net effect of this is to improve the ratio of live to
                  dead data on the disk in favour of liveness which has
                  benefits for out of cache searching by reducing the number
                  of disk blocks we scan and skip.
                  */
                if (! mySleeve.getState().test(SleeveState.NOT_ON_DISK)) {
                    // System.err.println("On disk - early flush: " + theOID);
                    myRepos.flush(myCBD);
                }
            } else {
                if (!isDeleted)
                    myEntry = mySleeve.getEntry();
            }

            myCBD.release();

            // Update counters outside lock
            if (isTake)
                myRepos.getCounters().didTake();
            else
                myRepos.getCounters().didRead();
        }

        return myEntry;
    }

    public MangledEntry abort(TxnState aState) throws IOException {
        MangledEntry myEntry = null;

        EntryReposRecovery myRepos =
            EntryRepositoryFactory.get().getAdmin(theType);

        CacheBlockDescriptor myCBD = myRepos.load(theOID);

        if (myCBD != null) {

            EntrySleeveImpl mySleeve = (EntrySleeveImpl) myCBD.getContent();

            if (!mySleeve.getState().test(SleeveState.DELETED))
                myEntry = mySleeve.getEntry();

            myCBD.release();
        }

        return myEntry;
    }

    public OID getOID() {
        return theOID;
    }

    public String getType() {
        return theType;
    }

    public String toString() {
        if (isTake)
            return "T : " + theType + " : " + theOID;
        else
            return "R : " + theType + " : " + theOID;
    }
}
