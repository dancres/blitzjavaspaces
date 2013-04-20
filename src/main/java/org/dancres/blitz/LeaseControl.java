package org.dancres.blitz;

import java.io.IOException;

import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;

import org.dancres.blitz.lease.SpaceUID;

/**
   Lease renewal/cancellation is handled via this interface.
 */
public interface LeaseControl {
    /**
       @return the actual lease duration assigned as the result of the
       renew.
     */
    public long renew(SpaceUID aUID, long aDuration)
        throws LeaseDeniedException, UnknownLeaseException, IOException;

    public void cancel(SpaceUID aUID) 
        throws UnknownLeaseException, IOException;
}
