package org.dancres.blitz.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lease.LeaseDeniedException;

import org.dancres.blitz.lease.SpaceUID;

/**
   Remote lease renewal operations will be carried out through this interface
 */
public interface Landlord extends Remote {
    public LeaseResults renew(SpaceUID[] aLeases, long[] aDurations)
        throws RemoteException;

    /**
       @return <code>null</code> if all cancels worked, otherwise, return
       an array init'd with the exceptions mapped at the same indices as the
       originally passed leases.
     */
    public LeaseResults cancel(SpaceUID[] aLeases)
        throws RemoteException;

    public long renew(SpaceUID aUID, long aDuration)
        throws UnknownLeaseException, LeaseDeniedException, RemoteException;

    public void cancel(SpaceUID aUID)
        throws UnknownLeaseException, RemoteException;
}
