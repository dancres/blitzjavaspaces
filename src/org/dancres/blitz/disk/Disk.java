package org.dancres.blitz.disk;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import java.util.logging.*;

import net.jini.config.ConfigurationException;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockStats;
import com.sleepycat.je.EnvironmentStats;
import com.sleepycat.je.StatsConfig;

import org.dancres.blitz.Logging;

import org.dancres.blitz.config.ConfigurationFactory;
import org.dancres.util.NumUtil;

/**
   This class is responsible for managing the underlying BerkeleyDB
   infrastructure.  Anyone using this class to create/manipulate databases
   should likely be registered as a Syncable instance so that it is aware
   of requests for synchronization and closure of databases.
 */
public class Disk {
    private static Environment theEnv;
    // private static TransactionConfig theTxnConfig;

    private static String theLocation;
    private static long theDbCacheSize;
    private static int theMaxTxns;
    private static int maxDbLog;
    private static int maxLogIteratorBuffer;
    private static int maxLogBuffers;
    private static int maxLogBufferBytes;
    private static int maxNodeEntries;

    private static RandomAccessFile theLockFile;
    private static FileChannel theLockChannel;
    private static FileLock theLock;

    private static List theDbs;

    private static boolean isTransient;

    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.disk.Disk");

    private static List theSyncTasks = new ArrayList();

