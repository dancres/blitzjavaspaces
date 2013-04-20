package org.dancres.blitz.cache;

/**
   Register one of these with a Cache to be informed of it's actions.
   Useful for auxilliary actions such as indexing of Identifiables.
 */
public interface CacheListener {
    public void dirtied(Identifiable anIdentifiable);

    /**
       Indicates an entry was recovered from disk and loaded into the
       cache.
     */
    public void loaded(Identifiable anIdentifiable);

    /**
       Indicates an entry was removed from the cache having, if necessary,
       been saved to disk.
     */
    public void flushed(Identifiable anIdentifiable);
}
