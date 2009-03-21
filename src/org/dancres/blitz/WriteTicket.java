package org.dancres.blitz;

import org.dancres.blitz.lease.SpaceUID;

/**
   The result of writing an entry consists of a SpaceUID representing the
   actual entry and a lease time determined by the space.
 */
public interface WriteTicket {
    public SpaceUID getUID();
    public long getExpirationTime();
}
