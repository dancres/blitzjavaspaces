package org.dancres.blitz.arc;

import java.io.IOException;

import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.dancres.blitz.Logging;
import org.dancres.blitz.entry.ci.CacheIndexer;

import org.dancres.blitz.cache.Identifiable;
import org.dancres.blitz.cache.Identifier;
import org.dancres.blitz.cache.Cache;
import org.dancres.blitz.cache.CacheListener;
import org.dancres.blitz.cache.CacheListenerSet;

/**
   A cache which implements ARC (see "ARC: A Self-Tuning, Low Overhead
   Replacement Cache" in USENIX FAST '03) or, try,
   <a href="http://citeseer.nj.nec.com/megiddo03arc.html"> CiteSeer</a>. <p>
 */
public class ArcCache implements Cache {
    static Logger theLogger =
        Logging.newLogger("org.dancres.blitz.arc.cache");
    /**
       Used for rapid location of a relevant CBD in memory - saves us scanning
       lists.
     */
    private Map theBlockIndex = new HashMap();

    private Lru t1 = new Lru(Lru.T1);
    private Lru t2 = new Lru(Lru.T2);
    private Lru b1 = new Lru(Lru.B1);
    private Lru b2 = new Lru(Lru.B2);

    private int theTargetT1;

    /**
       Variable <i>c</i> in the ARC algorithmic description
     */
    private int theCacheSize;

    private BackingStore theStore;

    private CacheListenerSet theListeners = new CacheListenerSet();

    public ArcCache(BackingStore aStore, int aCacheSize) {
        theStore = aStore;
        theCacheSize = aCacheSize;
    }

    public void add(CacheListener aListener) {
        theListeners.add(aListener);
    }

    public int getSize() {
        return theCacheSize;
    }

    public int getActiveSize() {
        int mySize;

        synchronized(this) {
            mySize = theBlockIndex.size();
        }

        return mySize >> 1;
    }

    /**
       For recovery purposes, we wish to be able to ensure that something
       has made it to disk and, if it hasn't, re-insert it to the cache.
       This is different from insert which is used for those entries which
       we know are not present on disk.  We must first check disk and,
       if there's no entry, populate the cache with a new one.
     */
    public RecoverySummary recover(Identifiable anIdentifiable)
        throws IOException {

        // Try a fetch from disk
        Identifiable myIdentifiable = theStore.load(anIdentifiable.getId());

        // Did we get it?
        if (myIdentifiable == null) {
            // No, we need to establish a copy in cache from passed version
            return new RecoverySummary(insert(anIdentifiable), false);
        } else {
            /*
              myIdentifiable contains the up-to-date disk image and this
              must be inserted into the cache rather than anIdentifiable
              which should only be used when the disk image was missing.
            */
            return new RecoverySummary(find(myIdentifiable.getId(),
                    myIdentifiable), true);
        }
    }

    /**
       <p> We achieve insert by adding a new memory-only entry into
       BackingStore using a specific method like prepareToCache(Entry).  We
       then instruct ArcCache to page-in the Entry which will cause ArcCache to
       make room and pull the Entry from the BackingStore which, coveniently,
       has the entry waiting in memory. </p>

       ONLY call this method for adding Entries which should be treated as
       newly written.
     */
    public CacheBlockDescriptor insert(Identifiable anIdentifiable)
        throws IOException {
        CacheBlockDescriptor myCBD = find(anIdentifiable.getId(),
                                          anIdentifiable);

        theListeners.signal(CacheListenerSet.LOADED, anIdentifiable);
        
        return myCBD;
    }

    /**
       Locate an Identifiable associated with Identifier - loading from
       disk if necessary.

       @return a CBD with the lock asserted representing the requested entry.
       Note that the entry may no longer exist but we allow "non-existence" to
       be cached because that may also be significant to some application.
       In such cases, this method will return <code>null</code>.
     */
    public CacheBlockDescriptor find(Identifier anId) throws IOException {
        CacheBlockDescriptor myCBD = find(anId, null);

        // If Entry isn't present, release CBD and return null to caller.
        if (myCBD.isEmpty()) {
            myCBD.release();
            return null;
        } else {
            return myCBD;
        }
    }

