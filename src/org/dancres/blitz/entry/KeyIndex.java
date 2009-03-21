package org.dancres.blitz.entry;

import java.io.IOException;
import java.io.Serializable;
import java.io.FileNotFoundException;

import java.util.logging.*;
import java.util.HashMap;

import com.sleepycat.je.Database;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.DeadlockException;
import com.sleepycat.je.LockNotGrantedException;

import org.dancres.util.BytePacker;

import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.mangler.MangledField;

import org.dancres.blitz.disk.DiskTxn;
import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.BackoffGenerator;

import org.dancres.blitz.oid.OID;
import org.dancres.blitz.oid.OIDFactory;

/**
   This class serves as a key generator for a DB secondary index and knows how
   to associate with the main db. <P>

   Note that duplicate keys are allowed in these databases because various
   key values can, of course, hash to the same value.
 */
class KeyIndex implements Serializable {
    private static Logger theLogger =
        Logger.getLogger("org.dancres.disk.KeyIndex");

    private String theType;
    private String theIndexName;
    private int theOffset;

    private transient Database theSecondaryDb;

    KeyIndex(String aType, String anIndexName, int anOffset) {
        theType = aType;
        theIndexName = anIndexName;
        theOffset = anOffset;
    }

    public String getName() {
        return theIndexName;
    }

    void init() throws IOException {
        /*
          Open db and create if necessary, invoke associate
          ignore bucket 0 entities.  Implies entry's in
          main DB should store Object so they can store both
          MangledEntry instances and arbitrary data.

          Keep a note of Db instance as we'll need it to do close.
         */
        try {
            DatabaseConfig myConfig = new DatabaseConfig();
            myConfig.setAllowCreate(true);
            myConfig.setSortedDuplicates(true);

            // Allow duplicates
            //
            // theSecondaryDb.set_flags(Db.DB_DUP | Db.DB_REVSPLITOFF);

            theSecondaryDb = Disk.newDb(null, theType + "_" + theIndexName,
                                        myConfig);

        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
            throw new IOException("Dbe");
        }
    }

    void index(OID anId, MangledField[] aFields, DiskTxn aTxn)
        throws DatabaseException {

        MangledField myField = aFields[theOffset];

        if (myField.isNull())
            return;

        // Safety check
        if (myField.getName().equals(theIndexName)) {
            byte[] myHash = new byte[4];
            BytePacker myPacker = BytePacker.getMSBPacker(myHash);
            myPacker.putInt(myField.hashCode(), 0);

            byte[] myTarget = OIDFactory.getKey(anId);

            theSecondaryDb.put(aTxn.getDbTxn(),
                               new DatabaseEntry(myHash),
                               new DatabaseEntry(myTarget));
        } else {
            throw new RuntimeException("Eeek, field we found isn't ours");
        }
    }

    void unIndex(OID anId, MangledField[] aFields)
        throws IOException {

        MangledField myField = aFields[theOffset];
        
        if (myField.isNull())
            return;

        // Pack the hash as key into secondary index
        byte[] myHash = new byte[4];
        BytePacker myPacker = BytePacker.getMSBPacker(myHash);
        myPacker.putInt(myField.hashCode(), 0);
        DatabaseEntry myHashKey = new DatabaseEntry(myHash);

        // We'll need to compare the data of each index entry with the key
        // of our target entry
        byte[] myEntryKey = OIDFactory.getKey(anId);
        DatabaseEntry myKeyValue = new DatabaseEntry(myEntryKey);
        int myRetryCount = 0;

        do {
            DiskTxn myTxn = DiskTxn.newNonBlockingStandalone();
            Cursor myCursor = newCursor(myTxn);

            try {
                // Locate all entries under the hashcode value of our field
                OperationStatus myResult = myCursor.getSearchBoth(myHashKey,
                                                                  myKeyValue,
                                                                  null); // LockMode.RMW);

                /*
                  If we crashed whilst doing a delete, some indexes may not have
                  an entry, that's okay we just note it in the log
                */
                if ((myResult.equals(OperationStatus.NOTFOUND)) ||
                    (myResult.equals(OperationStatus.KEYEMPTY)))
                    theLogger.log(Level.SEVERE,
                                  "Warning, didn't find an index entry " + anId +
                                  ", " + myField.hashCode() + ", " +
                                  myResult.equals(OperationStatus.NOTFOUND));
                else {
                    myCursor.delete();
                }
                
                myCursor.close();
                
                myTxn.commit();
                
                if (myRetryCount != 0) {
                    theLogger.log(Level.FINE,
                                  "Total retries: " + myRetryCount);
                }

                return;
            } catch (DatabaseException aDbe) {
                /*
                  Argh, docs say it'll throw Deadlock but code says
                  LockNotGranted.....
                 */
                if ((aDbe instanceof DeadlockException) ||
                    (aDbe instanceof LockNotGrantedException)) {

                    if (theLogger.isLoggable(Level.FINEST))
                        theLogger.log(Level.FINEST, "Got lock exception", aDbe);

                    try {
                        myCursor.close();
                    } catch (DatabaseException aCDbe) {
                        theLogger.log(Level.SEVERE, "Got Dbe", aCDbe);
                        throw new IOException("Dbe");
                    }
                    myTxn.abort();
                    
                    // System.err.println("Aborting index delete, retry: " +
                    //                    myRetryCount);

                    ++myRetryCount;

                    BackoffGenerator.pause();

                } else {
                    theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
                    throw new IOException("Dbe");
                }
            }
        } while (true);
    }

    private boolean compare(byte[] aBytes, byte[] anotherBytes) {
        for (int i = 0; i < aBytes.length; i++) {
            if (aBytes[i] != anotherBytes[i])
                return false;
        }

        return true;
    }

    Cursor newCursor() throws IOException {
        try {
            return theSecondaryDb.openCursor(DiskTxn.getActiveDbTxn(), null);
        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
            throw new IOException("Dbe");
        }
    }

    Cursor newCursor(DiskTxn aTxn) throws IOException {
        try {
            if (aTxn != null)
                return theSecondaryDb.openCursor(aTxn.getDbTxn(), null);
            else
                return theSecondaryDb.openCursor(null, null);
        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
            throw new IOException("Dbe");
        }
    }

    void close() throws IOException {
        try {
            theSecondaryDb.close();
        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Got Dbe", aDbe);
            throw new IOException("Dbe");
        }
    }

    void delete() throws IOException {
        Disk.deleteDb(theType + "_" + theIndexName);
    }
}
