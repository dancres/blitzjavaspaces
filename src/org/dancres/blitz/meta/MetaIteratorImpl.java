package org.dancres.blitz.meta;

import java.io.IOException;
import java.io.Serializable;

import java.util.logging.*;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.OperationStatus;

import org.dancres.blitz.disk.DiskTxn;

import org.dancres.util.ObjectTransformer;

class MetaIteratorImpl implements MetaIterator {
    private Database theDb;
    private Cursor theCursor;
    private Transaction theTxn;

    MetaIteratorImpl(Database aDb, DiskTxn aTxn) {
        theDb = aDb;
        theTxn = aTxn.getDbTxn();
    }

    public MetaEntry fetch() throws IOException {
        OperationStatus myResult = OperationStatus.NOTFOUND;
        
        DatabaseEntry myKey = new DatabaseEntry();
        DatabaseEntry myData = new DatabaseEntry();

        try {
            myResult = getCursor().getNext(myKey, myData, null);
        } catch (Exception anE) {
            RegistryImpl.theLogger.log(Level.SEVERE, "Failed to fetch", anE);
            throw new IOException();
        }

        if (myResult.equals(OperationStatus.NOTFOUND)) {
            return null;
        }

        try {
            Serializable myObject = ObjectTransformer.toObject(myData.getData());

            return new MetaEntryImpl(myKey.getData(), myObject);

        } catch (Exception anException) {
            RegistryImpl.theLogger.log(Level.SEVERE,
                                       "Failed to read meta data",
                                       anException);

            throw new IOException("Couldn't read meta data");
        }
    }

    public void release() throws IOException {
        try {
            getCursor().close();
        } catch (Exception anE) {
            RegistryImpl.theLogger.log(Level.SEVERE, "Failed to release", anE);
            throw new IOException();
        }
    }

    private Cursor getCursor() throws IOException {
        try {
            if (theCursor == null) {
                theCursor = theDb.openCursor(theTxn, null);
            }

            return theCursor;
        } catch (Exception anE) {
            RegistryImpl.theLogger.log(Level.SEVERE,
                    "Failed to getCursor", anE);
            throw new IOException();
        }
    }
}
