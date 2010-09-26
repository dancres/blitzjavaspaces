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
   <p>Provides an OID allocation service using a single zone at a time.
   The early OID's are smaller numerically than later OID's such that the
   user of these OID's can apply an ordering which yields FIFO behaviour.</p>
 */
class FIFOAllocatorImpl implements AllocatorAdmin, Syncable {
    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.oid.Allocator");

    private static final byte[] ALLOCATOR_KEY =
        new byte[] {0x00, 0x00, 0x00, 0x01};

    private static int MAX_ALLOCATOR = 512;

    private OIDAllocator theAllocator;

    private Registry theMetaData;
    private String theName;

    FIFOAllocatorImpl(String aName) throws IOException {
        theLogger.log(Level.INFO, "FIFOAllocator: " + aName);

        theMetaData =
            RegistryFactory.get(getDbNameFor(aName),
                                new OIDInitializer());
        theName = aName;

        UnsyncdOps myBarrier = (UnsyncdOps)
            BootContext.get(UnsyncdOps.class);

        if (myBarrier != null) {
            theLogger.log(Level.INFO, theName + ":Resync'ing allocator: " +
                               myBarrier.getOpsSinceLastCheckpoint());
        }

        loadAllocator();

        if (myBarrier != null) {
            theAllocator.jump(myBarrier.getOpsSinceLastCheckpoint());

            if (theAllocator.isExhausted()) {
                if (theAllocator.getId() == (MAX_ALLOCATOR - 1))
                    throw new Error("FIFOAllocator was exhausted - VERY BAD!");

                theAllocator = new OIDAllocator(theAllocator.getId() + 1,
                                                theAllocator.getId() + 1);
                theAllocator.jump(myBarrier.getOpsSinceLastCheckpoint());
            }

            try {
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

    private void loadAllocator() throws IOException  {
        DiskTxn myStandalone = DiskTxn.newStandalone();

        MetaIterator myAllocatorStates =
            theMetaData.getAccessor(myStandalone).readAll();

        Serializable myEntry =
            theMetaData.getAccessor(myStandalone).load(ALLOCATOR_KEY);

        OIDAllocator myAllocator = new OIDAllocator();
        myAllocator.setState(myEntry);

        theAllocator = myAllocator;
        myStandalone.commit();
    }

    /**
       Call this within an active DiskTxn
     */
    public OID getNextId() throws IOException {
        synchronized(this) {

            OID myID = null;

            if (theAllocator.isExhausted()) {
                if (theAllocator.getId() == (MAX_ALLOCATOR - 1))
                    throw new Error("FIFOAllocator was exhausted - VERY BAD!");

                theAllocator = new OIDAllocator(theAllocator.getId() + 1,
                                                theAllocator.getId() + 1);
            }

            myID = theAllocator.newOID();

            return myID;
        }
    }

    public int getMaxZoneId() {
        /*
          HACK:  We put up an arbitary limit, if we ever exceed this
          we'll break LeaseTrackerImpl in blitz.entry
         */
        return MAX_ALLOCATOR;
    }

    public void sync() throws Exception {
        DiskTxn myTxn = DiskTxn.newStandalone();

        RegistryAccessor myAccessor = theMetaData.getAccessor(myTxn);

        synchronized(this) {
            myAccessor.save(ALLOCATOR_KEY, theAllocator.getState());
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
        OIDInitializer() {
        }

        public void execute(RegistryAccessor anAccessor) throws IOException {
            OIDAllocator myAlloc = new OIDAllocator(0);
            anAccessor.save(ALLOCATOR_KEY, myAlloc.getState());
        }
    }
}
