package org.dancres.blitz.entry;

import java.io.IOException;

import java.util.logging.Level;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.OperationStatus;

import org.dancres.blitz.oid.OID;
import org.dancres.blitz.oid.OIDFactory;

/**
   TupleLocator implementation to be used on Lease tables. See LeaseTrackerImpl
   for information on the database format.
 */
class ExpiredLocatorImpl implements TupleLocator {
    private Cursor theCursor;
    private boolean isFirst = true;

    private DatabaseEntry theLeaseRecord;
    private DatabaseEntry theLeaseBucket;

    private long theScanTime;

    /**
       @param aCursor the cursor to use for iteration
     */
    ExpiredLocatorImpl(Cursor aCursor) {
        theCursor = aCursor;
        theScanTime = System.currentTimeMillis();
        theLeaseBucket = new DatabaseEntry();
    }

    /**
       Invoke this to load the next matching Tuple.

       @return <code>true</code> if there was a tuple, <code>false</code>
       otherwise
     */
    public boolean fetchNext() throws IOException {
        /*
          The cursor will have been initialised whilst we decided which
          field was best to search on which means it will already be pointed
          at the first record so we must handle this case specially.
        */
        if (isFirst) {
            isFirst = false;

            return loadNext();
        } else {
            return loadNextMove();
        }
    }

    private boolean loadNextMove() throws IOException {
        return loadNext(true);
    }

    private boolean loadNext() throws IOException {
        return loadNext(false);
    }

    private boolean loadNext(boolean advance) throws IOException {
        OperationStatus myResult;

        try {
            theLeaseRecord = new DatabaseEntry();

            if (advance)
                myResult = theCursor.getNextDup(theLeaseBucket, theLeaseRecord,
                                                null);
            else
                myResult = theCursor.getFirst(theLeaseBucket, theLeaseRecord,
                                                null);
        } catch (DatabaseException aDbe) {
            EntryStorage.theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
            throw new IOException("Dbe");
        }

        if ((myResult.equals(OperationStatus.NOTFOUND)) ||
            (theScanTime <
             LeaseRecordUtils.unpackExpiry(theLeaseRecord.getData()))) {
            
            if (myResult != OperationStatus.NOTFOUND)
                // System.err.println("Exceeding Expiry: " +
                //                    LeaseRecordUtils.unpackExpiry(theLeaseRecord.getData()));

            // Try for the next non-dupe
            theLeaseRecord = new DatabaseEntry();

            try {
                // System.err.println("Skipping to next bucket");

                myResult =
                    theCursor.getNextNoDup(theLeaseBucket, theLeaseRecord, null);

                if (myResult.equals(OperationStatus.SUCCESS))
                    return true;
            } catch (DatabaseException aDbe) {
                EntryStorage.theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
                throw new IOException("Dbe");
            }

            return false;
        } else {
            // System.err.println("Maybe expiring one: " +
            //                    LeaseRecordUtils.unpackExpiry(theLeaseRecord.getData()));
            return true;
        }
    }

    /**
       @return the OID of the tuple just fetched with
       <code>fetchNext</code>
     */
    public OID getOID() {
        OID myResult =
            OIDFactory.newOID(
                LeaseRecordUtils.unpackId(theLeaseRecord.getData()));

        // System.err.println("Time is up for: " + myResult);

        return myResult;
    }

    /**
       When you've finished with the TupleLocator instance, call release.
     */
    public void release() throws IOException {
        try {
            theCursor.close();
        } catch (DatabaseException aDbe) {
            EntryStorage.theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
            throw new IOException("Dbe");
        }
    }
}
