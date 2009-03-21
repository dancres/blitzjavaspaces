package org.dancres.blitz.entry.ci;

import java.util.*;

import java.util.logging.*;

import org.dancres.blitz.entry.EntrySleeve;
import org.dancres.blitz.entry.TupleLocator;

import org.dancres.blitz.mangler.MangledField;
import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.cache.Cache;
import org.dancres.blitz.cache.CacheListener;
import org.dancres.blitz.cache.Identifiable;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.Logging;

/**
   <p>This CacheIndexer enforces ordering of its UIDs such that searches
   favour the smallest UID's first.  There is a corresponding policy in
   UID allocation which must be adopted for Fifo guarentees which is to ensure
   that we allocate UID's serially from smallest to largets.</p>

   <p>Finally, we have to ensure that all searches favour disk then dirty
   cache then Entry cache because the oldest Entry's are more likely to be
   on disk than in the cache.</p>

   @see org.dancres.blitz.oid.AllocatorFactory
   @see org.dancres.blitz.entry.SleeveCache
*/
public class FifoIndexer extends CacheIndexerImpl {

    FifoIndexer(String aType) {
        super(aType);
    }

    Set newIds() {
        /*
        In order to ensure FIFO ordering of searches we must use some
        form of SortedSet thus we use TreeSet which guarentees us that
        iteration and toArray etc give us the ordering we require so long as
        the OIDs are allocated appropriately.
        */
        return new TreeSet();
    }

    CacheLines[] newLinesArray(int aSize) {
        return new FifoCacheLines[aSize];
    }

    CacheLines newLines(int anIndex, String aFieldName) {
        return new FifoCacheLines(anIndex, aFieldName);
    }
}
