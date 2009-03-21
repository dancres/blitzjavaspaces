package org.dancres.blitz.remote;

import java.io.File;
import java.io.Serializable;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import java.util.logging.Level;
import java.util.BitSet;

import net.jini.core.entry.Entry;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;

import net.jini.id.Uuid;

import net.jini.core.lookup.ServiceID;

import net.jini.discovery.DiscoveryManagement;

import net.jini.lookup.JoinManager;

import org.dancres.util.BytePacker;

import org.dancres.blitz.config.ConfigurationFactory;

import org.dancres.blitz.meta.Registry;
import org.dancres.blitz.meta.RegistryAccessor;
import org.dancres.blitz.meta.RegistryFactory;
import org.dancres.blitz.meta.Initializer;

import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.DiskTxn;
import org.dancres.blitz.disk.Syncable;

/**
   This class is responsible for managing JoinManagement state saving to
   and reloading from disk as required.

   @todo Need logging somewhere
 */
public class LookupStorage implements Syncable {
    public static final byte INIT_ATTRS = 1;
    public static final byte INIT_LOCATORS = 2;
    public static final byte INIT_GROUPS = 4;

    private static final String JOIN_STATE = "BlitzJoinState";

    private static byte[] JOIN_STATE_KEY = new byte[12];

    static {
        BytePacker myPacker = BytePacker.getMSBPacker(JOIN_STATE_KEY);
        myPacker.putInt(0, 1);
    }

    private LookupData theData;

    private Registry theRegistry;

    public LookupStorage() throws ConfigurationException {
    }

    /**
     * Load state from disk or create from scratch
     *
     * Warning: not thread-safe
     */
    public void init(Configuration aConfig) throws ConfigurationException {
        try {
            theRegistry =
                RegistryFactory.get(JOIN_STATE, new RegistryInit(aConfig));
            Disk.add(this);

            DiskTxn myTxn = DiskTxn.newTxn();

            theData = (LookupData)
                theRegistry.getAccessor().load(JOIN_STATE_KEY);

            myTxn.commit();

            // Display recovered settings for user
            theData.dump();
        } catch (Exception anE) {
            BlitzServiceImpl.theLogger.log(Level.SEVERE,
                "Exceptioned loading state",
                anE);
            throw new ConfigurationException("Failed to load join state",
                anE);
        }
    }

    /**
     * Recover previous state (if it's not already loaded) and then
     * overwrite selected settings.  Make sure to call saveState
     *
     * Warning: not thread-safe
     */
    public void reinit(Configuration aConfig, byte aFlags)
        throws ConfigurationException {

        if (theRegistry == null)
            init(aConfig);

        if ((aFlags & INIT_ATTRS) != 0) {
            theData.initAttrs(aConfig);
        }

        if ((aFlags & INIT_LOCATORS) != 0) {
            theData.initLocators(aConfig);
        }

        if ((aFlags & INIT_GROUPS) != 0) {
            theData.initGroups(aConfig);
        }
    }

    public void sync() throws Exception {
        // Always in sync
    }

    public void close() throws Exception {
        theRegistry.close();
    }

    /**
       Recover a DiscoveryManagement object pre-configured with settings.
     */
    DiscoveryManagement getDiscMgt() throws ConfigurationException {
        return theData.getLookupDiscovery();
    }

    /**
       Each JINI service has a unique (well, not quite) id
     */
    ServiceID getServiceID() {
        return theData.getServiceID();
    }

    Uuid getUuid() {
        return theData.getUuid();
    }

    /**
       Recover any descriptive attributes to be advertised as part of the
       registration.
     */
    Entry[] getAttributes() throws IOException {
        return theData.getAttributes();
    }

    synchronized void sync(JoinManager aMgr) throws IOException {
        theData.update(aMgr);
        saveState();
        theData.dump();
    }

    public synchronized void saveState() throws IOException {
        DiskTxn myTxn = DiskTxn.newTxn();
        
        theRegistry.getAccessor().save(JOIN_STATE_KEY, theData);

        myTxn.commit();
    }

    private class RegistryInit implements Initializer {
        private boolean wasExecuted;

        private Configuration theConfig;

        RegistryInit(Configuration aConfig) {
            theConfig = aConfig;
        }

        public void execute(RegistryAccessor anAccessor) throws IOException {

            LookupData myData = new LookupData();

            try {
                myData.init(theConfig);
            } catch (ConfigurationException aCE) {
                BlitzServiceImpl.theLogger.log(Level.SEVERE,
                                               "Exceptioned init'ing state",
                                               aCE);
                throw new IOException("Failed to init state");
            } catch (Throwable aT) {
                BlitzServiceImpl.theLogger.log(Level.SEVERE,
                                               "Exceptioned init'ing state",
                                               aT);
                throw new IOException("Failed to init state");
            }

            myData.dump();
            
            anAccessor.save(JOIN_STATE_KEY, myData);
        }
    }
}
