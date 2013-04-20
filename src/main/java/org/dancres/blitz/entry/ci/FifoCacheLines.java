package org.dancres.blitz.entry.ci;

import java.util.ArrayList;

import com.go.trove.util.IntHashMap;

import org.dancres.blitz.entry.TupleLocator;
import org.dancres.blitz.entry.EntrySleeve;

/**
   Maintains a set of cache lines (key'd by hashcode) for a particular
   field of one type of Entry.
 */
class FifoCacheLines implements CacheLines {
    private IntHashMap theHashCodes = new IntHashMap();
    private int theOffset;
    private String theName;

    FifoCacheLines(int anOffset, String aName) {
        theOffset = anOffset;
        theName = aName;
    }

    public String getName() {
        return theName;
    }

    public TupleLocator getIds(int aHashcode) {
        FifoCacheLine myEntries = getLine(aHashcode, false);

        if (myEntries == null)
            return ArrayLocatorImpl.EMPTY_LOCATOR;

        synchronized(myEntries) {
            return myEntries.getLocator();
        }
    }

    /**
       @return the number of entries under a particular hashcode
    */
    public int getSize(int aHashcode) {
        FifoCacheLine myEntries = getLine(aHashcode, false);

        if (myEntries == null)
            return 0;

        synchronized(myEntries) {
            return myEntries.getSize();
        }
    }

    /**
       @return the number of different hashcodes we know about
    */
    public int getSize() {
        synchronized(theHashCodes) {
            return theHashCodes.size();
        }
    }

    public void insert(EntrySleeve aSleeve) {
        int myKey = getKey(aSleeve);

        synchronized(theHashCodes) {
            FifoCacheLine myEntries = getLine(myKey, true);

            synchronized (myEntries) {
                myEntries.insert(aSleeve);
            }
        }
    }

    public void remove(EntrySleeve aSleeve) {
        int myKey = getKey(aSleeve);

        synchronized(theHashCodes) {
            FifoCacheLine myEntries = getLine(myKey, false);

            synchronized(myEntries) {
                myEntries.remove(aSleeve);
            }

            if (myEntries.getSize() == 0)
                theHashCodes.remove(myKey);
        }
    }

    private int getKey(EntrySleeve aSleeve) {
        int myHash = aSleeve.getHashCodeForField(theOffset);

        return myHash;
    }

    private FifoCacheLine getLine(int aKey, boolean doCreate) {
        FifoCacheLine myEntries;

        synchronized(theHashCodes) {
            myEntries = (FifoCacheLine) theHashCodes.get(aKey);

            if ((myEntries == null) && doCreate) {
                myEntries = new FifoCacheLine();
                theHashCodes.put(aKey, myEntries);
            }
        }

        return myEntries;
    }
}
