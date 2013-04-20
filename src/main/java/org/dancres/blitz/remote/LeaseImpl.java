package org.dancres.blitz.remote;

import java.rmi.RemoteException;

import com.sun.jini.lease.AbstractLease;

import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.LeaseMap;
import net.jini.core.lease.Lease;

import net.jini.id.Uuid;
import net.jini.id.ReferentUuid;

import org.dancres.blitz.lease.SpaceUID;

/**
   Generic lease implementation used to wrap all Blitz's internal lease
   implementations.
 */
public class LeaseImpl extends AbstractLease implements ReferentUuid {
    Landlord theStub;
    Uuid theUuid;

    private SpaceUID theUID;

    public LeaseImpl(Landlord aLandlord, Uuid aUuid, SpaceUID aUID,
              long anExpiration) {

        super(anExpiration);
        theStub = aLandlord;
        theUID = aUID;
        theUuid = aUuid;
    }

    void setLandlord(Landlord aLandlord, Uuid aUid) {
        theStub = aLandlord;
        theUuid = aUid;
    }

    public Uuid getReferentUuid() {
        return theUuid;
    }

    protected long doRenew(long duration)
        throws UnknownLeaseException, LeaseDeniedException, RemoteException {

        return theStub.renew(theUID, duration);
    }

    public void cancel()
        throws UnknownLeaseException, RemoteException {

        theStub.cancel(theUID);
    }

    void setExpiration(long anExpiry) {
        expiration = anExpiry;
    }

    SpaceUID getUID() {
        return theUID;
    }

    /**
       Each entry in a LeaseMap needs a duration to renew for so, when we
       create the leasemap, we insert the current lease as having the specified
       renewal duration.
     */
    public LeaseMap createLeaseMap(long aDuration) {
        return new LeaseMapImpl(this, aDuration, theStub);
    }

    public boolean canBatch(Lease aLease) {

        if (aLease instanceof LeaseImpl) {
            LeaseImpl myOther = (LeaseImpl) aLease;

            return myOther.theStub.equals(theStub);
        }

        return false;
    }

    public int hashCode() {
        return theUID.hashCode();
    }

    public boolean equals(Object anObject) {
        if (anObject instanceof LeaseImpl) {
            if (theUuid.equals(((LeaseImpl) anObject).theUuid)) {
                return (theUID.equals(((LeaseImpl) anObject).theUID));
            }
        }

        return false;
    }

    public String toString() {
        return "LeaseImpl: " + theUID;
    }
}
