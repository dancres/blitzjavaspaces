package org.dancres.blitz.entry;

import java.io.IOException;

import org.dancres.blitz.arc.CacheBlockDescriptor;
import org.dancres.blitz.lease.ReapFilter;
import org.dancres.blitz.oid.OID;

/**
  Responsible for taking each ID passed, attempting to load the
  specified sleeve, checking with ReapFilters and then deleting sleeve
  if appropriate.  Note, Storage is expected to pass OID's to us
  whilst we pass EntrySleeveImpls to our ReapFilter.
 */
public class CleanerImpl implements EntryReaper {
    private SleeveCache theCache;
    private ReapFilter theFilter;

    /**
     * @param aCache  the cache to request sleeves from for OIDs
     *                passed in by Storage
     * @param aFilter the filter to check with before performing deletion.
     */
    CleanerImpl(SleeveCache aCache, ReapFilter aFilter) {
        theCache = aCache;
        theFilter = aFilter;
    }

    public void clean(TupleLocator aLocator) throws IOException {
        /**
         Accept TupleLocator from Storage
         Attempt load to cache
         Check with theFilter
         Mark deleted (if filter says okay)
         Release CBD
         */
        long myCurrentTime = System.currentTimeMillis();

        long myPurged = 0;

        while (aLocator.fetchNext()) {
            OID myOID = aLocator.getOID();

            CacheBlockDescriptor myCBD = theCache.load(myOID);

            if (myCBD != null) {
                EntrySleeveImpl mySleeve = (EntrySleeveImpl)
                    myCBD.getContent();

                /*
                  If you expect a particular EntrySleeve to be cleaned
                  and it's not happening, uncomment these to provide some
                  hints
                  System.err.println("Has expired: " +
                  mySleeve.hasExpired(myCurrentTime));

                  System.err.println("Has deleted: " +
                  mySleeve.getState().test(SleeveState.DELETED));

                  System.err.println("Filtered: " +
                  theFilter.filter(mySleeve));
                 */

                if ((mySleeve.hasExpired(myCurrentTime)) &&
                    (! mySleeve.getState().test(SleeveState.DELETED)) &&
                    (! theFilter.filter(mySleeve))) {

                    /*
                    System.err.println("Lease delete:" +
                                       mySleeve.getUid());
                    */

                    mySleeve.getState().set(SleeveState.DELETED);
                    mySleeve.markDirty();
                    theCache.getCounters().didPurge();

                    ++myPurged;
                }

                myCBD.release();
            }
        }

        EntryStorage.theLogger.info("Reaper purged " + theCache +
            " " + myPurged);
    }
}