    /**
       @param aPreLoad If the identifiable associated with the identifier is
       not in the cache, make space in the cache and associate aPreLoad with
       the identifier.

       @return a CBD with the lock asserted representing the requested entry.
       Note that the entry may no longer exist but we allow "non-existence" to
       be cached because that may also be significant to some application.
       In such cases, getContent() will return null and isEmpty will return
       true.
     */
    private CacheBlockDescriptor find(Identifier anId,
                                      Identifiable aPreLoad)
        throws IOException {

        CacheBlockDescriptor myDesc = null;
        boolean needsFetch = false;

        synchronized(this) {
            myDesc = (CacheBlockDescriptor) theBlockIndex.get(anId);

            try {
                // Is it in cache?
                if (myDesc != null) {
                    myDesc.acquire();

                    /*
                      Consistency check - shouldn't have something in cache
                      already when we're pre-loading for insert

                    if (aPreLoad != null)
                        theLogger.log(Level.SEVERE, "Hmmm, we're inserting but we appear to have this in cache already :( " + anId + ", " + aPreLoad + ", " + myDesc.getId(), new RuntimeException());
                    */

                    switch(myDesc.getWhere()) {
                        case Lru.T1 :
                        case Lru.T2 : {
                            // System.out.println("Cache hit");

                            remove(myDesc);
                            t2.mruInsert(myDesc);

                            break;
                        }

                        case Lru.B1 :
                        case Lru.B2 : {
                            /* I/O - no dir change */
                            // System.out.println("Cache miss [in dir]");

                            if (myDesc.getWhere() == Lru.B1) {
                                theTargetT1 =
                                    Math.min(theTargetT1 +
                                             Math.max(b2.length() /
                                                      b1.length(), 1),
                                             theCacheSize);
                            } else {
                                theTargetT1 =
                                    Math.max(theTargetT1 -
                                             Math.max(b1.length() /
                                                      b2.length(), 1),
                                             0);
                            }

                            remove(myDesc);

                            // Flush an entry from cache
                            replace();  // I/O

                            myDesc.setId(anId);

                            t2.mruInsert(myDesc);

                            needsFetch = true;
                        }
                    }
                } else {
                    /* I/O - dir updated */

                    // System.out.println("Cache miss");

                    if ((t1.length() + b1.length()) == theCacheSize) {
                        if (t1.length() < theCacheSize) {
                            myDesc = b1.lruRemove();
                            myDesc.acquire();
                            replace();  // I/O
                        } else {
                            myDesc = t1.lruRemove();
                            myDesc.acquire();
                            destage(myDesc);  // I/O
                        }
                        theBlockIndex.remove(myDesc.getId());
                    } else {
                        if ((t1.length() + t2.length() + b1.length() +
                             b2.length()) >= theCacheSize) {
                            if ((t1.length() + t2.length() + b1.length() +
                                 b2.length()) == (2 * theCacheSize)) {
                                myDesc = b2.lruRemove();
                                myDesc.acquire();
                                theBlockIndex.remove(myDesc.getId());
                            } else {
                                myDesc =
                                    new CacheBlockDescriptor();
                                myDesc.acquire();
                            }

                            replace();  // I/O
                        } else {
                            myDesc = new CacheBlockDescriptor();
                            myDesc.acquire();
                        }
                    }

                    myDesc.setId(anId);

                    t1.mruInsert(myDesc);

                    theBlockIndex.put(anId, myDesc);

                    needsFetch = true;
                }
            } catch (InterruptedException anIE) {
                theLogger.log(Level.SEVERE, "Couldn't lock CDB", anIE);
            }
        }

        if (needsFetch) {
            myDesc.setContent(fetch(anId, aPreLoad));
        }

        return myDesc;
    }

    private void remove(CacheBlockDescriptor aDesc) {
        switch(aDesc.getWhere()) {
            case Lru.T1 : {
                t1.remove(aDesc);
                break;
            }
            case Lru.T2 : {
                t2.remove(aDesc);
                break;
            }
            case Lru.B1 : {
                b1.remove(aDesc);
                break;
            }
            case Lru.B2 : {
                b2.remove(aDesc);
                break;
            }
        }
    }

    /**
      Flush out another CDB if necessary.  In the original algorithm, we'd also
      need to rip the discarded CBD's cache page and put it in this CDB.  But,
      our version holds the object directly on the CDB so we don't need to do
      this.  Note we're flushing out a CDB in t1 or t2 which means we're
      removing an entry from the cache to make room for the new one.
      The one we're making room for could be a phantom hit or a real miss.
    */
    private void replace() throws IOException {
        CacheBlockDescriptor myDesc;

        try {
            if (t1.length() >= Math.max(1, theTargetT1)) {
                myDesc = t1.lruRemove();
                myDesc.acquire();

                b1.mruInsert(myDesc);
            } else {
                myDesc = t2.lruRemove();
                myDesc.acquire();

                b2.mruInsert(myDesc);
            }

            destage(myDesc);

            myDesc.setContent(null);

            myDesc.release();

        } catch (InterruptedException anIE) {
            theLogger.log(Level.SEVERE, "Failed to lock CBD", anIE);
        }
    }

    private void destage(CacheBlockDescriptor aCBD) throws IOException {
        // System.out.println("Flush: " + aCBD.getId());

        theStore.save(aCBD.getContent());
    }

    public void dump() {
        dump(t1);
        dump(t2);
        dump(b1);
        dump(b2);
    }

    private void dump(Lru anLru) {
        synchronized(anLru) {
            anLru.dump();
        }
    }

    public synchronized void sync() throws IOException {

        long myStart = 0;

        if (theLogger.isLoggable(Level.FINE)) {
            theLogger.log(Level.FINE, "Syncing: " + theStore.getName());

            myStart = System.currentTimeMillis();
        }

        t1.sync(theStore);
        t2.sync(theStore);

        if (theLogger.isLoggable(Level.FINE)) {
            long myEnd = System.currentTimeMillis();

            theLogger.log(Level.FINE,
                "Time to scan lists: " + (myEnd - myStart));
        }
    }

    private Identifiable fetch(Identifier anId, Identifiable aPreLoad)
        throws IOException {

        // System.out.println("Fetch: " + anId + ", " + aPreLoad);

        Identifiable myIdent;

        if (aPreLoad != null)
            myIdent = aPreLoad;
        else
            myIdent = theStore.load(anId);

        return myIdent;
    }

    public void forceSync(CacheBlockDescriptor aCBD) throws IOException {
        // System.err.println("Expunge");
        
        destage(aCBD);

        // System.err.println("Expunge done");
    }
}
