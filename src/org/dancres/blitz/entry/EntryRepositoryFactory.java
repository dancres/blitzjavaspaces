package org.dancres.blitz.entry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import java.util.logging.*;

import net.jini.config.ConfigurationFile;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;

import org.dancres.blitz.Lifecycle;
import org.dancres.blitz.LifecycleRegistry;
import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.Syncable;

import org.dancres.blitz.config.ConfigurationFactory;

import org.dancres.blitz.Logging;

import org.dancres.blitz.lease.LeaseReaper;

import org.dancres.blitz.txn.TxnManager;

import org.prevayler.SnapshotContributor;

public class EntryRepositoryFactory implements Syncable, SnapshotContributor {
    private static final String LOG_COUNTS = "logCounts";

    private static LeaseReaper theReaper;

    private static final Logger theLogger =
        Logging.newLogger("org.dancres.blitz.entry.EntryRepositoryFactory");

    private static EntryRepositoryFactory theReposFactory;

    private Map theRepositories = new HashMap();

    private boolean logCountOnBoot;
    private boolean haveRegisteredForSnapshot;
    private Object theLogLock = new Object();

    private static class LifecycleImpl implements Lifecycle {
        public void init() {
            theReposFactory = new EntryRepositoryFactory();
        }

        public void deinit() {
            theReposFactory.discard();
            theReposFactory = null;
        }
    }

    static {
        LifecycleRegistry.add(new LifecycleImpl());
    }

    private EntryRepositoryFactory() {
        try {
            long myReapTime =
                ((Long)
                 ConfigurationFactory.getEntry("leaseReapInterval",
                                               long.class,
                                               new Long(0))).longValue();

            InputStream myStream = 
                getClass().getResourceAsStream("filters.properties");

            if (myStream == null)
                throw new RuntimeException("Reap filters are missing!");
            else {
                Configuration myConfig =
                    new ConfigurationFile(new InputStreamReader(myStream),
                                          null);

                theReaper = new LeaseReaper(myConfig, myReapTime);
            }

        } catch (Exception anE) {
            theLogger.log(Level.SEVERE, "Failed to init reaper", anE);
            throw new RuntimeException("Critical failure starting reaper");
        }

        /*
          Figure out if instance counts should be put into checkpoints and
          log files.  Note we don't actually do anymore work here.  The
          remainder is done using barrier methods in this class and in
          EntryReposImpl.

          EntryReposImpl is responsible for trapping "first use" and emitting
          a record into the log.

          EntryRepositoryFactory is responsible for placing similar records
          into the checkpoint snapshot.
        */
        try {
            logCountOnBoot =
                ((Boolean)
                 ConfigurationFactory.getEntry(LOG_COUNTS,
                                               Boolean.class,
                                               new Boolean(false))).booleanValue();

        } catch (ConfigurationException aCE) {
            theLogger.log(Level.SEVERE,
                          "Failed to load logInstanceCounts setting", aCE);
        }

        Disk.add(this);

        haveRegisteredForSnapshot = false;
    }

    private void discard() {
        theReaper = null;
        Disk.remove(this);
    }

    public static void reap() {
        theReaper.reap();
    }

    public static EntryRepositoryFactory get() {
        /*
          Anyone wishing to access the factory's functions must go through
          this method first so it's the ideal place to barrier for registering
          with TxnManager to supply instance count records to checkpoints.
         */
        theReposFactory.registerBarrier();

        return theReposFactory;
    }

    /*
      Unfortunately, we may be instantiated during recovery at which point
      the transaction manager is not available so we must stave off
      registration of our SnapshotContributor until recovery is completed.
     */
    private void registerBarrier() {
        synchronized(theLogLock) {
            if (!logCountOnBoot || TxnManager.get().isRecovery())
                return;
            else if (!haveRegisteredForSnapshot) {
            /*
              If we have to log counts at boot we also need to log them at
              checkpoints - register SnapshotContributor
             */
                TxnManager.get().add(this);
                haveRegisteredForSnapshot = true;
            }
        }
    }

    public Serializable getContribution() {
        EntryReposImpl[] myRepos = getRepositoriesSnapshot();

        InstanceCheckpoint myContribution = new InstanceCheckpoint();

        for (int i = 0; i < myRepos.length; i++) {
            myContribution.add(myRepos[i].getType(),
                               myRepos[i].getTotalLiveEntries());
        }

        return myContribution;
    }

    public boolean isDebugLogging() {
        return logCountOnBoot;
    }

    /**
       Locate the EntryRepository instance for the specified type, creating
       a new, empty one, if it's not already present
     */
    public synchronized EntryRepository get(String aType) 
        throws IOException {
        EntryReposImpl myRepos = (EntryReposImpl) theRepositories.get(aType);

        if (myRepos == null) {
            Storage myStore = StorageFactory.getStorage(aType);
            myStore.init(false);

            myRepos = new EntryReposImpl(myStore);
            theRepositories.put(aType, myRepos);
        }

        return myRepos;
    }

    /**
       Locate the EntryRepository instance for the specified type.
       If it doesn't already exist, return <code>null</code>.
     */
    public synchronized EntryRepository find(String aType)
        throws IOException {

        EntryReposImpl myRepos = (EntryReposImpl) theRepositories.get(aType);

        if (myRepos == null) {
            Storage myStore = StorageFactory.getStorage(aType);

            if (myStore.init(true)) {
                myRepos = new EntryReposImpl(myStore);
                theRepositories.put(aType, myRepos);
            }
        }

        return myRepos;
    }

    synchronized EntryReposRecovery getAdmin(String aType)
        throws IOException {
        return (EntryReposRecovery) get(aType);
    }

    public void sync() throws Exception {
        EntryReposImpl[] myRepositories = getRepositoriesSnapshot();

        for (int i = 0; i < myRepositories.length; i++) {
            myRepositories[i].sync();
        }
    }

    public void close() throws Exception {
        EntryReposImpl[] myRepositories = getRepositoriesSnapshot();

        for (int i = 0; i < myRepositories.length; i++) {
            myRepositories[i].close();
        }
    }

    private EntryReposImpl[] getRepositoriesSnapshot() {
        synchronized(this) {
            Collection myRepositories = theRepositories.values();

            EntryReposImpl[] myResult =
                new EntryReposImpl[myRepositories.size()];

            return (EntryReposImpl[]) myRepositories.toArray(myResult);
        }
    }

    public void deleteAllEntrys() throws IOException {
        EntryReposImpl[] myRepos = getRepositoriesSnapshot();

        for (int i = 0; i < myRepos.length; i++) {
            myRepos[i].deleteAllEntrys();
        }
    }

    public void deleteAllRepos() throws IOException {
        // For each repository, close them and invoke delete
        synchronized(this) {
            EntryReposImpl[] myRepos = getRepositoriesSnapshot();
            
            for (int i = 0; i < myRepos.length; i++) {
                myRepos[i].close();
                myRepos[i].delete();
            }

            theRepositories.clear();
        }
    }

    static LeaseReaper getReaper() {
        return theReaper;
    }
}
