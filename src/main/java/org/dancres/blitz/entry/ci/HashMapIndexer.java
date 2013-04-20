package org.dancres.blitz.entry.ci;

import java.util.HashSet;
import java.util.Set;

import java.util.logging.*;

import org.dancres.blitz.entry.EntrySleeve;
import org.dancres.blitz.entry.TupleLocator;

import org.dancres.blitz.mangler.MangledField;
import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.cache.Cache;
import org.dancres.blitz.cache.CacheListener;
import org.dancres.blitz.cache.Identifiable;

import org.dancres.blitz.oid.OID;

/**
   Basic simple cache indexer that uses hashmaps
*/
public class HashMapIndexer extends CacheIndexerImpl {

    HashMapIndexer(String aType) {
        super(aType);
    }

    Set newIds() {
        return new HashSet();
    }

    CacheLines[] newLinesArray(int aSize) {
        return new SimpleCacheLines[aSize];
    }

    CacheLines newLines(int anIndex, String aFieldName) {
        return new SimpleCacheLines(anIndex, aFieldName);
    }

}
