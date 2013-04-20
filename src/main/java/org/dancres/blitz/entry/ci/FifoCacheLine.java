package org.dancres.blitz.entry.ci;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.entry.TupleLocator;
import org.dancres.blitz.entry.EntrySleeve;

/**
   Maintains a list of all entries with the same hashcode for a particular
   field.
 */
class FifoCacheLine {
    private Set theIds = new TreeSet();

    TupleLocator getLocator() {
        if (theIds.size() == 0)
            return ArrayLocatorImpl.EMPTY_LOCATOR;
        else
            return new FifoCacheLineLocatorImpl(getIdList(), this);
    }

    private Object[] getIdList() {
        Object[] myIds = new Object[theIds.size()];
        theIds.toArray(myIds);

        return myIds;
    }

    void insert(EntrySleeve aSleeve) {
        theIds.add(aSleeve.getOID());
    }

    void remove(EntrySleeve aSleeve) {
        theIds.remove(aSleeve.getOID());
    }

    int getSize() {
        return theIds.size();
    }
}