    static {
        try {
            theLocation =
                (String) ConfigurationFactory.getEntry("persistDir",
                                                       String.class);

            try {
                String myNewCacheForm =
                    ((String) ConfigurationFactory.getEntry("dbCache",
                        String.class,
                        new Long(1024 * 1024).toString()));

                try {
                    theDbCacheSize = NumUtil.convertToBytes(myNewCacheForm);
                } catch (IllegalArgumentException anIE) {
                    theLogger.log(Level.SEVERE, "Failed to parse cache size",
                        anIE);
                    throw new Error("Cannot start - failed to parse cace size");
                }

            } catch (ConfigurationException aCE) {
                // Ignore it for now and try the old format
                //
                theDbCacheSize =
                    ((Long) ConfigurationFactory.getEntry("dbCache",
                        long.class,
                        new Long(1024 * 1024))).longValue();
            }

            theMaxTxns =
                ((Integer) ConfigurationFactory.getEntry("maxDbTxns",
                                                         int.class,
                                                         new Integer(256))).intValue();

            maxDbLog =
                ((Integer) ConfigurationFactory.getEntry("maxDbLog",
                                                         int.class,
                                                         new Integer(10000000))).intValue();

            maxLogIteratorBuffer =
                ((Integer) ConfigurationFactory.getEntry("maxLogIteratorBuff",
                                                         int.class,
                                                         new Integer(1024))).intValue();

            maxLogBuffers =
                ((Integer) ConfigurationFactory.getEntry("maxLogBuffers",
                                                         int.class,
                                                         new Integer(5))).intValue();

            maxLogBufferBytes =
                ((Integer) ConfigurationFactory.getEntry("maxLogBufferBytes",
                                                         int.class,
                                                         new Integer(4620000))).intValue();

            maxNodeEntries =
                ((Integer) ConfigurationFactory.getEntry("maxNodeEntries",
                                                         int.class,
                                                         new Integer(128))).intValue();
                    
            theLogger.log(Level.INFO, "Max txns: " + theMaxTxns);
            theLogger.log(Level.INFO, "DbCache: " + theDbCacheSize);
        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Couldn't get Disk config",
                          anE);
            throw new Error("Disk didn't start", anE);
        }
    }

    public static void setTransient(boolean transientDisk) {
        isTransient = transientDisk;
    }

    public static void init() {
        try {
            new File(theLocation).mkdirs();

            lockLocation();

            Properties myDbProps = new Properties();

            InputStream myPropsStream =
                Disk.class.getResourceAsStream("db.properties");

            if (myPropsStream == null)
                throw new IOException("Failed to load default db settings");

            myDbProps.load(myPropsStream);

            if (isTransient) {
                theLogger.log(Level.INFO,
                              "Forced checkpointer on for transient ops");
                myDbProps.setProperty("je.env.runCheckpointer", "true");
            }

            myDbProps.setProperty("je.log.fileMax",
                                  Integer.toString(maxDbLog));

            /*
              Run a benchmark with these disabled, then run them again
              enabled.
            myDbProps.setProperty("je.log.iteratorReadSize",
                                  Integer.toString(maxLogIteratorBuffer));

            myDbProps.setProperty("je.log.numBuffers",
                                  Integer.toString(maxLogBuffers));

            myDbProps.setProperty("je.log.totalBufferBytes",
                                  Integer.toString(maxLogBufferBytes));

            myDbProps.setProperty("je.nodeMaxEntries",
                                  Integer.toString(maxNodeEntries));

            */

            EnvironmentConfig myConfig = new EnvironmentConfig(myDbProps);
            myConfig.setCacheSize(theDbCacheSize);
            myConfig.setTransactional(true);
            myConfig.setAllowCreate(true);

            // theTxnConfig = new TransactionConfig();
            // theTxnConfig.setNoSync(true);

            theLogger.log(Level.INFO, "Opening Database");

            theEnv = new Environment(new File(theLocation), myConfig);

            theLogger.log(Level.INFO, "Database recovery complete");

            theDbs = theEnv.getDatabaseNames();

            WriteDaemon.init();

        } catch (UnsatisfiedLinkError aULE) {
            theLogger.log(Level.SEVERE, "Warning, didn't load library for db cleanly - are you using Inca X?", aULE);
            theLogger.log(Level.SEVERE, "Try restarting the container...");
            theLogger.log(Level.SEVERE, "Ignoring library load failure - if Blitz doesn't boot, check your library path");
            ClassLoader myLoader = Disk.class.getClassLoader();
            throw new Error("Disk didn't start: " + myLoader, aULE);
        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Couldn't start Disk",
                          anE);
            throw new Error("Disk didn't start", anE);
        } catch (Error anErr) {
            theLogger.log(Level.SEVERE, "Got error", anErr);
            theLogger.log(Level.SEVERE, anErr.getMessage());

            ClassLoader myLoader = Disk.class.getClassLoader();
            throw new Error("Disk didn't start: " + myLoader, anErr);
        }
    }

    /**
       Eradicate state associated with Disk
     */
    public static synchronized void destroy() {
        deleteFiles(getDbLocation());
    }

    /**
       Eradicate state held in some specific directory
     */
    public static synchronized void clean(String aDir) {
        deleteFiles(aDir);
    }

    private static String getDbLocation() {
        return theLocation;
    }

    public static void add(Syncable aSyncable) {
        synchronized(theSyncTasks) {
            theSyncTasks.add(aSyncable);
        }
    }

    public static void remove(Syncable aSyncable) {
        synchronized(theSyncTasks) {
            theSyncTasks.remove(aSyncable);
        }
    }

    private static Syncable[] getSyncTasks() {
        synchronized(theSyncTasks) {
            Syncable[] myTasks = new Syncable[theSyncTasks.size()];
            return (Syncable[]) theSyncTasks.toArray(myTasks);
        }
    }

    public static void stop() throws Exception {
        if (theEnv != null) {
            theLogger.info("BDB closing");
            try {
                WriteDaemon.get().halt();

                close();

                theEnv.sync();
                theEnv.close();
                theLogger.info("BDB closed");
            } catch (DatabaseException aDBE) {
                theLogger.log(Level.SEVERE, "Couldn't close BDB", aDBE);
                throw new Exception("Couldn't close BDB");
            }
        }
    }

    /**
       If the backup directory doesn't exist, it will be created.
       If the backup directory does exist, the caller should ensure that
       it has been cleared before the backup is performed.  This permits
       the caller to determine what to do with old backups beforehand.
     */
    public static void backup(String aDestDir)
        throws IOException {
        
        File myDest = new File(aDestDir);

        myDest.mkdir();

        File[] myFiles = myDest.listFiles();

        if (myFiles.length > 0)
            throw new IOException("Backup dir should be empty");

        BackupTask myTask = new BackupTask(new File(getDbLocation()), myDest);

        try {
            /*
              We cannot use sync because we need WriteDaemon to perform
              the backup post sync'ing the queue.  This is required to
              prevent WriteDaemon from performing further updates whilst
              we perform the backup and prevents issues with state leakage.
             */
            Syncable[] mySyncables = getSyncTasks();

            for (int i = 0; i < mySyncables.length; i++) {
                mySyncables[i].sync();
            }

            WriteDaemon.get().queue(myTask);
            WriteDaemon.get().push();

        } catch (Exception anE) {
            IOException myIOE = new IOException("Couldn't start sync");
            myIOE.initCause(anE);
            throw myIOE;
        }

        myTask.waitForCompletion();
    }

    private static void deleteFiles(String aDir) {
        theLogger.log(Level.INFO, "Deleting: " + aDir);

        File myDir = new File(aDir);

        File[] myFiles = myDir.listFiles();

        if (myFiles == null)
            return;

        for (int i = 0; i < myFiles.length; i++) {
            File myFile = myFiles[i];

            if (myFile.isFile()) {
                theLogger.log(Level.INFO, "Deleting: " + myFile);
                myFile.delete();
            } else {
                theLogger.log(Level.INFO, "Leaving: " + myFile);
            }
        }
    }

    private static void lockLocation() throws IOException {
        theLockFile = new RandomAccessFile(new File(theLocation,
                                                    "blitz.lock"), "rw");

        theLockChannel = theLockFile.getChannel();

        theLock = theLockChannel.tryLock();

        if (theLock == null)
            throw new IOException("Couldn't lock, are you running another Blitz instance in this directory?");
    }

    private static void close() throws Exception {
        Syncable[] mySyncables = getSyncTasks();

        for (int i = 0; i < mySyncables.length; i++) {
            mySyncables[i].close();
        }

        if (theLock != null)
            theLock.release();

        if (theLockChannel != null)
            theLockChannel.close();

        if (theLockFile != null)
            theLockFile.close();
    }

    /**
       <p>Blocks the caller whilst a sync-to-disk is performed.
       Sync-to-disk requires:</p>

       <ol>
       <li>Flush dirty state from caches into WriteDaemon queue</li>
       <li>Flush queue</li>
       <li>Wait for queue flush</li>
       <li>Checkpoint Db</li>
       </ol>

     */
    public static void sync() throws Exception {
        sync(null);
    }

    /**
       @param aCompletionTask if null, the caller is blocked until
       state has successfully been flushed to disk.  Note that the point
       at which the caller will be awoken is guarenteed to be after state
       was sync'd but may not be immediately afterwards.  If non-null,
       the caller will not be blocked because the code that is dependent
       on completion of the flush is assumed to be in the passed completion
       task.  As per the null case, this completion task will be executed
       at some point after WriteDaemon flushed the queue but not necessarily
       immediately.

       @see org.dancres.blitz.disk.WriteDaemon
     */
    public static void sync(Runnable aCompletionTask)
        throws Exception {

        Syncable[] mySyncables = getSyncTasks();

        for (int i = 0; i < mySyncables.length; i++) {
            mySyncables[i].sync();
        }

        SyncFinalizer myCompleter = new SyncFinalizer(theEnv, aCompletionTask);
        WriteDaemon.get().push(myCompleter);

        /*
          SyncFinalizer will figure out whether to block the caller.
          If aCompletionTask is non-null, waitForCompletion will not block
          otherwise it will.
         */
        myCompleter.waitForCompletion();
    }
    
    public static Database newDb(Transaction aTxn, String aDbName,
                                 DatabaseConfig aConfig)
        throws DatabaseException {

        aConfig.setTransactional(true);
        
        synchronized(theDbs) {
            theDbs.add(aDbName);
        }

        try {
            return theEnv.openDatabase(aTxn, aDbName, aConfig);
        } catch (DatabaseException aDBE) {
            synchronized(theDbs) {
                theDbs.remove(aDbName);
                throw aDBE;
            }
        }
    }

    /**
       Delete's the underlying Db database.  WARNING:  This can be blocked
       by checkpoints or failed by replication if insufficient time has passed.
     */
    public static void deleteDb(String aName) throws IOException {
        try {
            synchronized(theDbs) {
                theDbs.remove(aName);
            }

            theEnv.removeDatabase(null, aName);
        } catch (DatabaseException aDbe) {
            theLogger.log(Level.SEVERE, "Couldn't delete Db", aDbe);
            throw new IOException("Failed to delete db");
        }
    }

    public static boolean dbExists(String aName) {
        synchronized(theDbs) {
            return theDbs.contains(aName);
        }
    }

    static Transaction newTxn() throws DatabaseException {
        TransactionConfig myConfig = new TransactionConfig();
        myConfig.setNoSync(true);

        return theEnv.beginTransaction(null, myConfig);
    }

    static Transaction newNonBlockingTxn() throws DatabaseException {
        TransactionConfig myConfig = new TransactionConfig();
        myConfig.setNoSync(true);
        myConfig.setNoWait(true);

        return theEnv.beginTransaction(null, myConfig);
    }

    public static void dumpLocks() {
        try {
            StatsConfig myConfig = new StatsConfig();
            myConfig.setFast(false);

            LockStats myStats = theEnv.getLockStats(myConfig);

            System.err.println("Locks");
            System.err.println("Owners:" + myStats.getNOwners());
            System.err.println("RdLock:" + myStats.getNReadLocks());
            System.err.println("WrLock:" + myStats.getNWriteLocks());
            System.err.println("Total locks:" + myStats.getNTotalLocks());
            System.err.println("Waiters:" + myStats.getNWaiters());
            
        } catch (DatabaseException anE) {
            System.err.println("Whoops couldn't dump stats");
        }
    }

    public static void dumpStats() {
        try {
            StatsConfig myConfig = new StatsConfig();
            myConfig.setFast(false);

            EnvironmentStats myStats = theEnv.getStats(myConfig);

            System.err.println(myStats);

            /*
            System.err.println("Log Buffer bytes: " +
                               myStats.getBufferBytes());

            System.err.println("Cache misses: " + myStats.getNCacheMiss());

            System.err.println("Cache data bytes" +
                               myStats.getCacheDataBytes());

            System.err.println("Cache total bytes" +
                               myStats.getCacheTotalBytes());
             */
        } catch (Exception anE) {
            WriteDaemon.theLogger.log(Level.INFO,
                                      "Couldn't dump stats", anE);
        }
    }

    public static void main(String args[]) {
        try {
            System.out.println("Disk storing at: " + Disk.getDbLocation());
            Disk.stop();
        } catch (Exception anE) {
            System.err.println("Got errors during test - see log");
        }
    }    
}
