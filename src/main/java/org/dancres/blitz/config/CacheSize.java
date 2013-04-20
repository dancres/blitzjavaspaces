package org.dancres.blitz.config;

/**
   <p>Use this constraint to specify a per-type Entry cache size which will
   override the global default specified in <code>entryReposCacheSize</code>
   </p>
 */
public class CacheSize implements EntryConstraint {
    private int theSize;

    public CacheSize(int aSize) {
        theSize = aSize;
    }

    public int getSize() {
        return theSize;
    }
}