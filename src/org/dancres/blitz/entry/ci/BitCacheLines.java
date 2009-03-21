package org.dancres.blitz.entry.ci;

import com.go.trove.util.IntHashMap;

import org.dancres.blitz.entry.EntrySleeve;

import org.dancres.struct.BitIndex;

/**
   Maintains a set of cache lines (key'd by hashcode) for a particular
   field of one type of Entry.
 */
class BitCacheLines {
    private IntHashMap theHashCodes = new IntHashMap();
    private int theOffset;
    private String theName;

    private int theSize;

    BitCacheLines(int anOffset, String aName, int aSize) {
        theOffset = anOffset;
        theName = aName;
        theSize = aSize;
    }

    String getName() {
        return theName;
    }

    BitIndex getHits(int aHashcode) {
        BitCacheLine myEntries = getLine(aHashcode, false);

        if (myEntries == null)
            return null;

        return myEntries.getSlots();
    }

    /**
       @return the number of entries under a particular hashcode
    */
    int getSize(int aHashcode) {
        BitCacheLine myEntries = getLine(aHashcode, false);

        if (myEntries == null)
            return 0;

        return myEntries.getSize();
    }

    /**
       @return the number of different hashcodes we know about
    */
    int getSize() {
        synchronized(theHashCodes) {
            return theHashCodes.size();
        }
    }

    void insert(EntrySleeve aSleeve, int aSlot) {
        int myKey = getKey(aSleeve);

        synchronized(theHashCodes) {
            BitCacheLine myEntries = getLine(myKey, true);
            myEntries.insert(aSleeve, aSlot);
        }
    }

    void remove(EntrySleeve aSleeve, int aSlot) {
        int myKey = getKey(aSleeve);

        synchronized (theHashCodes) {
            BitCacheLine myEntries = getLine(myKey, false);

            myEntries.remove(aSleeve, aSlot);

            if (myEntries.getSize() == 0)
                theHashCodes.remove(myKey);
        }
    }

    private int getKey(EntrySleeve aSleeve) {
        int myHash = aSleeve.getHashCodeForField(theOffset);

        return myHash;
    }

    private synchronized BitCacheLine getLine(int aKey, boolean doCreate) {
        BitCacheLine myEntries;

        myEntries = (BitCacheLine) theHashCodes.get(aKey);

        if ((myEntries == null) && doCreate) {
            myEntries = new BitCacheLine(theSize);
            theHashCodes.put(aKey, myEntries);
        }

        return myEntries;
    }
}
