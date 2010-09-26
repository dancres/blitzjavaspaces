package org.dancres.blitz.oid;

import java.io.Serializable;
import java.io.IOException;

import java.util.Random;
import java.util.ArrayList;

import java.util.logging.*;

import org.dancres.blitz.disk.DiskTxn;
import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.Syncable;

import org.dancres.blitz.meta.Registry;
import org.dancres.blitz.meta.RegistryAccessor;
import org.dancres.blitz.meta.RegistryFactory;
import org.dancres.blitz.meta.MetaIterator;
import org.dancres.blitz.meta.MetaEntry;
import org.dancres.blitz.meta.Initializer;

import org.dancres.blitz.Logging;
import org.dancres.blitz.BootContext;

import org.dancres.blitz.txn.UnsyncdOps;

/**
   <p>Provides an OID allocation service using upto n zones where n is
   the maximum number of zones as specified at construction time.</p>

   <p>AllocatorImpl's are checkpointed regularly as part of other checkpointing
   operations.  Thus, they can only be out of date by the maximum number of
   operations possible between checkpoints so, on reload, a corrective jump
   is applied determined by that maximum</p>
 */
class AllocatorImpl implements AllocatorAdmin, Syncable {
    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.oid.Allocator");

    private OIDAllocator[] theAllocators;
    private Random theAllocatorChooser = new Random();
    private int theMaxZones;

    private Registry theMetaData;
    private String theName;

    AllocatorImpl(String aName, int anAllocSpaceSize) throws IOException {
        theLogger.log(Level.INFO, "Allocator: " + aName);

        theMetaData =
            RegistryFactory.get(getDbNameFor(aName),
                                new OIDInitializer(anAllocSpaceSize));
        theName = aName;
        theMaxZones = anAllocSpaceSize;
        theAllocators = new OIDAllocator[theMaxZones];

        UnsyncdOps myBarrier = (UnsyncdOps)
            BootContext.get(UnsyncdOps.class);

        if (myBarrier != null) {
            theLogger.log(Level.INFO, theName + ":Resync'ing allocators: " +
                               myBarrier.getOpsSinceLastCheckpoint());
        }

        ArrayList myAllocators = loadAllocators();

        /*
           Make sure OIDAllocators go back into the correct slots within
           the array.  It's possible that they will have been read out of order
           from the underlying storage
         */
        long myMin = Long.MAX_VALUE;

        for (int i = 0; i < theMaxZones; i++) {
            OIDAllocator myAllocator = (OIDAllocator) myAllocators.get(i);
            theAllocators[myAllocator.getId()] = myAllocator;

            /*
              Do we need to resync the oid allocator?
            */
            if (myBarrier != null) {
                long myNext =
                    myAllocator.jump(myBarrier.getOpsSinceLastCheckpoint());

                if (myNext < myMin)
                    myMin = myNext;
            }
        }

        if (myBarrier != null) {
            try {
                theLogger.log(Level.INFO, "Minimum allocator oid for " +
                              theName + ": " + myMin);
                sync();
            } catch (Exception anE) {
                theLogger.log(Level.SEVERE, theName +
                              ":Failed to sync against barrier",
                              anE);
                throw new IOException("Failed to sync against barrier");
            }
        }

        Disk.add(this);
    }

    private ArrayList loadAllocators() throws IOException  {
        ArrayList myAllocators = new ArrayList();

        DiskTxn myStandalone = DiskTxn.newStandalone();

        MetaIterator myAllocatorStates =
            theMetaData.getAccessor(myStandalone).readAll();

        MetaEntry myEntry;

        while ((myEntry = myAllocatorStates.fetch()) != null) {
            OIDAllocator myAllocator = new OIDAllocator();
            myAllocator.setState(myEntry.getData());
            myAllocators.add(myAllocator);
        }

        myAllocatorStates.release();
        myStandalone.commit();

        return myAllocators;
    }

    /**
       Call this within an active DiskTxn
     */
    public OID getNextId() throws IOException {
        OIDAllocator myAllocator =
            theAllocators[theAllocatorChooser.nextInt(theMaxZones)];

        OID myID = null;

        synchronized(myAllocator) {
            myID = myAllocator.newOID();
        }

        return myID;
    }

    public int getMaxZoneId() {
        return theMaxZones;
    }

    public void sync() throws Exception {
        DiskTxn myTxn = DiskTxn.newStandalone();

        RegistryAccessor myAccessor = theMetaData.getAccessor(myTxn);

        for (int i = 0; i < theAllocators.length; i++) {
            OIDAllocator myAlloc = theAllocators[i];

            synchronized(myAlloc) {
                myAccessor.save(myAlloc.getKey(), myAlloc.getState());
            }
        }

        myTxn.commit(true);
    }

    public void close() throws Exception {
        theMetaData.close();
    }

    public void delete() throws IOException {
        theMetaData.close();

        RegistryFactory.delete(getDbNameFor(theName));

        discard();
    }

    public void discard() {
        Disk.remove(this);
    }

    private String getDbNameFor(String aName) {
        return aName + "_oid";
    }

    private class OIDInitializer implements Initializer {
        private int theNumAllocators;

        OIDInitializer(int aNumAllocators) {
            theNumAllocators = aNumAllocators;
        }

        public void execute(RegistryAccessor anAccessor) throws IOException {
            for (int i = 0; i < theNumAllocators; i++) {
                OIDAllocator myAlloc = new OIDAllocator(i, theNumAllocators);
                anAccessor.save(myAlloc.getKey(), myAlloc.getState());
            }
        }
    }
}
