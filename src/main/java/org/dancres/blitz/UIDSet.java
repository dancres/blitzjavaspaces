package org.dancres.blitz;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * A set of UID's that is guarenteed to contain no duplicates and limits it's
 * size to the specified capacity
 */
public class UIDSet {
    private Set theFilter = new HashSet();
    private ArrayList theMembers = new ArrayList();
    private long theCapacity;

    UIDSet(long aMaxSize) {
        theCapacity = aMaxSize;
    }

    void add(SpaceEntryUID aUID) {
        synchronized(theFilter) {
            if (isFull())
                return;

            if (! theFilter.contains(aUID)) {
                theFilter.add(aUID);
                theMembers.add(aUID);
            }
        }
    }

    boolean isFull() {
        synchronized(theFilter) {
            return (theFilter.size() == theCapacity);
        }
    }

    SpaceEntryUID pop() {
        synchronized(theFilter) {
            if (theMembers.size() == 0)
                return null;
            else
                return (SpaceEntryUID) theMembers.remove(theMembers.size() - 1);
        }
    }
}
