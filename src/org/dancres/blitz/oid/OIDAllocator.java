package org.dancres.blitz.oid;

import java.io.Serializable;

import java.util.HashMap;

import org.dancres.util.BytePacker;

/**
   Allocation of OID's is done by an OIDAllocator.  Each OIDAllocator has it's
   own unique identifier (a zone id) so they may be "ganged together" for the
   purposes of ensuring a large enough pool of available OIDs.  This unique
   identifier forms part of the underlying OID.
 */
class OIDAllocator {
    private int theZoneId;
    private long theNextOid = 1;
    private int theSeed;

    OIDAllocator() {
    }

    /**
       Use for FIFO only
     */
    OIDAllocator(int anId) {
        theZoneId = anId;
        theSeed = anId;
    }

    OIDAllocator(int anId, int aMaxId) {
        theZoneId = anId;
        theSeed = (Integer.MAX_VALUE / aMaxId) * anId;
    }

    int getId() {
        return theZoneId;
    }

    int getSeed() {
        return theSeed;
    }

    byte[] getKey() {
        byte[] myKey = new byte[4];
        BytePacker myPacker = BytePacker.getMSBPacker(myKey);

        myPacker.putInt(theZoneId, 0);

        return myKey;
    }

    /**
       Take a copy of current state to save into persistent storage
     */
    Serializable getState() {
        return new OIDAllocatorState(theZoneId, theNextOid, theSeed);
    }

    /**
       Restore state from a copy recovered from persistent storage
     */
    void setState(Serializable aState) {
        OIDAllocatorState myState = (OIDAllocatorState) aState;

        theZoneId = myState.getId();
        theNextOid = myState.getCurrentOid();
        theSeed = myState.getSeed();
    }

    /**
       Only to be called during recovery - update state based on BootContext
     */
    long jump(long aJump) {
        theNextOid += aJump;
        return theNextOid;
    }

    OID newOID() {
        return OIDFactory.newOID(theSeed, theNextOid++);
    }

    boolean isExhausted() {
        return (theNextOid < 0);
    }

    /**
       Check for collisions between a range of allocators
     */
    public static void main(String args[]) {
        int myNum = Integer.parseInt(args[0]);

        HashMap mySeeds = new HashMap();

        for (int i = 0; i < myNum; i++) {
            OIDAllocator myAlloc = new OIDAllocator(i, myNum);

            if (mySeeds.get(new Integer(myAlloc.getSeed())) != null)
                System.err.println("Collision: " + myAlloc.getSeed());
            else {
                System.err.println(myAlloc.getSeed());
                mySeeds.put(new Integer(myAlloc.getSeed()), myAlloc);
            }
        }
    }

    private static class OIDAllocatorState implements Serializable {
        private int theZoneId;
        private int theSeed;
        private long theCurrentOid;
 
        OIDAllocatorState(int anId, long aCurrentOid, int aSeed) {
            theZoneId = anId;
            theCurrentOid = aCurrentOid;
            theSeed = aSeed;
        }
 
        int getSeed() {
            return theSeed;
        }

        int getId() {
            return theZoneId;
        }
 
        long getCurrentOid() {
            return theCurrentOid;
        }
    }
}
