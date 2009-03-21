package org.dancres.blitz.entry.ci;

import java.io.IOException;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.entry.TupleLocator;

import org.dancres.struct.BitIndex;
import org.dancres.struct.BitVisitor;

/**
   Used by CacheIndexer.
 */
class BitLocatorImpl implements TupleLocator {
    private BitVisitor theHits;
    private OID[] theOIDs;
    private int theIndex = 0;
    private OID theCurrent;

    BitLocatorImpl(BitIndex aHits, OID[] aListOfOIDs, boolean hitOnZero) {
        theOIDs = aListOfOIDs;
        theHits = aHits.getVisitor(hitOnZero);
    }

    /**
       Invoke this to load the next matching Tuple.

       @return <code>true</code> if there was a tuple, <code>false</code>
       otherwise
     */
    public boolean fetchNext() throws IOException {
        int myNext;

        while ((myNext = theHits.getNext()) != -1) {
            theCurrent = (OID) theOIDs[myNext];

            if (theCurrent != null)
                return true;
            else {
                /*
                  System.err.println("There should've been a OID here: " +
                  myNext);
                */
            }
        }

        return false;
    }

    /**
       @return the OID of the tuple just fetched with
       <code>fetchNext</code>
     */
    public OID getOID() {
        return theCurrent;
    }

    /**
       When you've finished with the TupleLocator instance, call release.
     */
    public void release() throws IOException {
        // System.out.println("Iterated: " + theIndex);
    }
}
