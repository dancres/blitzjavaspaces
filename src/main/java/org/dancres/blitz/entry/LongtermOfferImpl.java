package org.dancres.blitz.entry;

import java.io.IOException;
import java.util.logging.Level;

import org.dancres.blitz.arc.CacheBlockDescriptor;
import org.dancres.blitz.oid.OID;

/**
 */
public class LongtermOfferImpl implements LongtermOffer {
    private CacheBlockDescriptor _cbd;

    LongtermOfferImpl(CacheBlockDescriptor aDescriptor) {
        _cbd = aDescriptor;

        // System.err.println("Locked: " + _cbd + ", " + Thread.currentThread());
    }

    public String getEntryType() {
        return ((EntrySleeveImpl) _cbd.getContent()).getType();
    }
    
    public boolean offer(SearchVisitor aVisitor) throws IOException {
        long myStartTime = System.currentTimeMillis();

        EntrySleeveImpl mySleeve =
            (EntrySleeveImpl) _cbd.getContent();

        boolean offered = false;

        /*
         If the JS specification is changed to cope with the issues
         discussed in http://archives.java.sun.com/cgi-bin/wa?A2=ind0311&L=javaspaces-users&F=&S=&P=4599 and http://archives.java.sun.com/cgi-bin/wa?A2=ind0311&L=javaspaces-users&F=&S=&P=3590 then we need to do two things:

         (1) Allow the SearchVisitor to see Sleeve's even if they've
         expired.
         (2) Having "shown" it to the SearchVisitor we'd need to
         query the ReapFilters and if they don't boycott, mark the
         item deleted.

         These two steps have the effect of allowing a *ifExists to
         conflict on lease-expired entries that have been locked
         by a transaction and ensures we only delete such entries
         when no transactions have posession of them anymore.  Of
         course, this is somewhat slower and less efficient as
         there's never a circumstance under which we can be assured
         that a SearchVisitor *never* sees a particular entry again.

         If we must implement the strategy of flunking a transaction
         owing to a lock on a lease expired object, this would be
         best dealt with by having TxnOp's check expiries at prepare
         or commit time.  However this is much less appealing as what
         is basically a pessimistic transaction API becomes
         optimistic in this case and only in this case.

         All this applies to the similar statement in offer() below.
        */
        if (!mySleeve.getState().test(SleeveState.DELETED)) {

            // If it's expired, mark it deleted, subject to filters
            //
            if (mySleeve.hasExpired(myStartTime)) {

                if (!EntryRepositoryFactory.getReaper().filter(mySleeve)) {
                    mySleeve.getState().set(SleeveState.DELETED);
                    mySleeve.markDirty();

                    // Update stats
                    // theCounters.didPurge();

                    // myLog.incDeletions();
                }
            } else {
                OpInfo myInfo =
                    new FindEntryOpInfo(mySleeve.getType(),
                        mySleeve.getOID(),
                        aVisitor.isDeleter());

                SearchOffer myOffer =
                    new SearchOfferImpl(mySleeve.getEntry(),
                        myInfo);

                aVisitor.offer(myOffer);

                offered = true;

                // myLog.incOffers();
            }
        }

        return offered;
    }

    public void release() throws IOException {
        // System.err.println("Unlocked: " + _cbd + ", " + Thread.currentThread());
        _cbd.release();
    }

    public OID getOID() {
        return ((EntrySleeveImpl) _cbd.getContent()).getOID();
    }
}
