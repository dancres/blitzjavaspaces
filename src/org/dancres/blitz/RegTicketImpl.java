package org.dancres.blitz;

import org.dancres.blitz.lease.SpaceUID;

import org.dancres.blitz.notify.Registrar;

/**
   Contains the necessary details to build an EventRegistration object.
   These details are internal source ID, initial sequence number and
   UID (which will be converted into a SpaceUID).  The raw details are
   received from EventQueue via the Registrar interface.  The details are then
   slightly massaged (convert UID to SpaceUID).  This object is then
   returned to the caller who can then turn these details into a suitable
   EventRegistration object.
 */
class RegTicketImpl implements RegTicket, Registrar {
    private long theInternalSource;
    private long theInitialSequenceNum;
    private SpaceUID theUID;
    private long theExpirationTime;

    RegTicketImpl(long anExpirationTime) {
        theExpirationTime = anExpirationTime;
    }

    public void newRegistration(long aSourceId, long aSeqNum,
                                SpaceUID aUID) {
        theInternalSource = aSourceId;
        theInitialSequenceNum = aSeqNum;
        theUID = aUID;
    }

    public SpaceUID getUID() {
        return theUID;
    }

    public long getSourceId() {
        return theInternalSource;
    }

    public long getSeqNum() {
        return theInitialSequenceNum;
    }

    public long getExpirationTime() {
        return theExpirationTime;
    }
}
