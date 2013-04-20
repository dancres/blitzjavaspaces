package org.dancres.blitz;

import org.dancres.blitz.lease.SpaceUID;

class WriteTicketImpl implements WriteTicket {
    private SpaceUID theUID;
    private long theExpirationTime;

    WriteTicketImpl(SpaceUID aUID, long anExpirationTime) {
        theUID = aUID;
        theExpirationTime = anExpirationTime;
    }

    public SpaceUID getUID() {
        return theUID;
    }

    public long getExpirationTime() {
        return theExpirationTime;
    }
}
