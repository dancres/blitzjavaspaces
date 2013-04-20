package org.dancres.blitz.lease;

import java.io.IOException;

import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lease.LeaseDeniedException;

/**
   Each LeaseHandler instance knows how to renew/cancel a leased resource
   identified by a particular type of SpaceUID.

   @see org.dancres.blitz.lease.LeaseHandlers
 */
public interface LeaseHandler {
    public boolean recognizes(SpaceUID aUID);

    /**
       @return the duration actually assigned to the resource associated with
       the SpaceUID after bounding etc.
     */
    public long renew(SpaceUID aUID, long aLeaseDuration)
        throws UnknownLeaseException, LeaseDeniedException, IOException;

    public void cancel(SpaceUID aUID)
        throws UnknownLeaseException, IOException;
}
