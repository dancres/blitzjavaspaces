package org.dancres.blitz.remote;

import java.rmi.RemoteException;

import java.util.Iterator;
import java.util.HashMap;

import net.jini.core.lease.LeaseMapException;
import net.jini.core.lease.Lease;

import com.sun.jini.lease.AbstractLeaseMap;

import org.dancres.blitz.lease.SpaceUID;

/**
   @todo Check constraints are compatible
 */
class LeaseMapImpl extends AbstractLeaseMap {
    private Landlord theLandlord;

    LeaseMapImpl(Lease aLease, long aDuration, Landlord aLandlord) {
        super(aLease, aDuration);
        theLandlord = aLandlord;
    }

    public boolean canContainKey(Object aKey) {
        if (aKey instanceof LeaseImpl) {
            LeaseImpl myOther = (LeaseImpl) aKey;

            return (myOther.theStub.equals(theLandlord));
        }

        return false;
    }

    public void cancelAll() throws LeaseMapException, RemoteException {
        LeaseImpl[] myLeases = new LeaseImpl[size()];
        SpaceUID[] myUIDs = new SpaceUID[myLeases.length];

        Iterator mySet = keySet().iterator();

        for (int i = 0; mySet.hasNext(); i++) {
            LeaseImpl myLease = (LeaseImpl) mySet.next();

            myLeases[i] = myLease;
            myUIDs[i] = myLease.getUID();
        }

        LeaseResults myResults = theLandlord.cancel(myUIDs);

        if (myResults == null)
            return;

        HashMap myFails = new HashMap();

        for (int i = 0; i < myResults.theFailures.length; i++) {
            if (myResults.theFailures[i] != null) {
                myFails.put(myLeases[i], myResults.theFailures[i]);
            }
        }

        throw new LeaseMapException("Some leases didn't cancel", myFails);
    }

    public void renewAll() throws LeaseMapException, RemoteException {
        LeaseImpl[] myLeases = new LeaseImpl[size()];
        SpaceUID[] myUIDs = new SpaceUID[myLeases.length];
        long[] myDurations = new long[myLeases.length];

        Iterator mySet = keySet().iterator();

        for (int i = 0; mySet.hasNext(); i++) {
            LeaseImpl myLease = (LeaseImpl) mySet.next();

            myLeases[i] = myLease;
            myUIDs[i] = myLease.getUID();
            myDurations[i] = ((Long) get(myLease)).longValue();
        }

        LeaseResults myResults = theLandlord.renew(myUIDs, myDurations);

        long myNow = System.currentTimeMillis();

        HashMap myFailed = null;

        for (int i = 0; i < myResults.theNewDurations.length; i++) {
            long myDuration = myResults.theNewDurations[i];
            if (myDuration == -1) {
                if (myFailed == null)
                    myFailed = new HashMap();

                myFailed.put(myLeases[i], myResults.theFailures[i]);

                // Get this lease out of the map - it's "broken"
                remove(myLeases[i]);
            } else {
                myDuration = myDuration + myNow;
                if (myDuration < 0)
                    myDuration = Long.MAX_VALUE;

                myLeases[i].setExpiration(myDuration);
            }
        }

        if (myFailed != null)
            throw new LeaseMapException("Renewing", myFailed);
    }
}
