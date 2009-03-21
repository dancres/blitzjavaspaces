package org.dancres.blitz.remote;

import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import org.dancres.blitz.mangler.MangledEntry;

import java.rmi.RemoteException;
import java.rmi.MarshalledObject;

/**
 */
public interface BackEndSpace {
    public LeaseImpl write(MangledEntry anEntry, Transaction aTxn,
                       long aLeaseTime)
        throws RemoteException, TransactionException;

    public MangledEntry take(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime)
        throws RemoteException, TransactionException;

    public MangledEntry read(MangledEntry anEntry, Transaction aTxn,
                             long aWaitTime)
        throws RemoteException, TransactionException;

    public MangledEntry takeIfExists(MangledEntry anEntry, Transaction aTxn,
                                     long aWaitTime)
        throws RemoteException, TransactionException;

    public MangledEntry readIfExists(MangledEntry anEntry, Transaction aTxn,
                                     long aWaitTime)
        throws RemoteException, TransactionException;

    public EventRegistration notify(MangledEntry anEntry, Transaction aTxn,
                                    RemoteEventListener aListener,
                                    long aLeaseTime,
                                    MarshalledObject aHandback)
        throws RemoteException, TransactionException;

    /**
       Used by the base proxy (which implements Administrable) to return
       a proxy for the admin functions.  Saves sending the admin proxy to
       clients that don't intend to use it.
     */
    public Object getAdmin() throws RemoteException;
}
