package org.dancres.blitz.entry.ci;

import org.dancres.blitz.cache.Identifiable;
import org.dancres.blitz.entry.EntrySleeve;
import org.dancres.blitz.entry.TupleLocator;
import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.oid.OID;

import java.util.HashSet;
import java.util.Set;

/**
 * This indexer doesn't do any indexing at all - it merely maintains the set of all IDs currently in memory.
 * In situations where the live Entry set is small and fits into a small cache, this indexer is likely to be faster
 * by virtue of indexing being a tax on each write and take that outweighs the performance gained through faster
 * lookup.
 */
public class NullIndexer extends CacheIndexer {
    private String _type;
    private Set _all = new HashSet();

    NullIndexer(String aType) {
        _type = aType;
    }

    private void insert(EntrySleeve aSleeve) {
        synchronized(_all) {
            _all.add(aSleeve.getOID());
        }
    }

    private void remove(EntrySleeve aSleeve) {
        synchronized(_all) {
            _all.remove(aSleeve.getOID());
        }
    }

    public TupleLocator find(MangledEntry anEntry) {
        synchronized(_all) {
            if (_all.size() == 0)
                return ArrayLocatorImpl.EMPTY_LOCATOR;
            else {
                OID[] myUids = new OID[_all.size()];
                myUids = (OID[]) _all.toArray(myUids);

                return new ArrayLocatorImpl(myUids);
            }
        }
    }

    public void dirtied(Identifiable anIdentifiable) {
        EntrySleeve mySleeve = (EntrySleeve) anIdentifiable;

        if (mySleeve.isDeleted())
            remove(mySleeve);
    }

    public void loaded(Identifiable anIdentifiable) {
        EntrySleeve mySleeve = (EntrySleeve) anIdentifiable;

        if (mySleeve.isDeleted())
            return;

        insert(mySleeve);
    }

    public void flushed(Identifiable anIdentifiable) {
        EntrySleeve mySleeve = (EntrySleeve) anIdentifiable;

        remove(mySleeve);
    }
}
