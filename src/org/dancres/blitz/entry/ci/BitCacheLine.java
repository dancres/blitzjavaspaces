package org.dancres.blitz.entry.ci;

import java.util.Set;
import java.util.HashSet;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.dancres.blitz.entry.TupleLocator;
import org.dancres.blitz.entry.EntrySleeve;

import org.dancres.struct.BitIndex;

/**
   Maintains a list of all entries with the same hashcode for a particular
   field.
 */
class BitCacheLine {
    private BitIndex theIndex;

    BitCacheLine(int aSize) {
        theIndex = new BitIndex(aSize);
    }

    BitIndex getSlots() {
        // return theIndex.copy();
        /*
            We can afford to return the live reference because anything new
            will be considered a recent write and caught elsewhere.

            Anything that moves out of cache and thus we miss will be moving
            down the cache hierarchy which we will also search.  Ultimately
            in the worst case we'll find the match on disk.  Note that
            WriteBuffer will not flush it's dirty cache (which we search)
            until the entry has been forced to disk.  This ensures that we
            cannot chase the Entry through all the caches and ultimately miss
            it.
        */
        return theIndex;
    }

    void insert(EntrySleeve aSleeve, int aSlot) {
        theIndex.set(aSlot);
    }

    void remove(EntrySleeve aSleeve, int aSlot) {
        theIndex.clear(aSlot);
    }

    int getSize() {
        return theIndex.count();
    }
}
