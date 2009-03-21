package org.dancres.blitz.remote.perf;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.MarshalledObject;

import net.jini.core.lease.Lease;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.server.TransactionParticipant;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.EventRegistration;

import net.jini.admin.JoinAdmin;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.lease.SpaceUID;

import com.sun.jini.start.ServiceProxyAccessor;

import com.sun.jini.admin.DestroyAdmin;

/**
 */
public interface Server extends Remote, Landlord {
    public Lease write(MangledEntry anEntry, Transaction aTxn,
                       long aLeaseTime)
        throws RemoteException, TransactionException;

    public MangledEntry read(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime)
        throws RemoteException, TransactionException;
}
