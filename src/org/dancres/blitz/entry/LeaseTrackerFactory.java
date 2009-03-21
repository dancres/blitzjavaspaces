package org.dancres.blitz.entry;

import java.io.IOException;

import org.dancres.blitz.lease.LeaseReaper;
import org.dancres.blitz.lease.ReapFilter;

import org.dancres.blitz.oid.Allocator;

class LeaseTrackerFactory {
    /**
       Create a LeaseTracker capable of managing all leases for a particular
       entry type which is using a particular Allocator instance to assign ids.
     */
    static LeaseTracker getTracker(String aType, Allocator anAllocator)
        throws IOException {

        if (EntryRepositoryFactory.getReaper().isActive())
            return new LeaseTrackerImpl(aType,
                                        anAllocator.getMaxZoneId());
        else
            return new NullTrackerImpl(aType);
    }

    private static class NullTrackerImpl implements LeaseTracker {
        private String theType;

        NullTrackerImpl(String aType) {
            // System.err.println("NullTracker: " + aType);
            theType = aType;
        }

        public void bringOutTheDead(EntryReaper aReaper) {
            // Do nothing
        }

        public void delete(PersistentEntry anEntry) throws IOException {
            // Do nothing
        }

        public void update(PersistentEntry anEntry) throws IOException {
            // Do nothing
        }

        public void write(PersistentEntry anEntry) throws IOException {
            // Do nothing
        }

        public void close() throws IOException {
            // Do nothing
        }

        public void delete() throws IOException {
            // Do nothing
        }
    }
}
