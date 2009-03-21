package org.dancres.blitz;

import java.io.IOException;

import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;

import org.dancres.blitz.lease.SpaceUID;
import org.dancres.blitz.lease.LeaseHandlers;

import org.dancres.blitz.disk.DiskTxn;

class LeaseControlImpl implements LeaseControl {

    public long renew(SpaceUID aUID, long aDuration)
        throws LeaseDeniedException, UnknownLeaseException, IOException {

        DiskTxn myTxn = DiskTxn.newTxn();

        try {
            return LeaseHandlers.renew(aUID, aDuration);
        } finally {
            myTxn.commit();
        }
    }

    public void cancel(SpaceUID aUID) 
        throws UnknownLeaseException, IOException {

        DiskTxn myTxn = DiskTxn.newTxn();

        try {
            LeaseHandlers.cancel(aUID);
        } finally {
            myTxn.commit();
        }
    }
}
