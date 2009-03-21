package org.dancres.blitz.entry;

import java.io.IOException;

import java.util.logging.Level;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.OperationStatus;

import org.dancres.blitz.oid.OID;
import org.dancres.blitz.oid.OIDFactory;
import org.dancres.blitz.mangler.MangledEntry;

/**
   TupleLocator implementation to be used on secondary indexes which consist
   of hashcode followed by oid
 */
class IndexLocatorImpl implements TupleLocator {
    private MangledEntry theTemplate;
    private int theReadahead;

    private Cursor theCursor;
    private boolean isFirst = true;

    private DatabaseEntry theKey;
    private DatabaseEntry theHashCode;

    /**
       @param aCursor the cursor to use for iteration
     */
    IndexLocatorImpl(Cursor aCursor, DatabaseEntry aHashCode,
                     MangledEntry aTemplate, int aReadahead) {
        theCursor = aCursor;
        theHashCode = aHashCode;
        theTemplate = aTemplate;
        theReadahead = aReadahead;
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
          at the first record so we must handle this case specially.  Note
          that we only get created if the cursor actually contains records so
          we don't need to do any testing here.
        */
        if (isFirst) {
            loadNext();

            isFirst = false;

            return true;
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
            theKey = new DatabaseEntry();

            if (advance)
                myResult = theCursor.getNextDup(theHashCode, theKey, null);
            else
                myResult = theCursor.getCurrent(theHashCode, theKey, null);
        } catch (DatabaseException aDbe) {
            EntryStorage.theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
            throw new IOException("Dbe");
        }

        // BytePacker myPacker = BytePacker.getMSBPacker(theHashCode.getData());

        // System.out.println("Loop: " + this + ", " + myPacker.getInt(0));


        return (! myResult.equals(OperationStatus.NOTFOUND));
    }

    /**
       @return the OID of the tuple just fetched with
       <code>fetchNext</code>
     */
    public OID getOID() {
        OID myResult = OIDFactory.newOID(theKey.getData());

        return myResult;
    }

    /**
       When you've finished with the TupleLocator instance, call release.
     */

    public void release() throws IOException {
        releaseImpl();

        Readahead.get().add(theTemplate, theReadahead);
    }

    public void releaseImpl() throws IOException {
        try {
            theCursor.close();
        } catch (DatabaseException aDbe) {
            EntryStorage.theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
            throw new IOException("Dbe");
        }
    }
}
