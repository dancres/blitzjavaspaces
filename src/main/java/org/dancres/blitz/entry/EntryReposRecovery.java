package org.dancres.blitz.entry;

import java.io.IOException;

import org.dancres.blitz.oid.OID;
import org.dancres.blitz.arc.CacheBlockDescriptor;
import org.dancres.blitz.arc.RecoverySummary;

/**
 * Used by <code>OpInfo</code> instances to manage Entry state
 */
public interface EntryReposRecovery extends EntryRepository {
    public CacheBlockDescriptor load(OID anOID) throws IOException;

    public void flush(CacheBlockDescriptor aCBD) throws IOException;

    public Counters getCounters();

    public RecoverySummary recover(EntrySleeveImpl aSleeve) throws IOException;
}
