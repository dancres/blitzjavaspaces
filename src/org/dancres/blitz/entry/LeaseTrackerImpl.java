package org.dancres.blitz.entry;

import java.io.IOException;

import java.util.logging.Level;

import com.sleepycat.je.Database;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.LockMode;

import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.DiskTxn;

import org.dancres.blitz.lease.ReapFilter;

/**
   Database layout:

   <ul>
   <li>Sleeve expiries are divided into buckets according to the allocator
   id of their EntryUID.</li>
   <li>Each bucket contains entries consisting of the expiry(long) followed by
   oid(long) making a total of 16 bytes.</li>
   <li>Each bucket's entries are sorted in ascending order by expiry.</li>
   <ul>

   <p> Typical page size is 4096 bytes / 16 = 256 entries per page.
   Thus, if we configure sufficient allocators (good for concurrency anyway)
   we can ensure we get efficient grouping of entries against pages which
   allows.for good cache behaviour.</p>
*/
class LeaseTrackerImpl implements LeaseTracker {
    private Database theLeasesDb;
    private String theType;

    private int theMaxAllocId;

    LeaseTrackerImpl(String aType, int aMaxAllocId) throws IOException {
        EntryStorage.theLogger.log(Level.FINE, "Tracker created: " + aType);

        theType = aType;
        theMaxAllocId = aMaxAllocId;

        try {
            DatabaseConfig myConfig = new DatabaseConfig();
            myConfig.setAllowCreate(true);
            myConfig.setSortedDuplicates(true);

            theLeasesDb = Disk.newDb(null, theType + "_leases", myConfig);

        } catch (DatabaseException aDbe) {
            EntryStorage.theLogger.log(Level.SEVERE,
                                       "Tracker failed to init db",
                                       aDbe);
            throw new IOException();
        }
    }

    public void bringOutTheDead(EntryReaper aReaper) throws IOException {
        /*
          Read blocks from disk recovering all necessary entries which
          are expired at this point in time.  Then we can release the
          txn and schedule up some deletes in cache via the ReapFilter.
          Repeat for each bucket - we don't even need to expire all dead items
          in a block...
         */

        EntryStorage.theLogger.log(Level.INFO,
                                   "LeaseTracker bringing out the dead for: " +
                                   theType);

        // System.err.println("LeaseTracker bringing out the dead for: " +
        //                            theType);
        DiskTxn myTxn = null;

        myTxn = DiskTxn.newStandalone();

        try {
            Cursor myCursor = theLeasesDb.openCursor(null, null);

            aReaper.clean(new ExpiredLocatorImpl(myCursor));

            myCursor.close();
        } catch (Throwable aT) {
            EntryStorage.theLogger.log(Level.SEVERE,
                "Exception during reap",
                aT);
            throw new IOException("Fatal error in reap");
        } finally {
            myTxn.commit();
        }

        EntryStorage.theLogger.log(Level.INFO, 
                                   "LeaseTracker claimed the dead for: " +
                                   theType);
        // System.err.println("LeaseTracker claimed the dead for: " +
        //                            theType);
    }

    public void delete(PersistentEntry anEntry) throws IOException {
        byte[] myOid = LeaseRecordUtils.getId(anEntry);
        DiskTxn myTxn = DiskTxn.newStandalone();

        // System.err.println("Delete a lease");

        try {
            DatabaseEntry myKey =
                new DatabaseEntry(LeaseRecordUtils.getBucketKey(anEntry));
            DatabaseEntry myValue = new DatabaseEntry();

            Cursor myCursor = theLeasesDb.openCursor(myTxn.getDbTxn(), null);
            OperationStatus myStatus = 
                myCursor.getSearchKey(myKey, myValue, LockMode.RMW);
            
            while ((! myStatus.equals(OperationStatus.NOTFOUND)) &&
                   (! LeaseRecordUtils.isKey(myValue.getData(), myOid))) {
                // System.err.println("Skipping for del");

                myValue = new DatabaseEntry();
                myStatus = myCursor.getNextDup(myKey, myValue, LockMode.RMW);
            }

            if (! myStatus.equals(OperationStatus.NOTFOUND)) {
                // System.err.println("Lease killed");
                myCursor.delete();
            }

            myCursor.close();
        } catch (DatabaseException aDbe) {
            EntryStorage.theLogger.log(Level.SEVERE, "Failed to delete entry",
                                  aDbe);
            throw new IOException();
        } finally {
            myTxn.commit();
        }
    }

    public void update(PersistentEntry anEntry) throws IOException {
        // System.err.println("Update a lease");
        delete(anEntry);
        write(anEntry);
    }

    public void write(PersistentEntry anEntry) throws IOException {
        // System.err.println("Write a lease");
        byte[] myBucketKey = LeaseRecordUtils.getBucketKey(anEntry);
        byte[] myLeaseEntry = LeaseRecordUtils.getLeaseEntry(anEntry);

        DiskTxn myTxn = DiskTxn.newStandalone();

        try {
            theLeasesDb.put(myTxn.getDbTxn(), new DatabaseEntry(myBucketKey),
                            new DatabaseEntry(myLeaseEntry));
        } catch (DatabaseException aDbe) {
            EntryStorage.theLogger.log(Level.SEVERE,
                                       "Failed to insert new entry",
                                       aDbe);
            throw new IOException();
        } finally {
            myTxn.commit();
        }
    }

    public void close() throws IOException {
        try {
            theLeasesDb.close();
        } catch (DatabaseException aDbe) {
            EntryStorage.theLogger.log(Level.SEVERE,
                                       "Failed to close lease db",
                                       aDbe);
            throw new IOException();
        }
    }

    public void delete() throws IOException {
        Disk.deleteDb(theType + "_leases");
    }
}
