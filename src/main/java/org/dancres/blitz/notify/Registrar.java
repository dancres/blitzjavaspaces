package org.dancres.blitz.notify;

import org.dancres.blitz.lease.SpaceUID;

/**
   Once EventQueue has performed a registration, it forwards the appropriate
   details to a Registrar instance which can then do any further processing
   required before passing the results on to the ultimate recipient.
 */
public interface Registrar {
    public void newRegistration(long aSourceId, long aSeqNum,
                                SpaceUID aUID);
}
