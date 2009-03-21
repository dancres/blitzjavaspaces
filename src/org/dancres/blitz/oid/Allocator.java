package org.dancres.blitz.oid;

import java.io.IOException;

/**
   <p>An Allocator generates a series of UIDImpl's each of which is
   guarenteed to be unique within the scope of the Allocator.  Note that
   allocated id's are not re-used so an Allocator can, potentially, become
   exhausted.  This can be avoided by allowing the allocator sufficient
   space to allocate from by increasing the maximum number of available
   allocator ids.</p>

   <p>Allocator's are self-recovering.  They check <code>BootContext</code>
   for the presence of a <code>SyncBarrier</code> instance which indicates
   the maximum number of log operations between checkpoints.  This number
   represents the maximum number of entry writes or notify registrations which
   could have been carried out without the appropriate Allocator instances
   being sync'd to disk.  Thus, the allocator simply ensures that all it's
   oid generators are incremented by the value of the SyncBarrier and sync'd
   back to disk.  This guarentees that there will be no oid overlap and
   means that "user code" need not be involved in the recovery process.</p>
 */
public interface Allocator {
    public OID getNextId() throws IOException;

    /**
       @return the maximum number of zone id's this Allocator instance
       is allowed to use.
     */
    public int getMaxZoneId();
}
