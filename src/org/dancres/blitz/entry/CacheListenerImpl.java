package org.dancres.blitz.entry;

import org.dancres.blitz.cache.CacheListener;
import org.dancres.blitz.cache.Identifiable;
import org.dancres.blitz.entry.ci.CacheIndexer;

/**
 * Sits between an ArcCache and the CacheIndexer.
 */
public class CacheListenerImpl implements CacheListener {
    private CacheIndexer _indexer;

    CacheListenerImpl(CacheIndexer anIndexer) {
        _indexer = anIndexer;
    }

    public void dirtied(Identifiable anIdentifiable) {
    }

    public void loaded(Identifiable anIdentifiable) {
        _indexer.loaded(anIdentifiable);
    }

    public void flushed(Identifiable anIdentifiable) {
        /*
            We say nothing - done in WriteScheduler and WriteBuffer
            CacheIndexer tracks newly written entry's not yet on disk.  WriteScheduler
            and WriteBuffer are the objects which know when an entry has made it to disk
            hence we leave it to them to notify the CacheIndexer appropriately.
          */
    }
}
