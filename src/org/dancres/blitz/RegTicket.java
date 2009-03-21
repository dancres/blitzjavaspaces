package org.dancres.blitz;

import org.dancres.blitz.lease.SpaceUID;

/**
   Contains the registration details resulting from a notify request.
   Includes a lease time determined by the notify subsystem.
 */
public interface RegTicket {
    public SpaceUID getUID();
    public long getSourceId();
    public long getSeqNum();
    public long getExpirationTime();
}
