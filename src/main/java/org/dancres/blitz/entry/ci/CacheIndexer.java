package org.dancres.blitz.entry.ci;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.HashMap;

import net.jini.config.ConfigurationException;

import org.dancres.blitz.Lifecycle;
import org.dancres.blitz.LifecycleRegistry;
import org.dancres.blitz.config.NoIndex;
import org.dancres.blitz.entry.TupleLocator;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.oid.OID;

import org.dancres.blitz.cache.Cache;
import org.dancres.blitz.cache.CacheListener;
import org.dancres.blitz.cache.Identifiable;

import org.dancres.blitz.Logging;

import org.dancres.blitz.config.ConfigurationFactory;
import org.dancres.blitz.config.EntryConstraints;
import org.dancres.blitz.config.Fifo;

/**
   CacheIndexer is responsible for indexing EntrySleeves held in any
   cache capable of support CacheListener instances.  It supports searching
   as per other forms of storage via a TupleLocator::find mechanism.

   @todo Maybe use some other kind of list rather than ArrayList which, for
   large sizes, may not be so good.
*/
public abstract class CacheIndexer {
    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.disk.CacheIndexer");

    static class LifecycleImpl implements Lifecycle {
        public void init() {
        }

        public void deinit() {
            _indexers.clear();
        }
    }

    static {
        LifecycleRegistry.add(new LifecycleImpl());
    }

    private static HashMap _indexers = new HashMap();

    public static CacheIndexer getIndexer(String aType) {
        synchronized(_indexers) {
            return (CacheIndexer) _indexers.get(aType);
        }
    }

    public static CacheIndexer newIndexer(String aType,
                                          EntryConstraints aConstraints) {

        synchronized(_indexers) {
            CacheIndexer myIndexer;

            if (aConstraints.get(Fifo.class) != null) {
                theLogger.log(Level.INFO, "Using FIFO indexer: " + aType);
                myIndexer = new FifoIndexer(aType);
            } else if (aConstraints.get(NoIndex.class) != null) {
                myIndexer = new NullIndexer(aType);
                theLogger.log(Level.INFO, "Using NULL indexer: " + aType);
            } else {
                theLogger.log(Level.INFO, "Using HASHMAP indexer: " + aType);
                myIndexer = new HashMapIndexer(aType);
            }

            _indexers.put(aType, myIndexer);

            return myIndexer;
        }
    }

    public abstract TupleLocator find(MangledEntry anEntry);

    public abstract void dirtied(Identifiable anIdentifiable);

    /**
       Indicates an entry was recovered from disk and loaded into the
       cache.
     */
    public abstract void loaded(Identifiable anIdentifiable);

    /**
       Indicates an entry was removed from the cache having, if necessary,
       been saved to disk.
     */
    public abstract void flushed(Identifiable anIdentifiable);

}
