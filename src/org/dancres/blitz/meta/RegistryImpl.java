package org.dancres.blitz.meta;

import java.io.Serializable;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;

import java.util.logging.*;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.DatabaseConfig;

import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.DiskTxn;
import org.dancres.blitz.disk.Syncable;

import org.dancres.blitz.Logging;

import org.dancres.util.ObjectTransformer;

class RegistryImpl implements Registry {
    private Database theDb;
    private String theName;

    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.meta.Registry");

    RegistryImpl(String aName, Initializer anInitializer) throws IOException {
        theName = aName;

        try {
            // theDb.set_flags(Db.DB_REVSPLITOFF);

            DiskTxn myTxn = DiskTxn.newStandalone();

            DatabaseConfig myConfig = new DatabaseConfig();

            try {
                if (exists(aName)) {
                    theDb = Disk.newDb(myTxn.getDbTxn(), getDbNameFor(aName),
                                       myConfig);
                } else {
                    myConfig.setAllowCreate(true);
                    theDb = Disk.newDb(myTxn.getDbTxn(), getDbNameFor(aName),
                                       myConfig);
                    
                    if (anInitializer != null)
                        anInitializer.execute(getAccessor(myTxn));
                }
            } finally {
                myTxn.commit(true);
            }

        } catch (FileNotFoundException aFNFE) {
            theLogger.log(Level.SEVERE, "File not found", aFNFE);
            try {
                theDb.close();
            } catch (DatabaseException aDbe) {
                // Don't care anymore
            }

            throw new IOException("File not found");
        } catch (DatabaseException aDBE) {
            theLogger.log(Level.SEVERE, "DatabaseException", aDBE);
            try {
                theDb.close();
            } catch (DatabaseException aDbe) {
                // Don't care anymore
            }
            throw new IOException("DatabaseException");
        }
    }

    public RegistryAccessor getAccessor() throws IOException {
        return new RegistryAccessorImpl(this);
    }

    public RegistryAccessor getAccessor(DiskTxn aTxn) throws IOException {
        return new RegistryAccessorImpl(this, aTxn);
    }

    Serializable load(DiskTxn aTxn, byte[] aKey) throws IOException {
        DatabaseEntry myKey = new DatabaseEntry(aKey);
        DatabaseEntry myData = new DatabaseEntry();

        try {
            OperationStatus myResult =
                theDb.get(aTxn.getDbTxn(), myKey, myData, null);

            if (myResult.equals(OperationStatus.NOTFOUND))
                return null;
            else {
                try {
                    return ObjectTransformer.toObject(myData.getData());
                } catch (Exception anE) {
                    theLogger.log(Level.SEVERE, "Failed to unpack metadata",
                                  anE);
                    throw new IOException("Failed to unpack metadata");
                }
            }
        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Failed to load metadata",
                          anE);
            throw new IOException("Failed to load metadata");
        }
    }

    byte[] loadRaw(DiskTxn aTxn, byte[] aKey) throws IOException {
        DatabaseEntry myKey = new DatabaseEntry(aKey);
        DatabaseEntry myData = new DatabaseEntry();
        
        try {
            OperationStatus myResult =
                theDb.get(aTxn.getDbTxn(), myKey, myData, null);
        
            if (myResult.equals(OperationStatus.NOTFOUND))
                return null;
            else
                return myData.getData();
        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Got DatabaseException", aDbe);
            throw new IOException("Got Dbe");
        }
    }

    void save(DiskTxn aTxn, byte[] aKey, Serializable anObject)
        throws IOException {

        DatabaseEntry myKey = new DatabaseEntry(aKey);

        byte[] myFlatObject = null;

        try {
            myFlatObject = ObjectTransformer.toByte(anObject);
        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Failed to pack metadata",
                          anE);
            throw new IOException("Failed to pack metadata");
        }

        DatabaseEntry myData = new DatabaseEntry(myFlatObject);

        try {
            theDb.put(aTxn.getDbTxn(), myKey, myData);
        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Got DatabaseException", aDbe);
            throw new IOException("Got Dbe");
        }
    }

    MetaIterator readAll(DiskTxn aTxn) throws IOException {
        return new MetaIteratorImpl(theDb, aTxn);
    }

    void delete(DiskTxn aTxn, byte[] aKey) throws IOException {

        try {
            theDb.delete(aTxn.getDbTxn(), new DatabaseEntry(aKey));
        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Got DatabaseException", aDbe);
            throw new IOException("Got Dbe");
        }
    }

    public void close() throws IOException {
        try {
            theDb.close();
        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Got DatabaseException", aDbe);
            throw new IOException("Got Dbe");
        }
    }

    void delete() throws IOException {
        Disk.deleteDb(getDbNameFor(theName));
    }

    static boolean exists(String aName) {
        return Disk.dbExists(getDbNameFor(aName));
    }
    
    private static String getDbNameFor(String aRegName) {
        return aRegName + "-meta";
    }
}
