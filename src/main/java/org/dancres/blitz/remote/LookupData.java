package org.dancres.blitz.remote;

import java.io.Serializable;
import java.io.IOException;

import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.core.entry.Entry;

import net.jini.core.discovery.LookupLocator;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;

import net.jini.core.lookup.ServiceID;

import net.jini.config.ConfigurationException;
import net.jini.config.Configuration;
import net.jini.config.NoSuchEntryException;

import net.jini.lookup.JoinManager;

import net.jini.discovery.DiscoveryManagement;
import net.jini.discovery.DiscoveryLocatorManagement;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.LookupDiscoveryManager;

import java.rmi.MarshalledObject;

import org.dancres.blitz.config.ConfigurationFactory;

import org.dancres.blitz.Logging;

/**
   All data related to join management is held in this class.
   Objects of this class are persisted to disk whenever their state
   is updated and used to restore join management information next time
   the service is run.  LookupStorage delegates most of it's method calls
   to this class.
*/
class LookupData implements Serializable {
    private static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.remote.LookupData");

    static final long serialVersionUID = -6636356018279188568L;

    private String[] theGroups;
    private MarshalledObject theAttributes;
    private LookupLocator[] theLocators;

    private Uuid theUuid;

    /*
      No need to save this to disk it's derived from theUuid
    */
    private transient ServiceID theServiceId;

    private transient Entry[] theStdAttrs;

    LookupData() {
    }

    /**
       Invoked the first time the service is exported.  The initial
       state is recovered from the Configuration file and then persisted
       to disk.  After that, the config file is only used to locate the
       saved state which is then used in preference to the config file
       for all join management related data.
    */
    void init(Configuration aConfig) throws ConfigurationException {
        initAttrs(aConfig);
        initGroups(aConfig);
        initLocators(aConfig);
        initUuid(aConfig);
    }

    void initAttrs(Configuration aConfig) throws ConfigurationException {
        Entry[] myUserAttrs = new Entry[0];

        try {
            myUserAttrs =
                (Entry[])
                    aConfig.getEntry(ConfigurationFactory.BLITZ_MODULE,
                        "initialAttrs",
                        Entry[].class);
        } catch (NoSuchEntryException aNSEE) {
        } catch (Throwable aT) {
            throw new ConfigurationException("Failed to load initAttrs",
                aT);
        }

        try {
            theAttributes = new MarshalledObject(myUserAttrs);
        } catch (IOException anIOE) {
            throw new ConfigurationException("Failed to store attributes",
                anIOE);
        }
    }

    void initGroups(Configuration aConfig) throws ConfigurationException {
        theGroups = (String[])
            aConfig.getEntry(ConfigurationFactory.BLITZ_MODULE,
                "initialGroups",
                String[].class,
                null);
    }

    void initLocators(Configuration aConfig) throws ConfigurationException {
        theLocators =
            (LookupLocator[])
                aConfig.getEntry(ConfigurationFactory.BLITZ_MODULE,
                    "initialLocators",
                    LookupLocator[].class,
                    new LookupLocator[0]);
    }

    void initUuid(Configuration aConfig) throws ConfigurationException {
        theUuid = UuidFactory.generate();
    }

    /**
       Update our state from the JoinManager instance passed in.
    */
    void update(JoinManager aMgr) throws IOException {
        theGroups = getDGM(aMgr).getGroups();
        theLocators = getDLM(aMgr).getLocators();
        theAttributes = new MarshalledObject(aMgr.getAttributes());
    }

    private boolean ignore(Entry anAttr) {
        for (int i = 0; i < theStdAttrs.length; i++) {
            if (anAttr.getClass().equals(theStdAttrs[i].getClass())) {
                return true;
            }
        }

        return false;
    }

    private DiscoveryGroupManagement getDGM(JoinManager aMgr) {
        return (DiscoveryGroupManagement) aMgr.getDiscoveryManager();
    }

    private DiscoveryLocatorManagement getDLM(JoinManager aMgr) {
        return (DiscoveryLocatorManagement) aMgr.getDiscoveryManager();
    }

    void dump() {
        theLogger.log(Level.INFO, "ServiceID: " + getServiceID());
        theLogger.log(Level.INFO, "Groups: ");
        if (theGroups == null)
            theLogger.log(Level.INFO, "All groups");
        else {
            for (int i = 0; i < theGroups.length; i++) {
                theLogger.log(Level.INFO, theGroups[i]);
            }
        }

        theLogger.log(Level.INFO, "Locators: ");
        if ((theLocators == null) || (theLocators.length == 0))
            theLogger.log(Level.INFO, "None");
        else {
            for (int i = 0; i < theLocators.length; i++) {
                theLogger.log(Level.INFO, theLocators[i].toString());
            }
        }
    }

    Entry[] getAttributes() throws IOException {
        try {
            if (theStdAttrs == null) {
                String myName = null;

                try {
                    myName =
                        (String)
                        ConfigurationFactory.getEntry("name", String.class);
                } catch (NoSuchEntryException aNSEE) {
                    // Doesn't matter
                }

                theStdAttrs = BlitzProxy.getDefaultAttrs(myName);
            }

            Entry[] myAttrs = (Entry[]) theAttributes.get();

            ArrayList mySaveable = new ArrayList();

            for (int i = 0; i < myAttrs.length; i++) {
                Entry myAttr = myAttrs[i];

                if (! ignore(myAttr))
                    mySaveable.add(myAttr);
            }

            myAttrs = new Entry[mySaveable.size()];

            myAttrs = (Entry[]) mySaveable.toArray(myAttrs);


            Entry[] myMergedAttrs = new Entry[myAttrs.length +
                                              theStdAttrs.length];

            System.arraycopy(theStdAttrs, 0, myMergedAttrs, 0,
                             theStdAttrs.length);
            System.arraycopy(myAttrs, 0, myMergedAttrs,
                             theStdAttrs.length, myAttrs.length);

            myAttrs = myMergedAttrs;
            
            return myAttrs;
        } catch (Exception anE) {
            IOException myIOE =
                new IOException("Failed to restore attributes");
            myIOE.initCause(anE);
            throw myIOE;
        }
    }

    DiscoveryManagement getLookupDiscovery()
        throws ConfigurationException {
        try {
            return new LookupDiscoveryManager(theGroups, theLocators,
                                              null);
        } catch (IOException anIOE) {
            BlitzServiceImpl.theLogger.log(Level.SEVERE,
                                           "Got IO problems from LDM",
                                           anIOE);
            throw new ConfigurationException("IO exception in LDM", anIOE);
        }
    }

    Uuid getUuid() {
        return theUuid;
    }

    synchronized ServiceID getServiceID() {
        if (theServiceId == null) {
            theServiceId =
                new ServiceID(theUuid.getMostSignificantBits(),
                              theUuid.getLeastSignificantBits());
        }

        return theServiceId;
    }
}
