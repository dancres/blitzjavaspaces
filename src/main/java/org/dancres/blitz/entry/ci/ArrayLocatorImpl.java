package org.dancres.blitz.entry.ci;

import java.io.IOException;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.entry.TupleLocator;

/**
   Used by CacheIndexer.
 */
public class ArrayLocatorImpl implements TupleLocator {
    private Object[] theOIDs;
    private int theIndex = 0;
    private OID theCurrent;

    public static TupleLocator EMPTY_LOCATOR =
        new ArrayLocatorImpl(new OID[0]);

    ArrayLocatorImpl(Object[] aListOfOIDs) {
        theOIDs = aListOfOIDs;
    }

    /**
       Invoke this to load the next matching Tuple.

       @return <code>true</code> if there was a tuple, <code>false</code>
       otherwise
     */
    public boolean fetchNext() throws IOException {
        if (theIndex >= theOIDs.length)
            return false;

        theCurrent = (OID) theOIDs[theIndex++];
        return true;
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