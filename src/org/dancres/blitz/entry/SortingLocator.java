package org.dancres.blitz.entry;

import java.io.IOException;

import org.dancres.blitz.oid.OID;

/**
   This locator will provide a single FIFO list of tuples created from
   merging the contents of all the passed locators.  It assumes that each
   individual locator is FIFO sorted.
 */
class SortingLocator implements TupleLocator {
    private TupleLocator[] theLocators;

    private OID[] theSortBuffer;

    private OID theNext;

    SortingLocator(TupleLocator[] aSetOfLocators) {
        theLocators = aSetOfLocators;
    }

    /**
       Invoke this to load the next matching Tuple.

       @return <code>true</code> if there was a tuple, <code>false</code>
       otherwise
     */
    public boolean fetchNext() throws IOException {
        if (theSortBuffer == null) {
            // First-time init
            theSortBuffer = new OID[theLocators.length];

            for (int i = 0; i < theLocators.length; i++) {
                boolean hasUID = theLocators[i].fetchNext();

                if (hasUID)
                    theSortBuffer[i] = theLocators[i].getOID();
            }
        } else {
            // This cannot be the first time so if theNext is null, we've
            // run out of matches
            if (theNext == null)
                return false;

            // Refresh slots in the slot buffer which contain the last OID
            for (int i = 0; i < theSortBuffer.length; i++) {
                if (theSortBuffer[i] != null) {
                    if (theSortBuffer[i].equals(theNext)) {
                        if (theLocators[i].fetchNext())
                            theSortBuffer[i] = theLocators[i].getOID();
                        else
                            theSortBuffer[i] = null;
                    }
                }
            }
        }

        OID myOldest = null;

        for (int i = 0; i < theSortBuffer.length; i++) {
            if (theSortBuffer[i] != null) {
                if (myOldest == null) {
                    myOldest = theSortBuffer[i];
                } else {
                    if (theSortBuffer[i].compareTo(myOldest) < 0) {
                        myOldest = theSortBuffer[i];
                    }
                }
            }
        }

        theNext = myOldest;

        return (theNext != null);
    }

    /**
       @return the OID of the tuple just fetched with
       <code>fetchNext</code>
     */
    public OID getOID() {
        return theNext;
    }

    /**
       When you've finished with the TupleLocator instance, call release.
     */
    public void release() throws IOException {
        for (int i = 0; i < theLocators.length; i++) {
            theLocators[i].release();
        }
    }
}