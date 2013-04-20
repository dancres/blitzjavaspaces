package org.dancres.blitz.remote;

import java.util.List;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.MarshalledObject;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;

import net.jini.id.Uuid;

import org.dancres.blitz.mangler.MangledEntry;

/**
    Back end server interface which defines all the functionality required for
    JavaSpace05.  Note that the contents operation is handled via EntryViewAdmin
 
    @see org.dancres.blitz.remote.EntryViewAdmin
 */
public interface JS05Server extends Remote {
    public List write(List aMangledEntries, Transaction aTxn, List aLeaseTimes)
        throws RemoteException, TransactionException;

    public List take(MangledEntry[] aTemplates, Transaction aTxn,
                     long aWaitTime, long aLimit) 
        throws RemoteException, TransactionException;

    public EventRegistration
        registerForVisibility(MangledEntry[] aTemplates, Transaction aTxn,
                              RemoteEventListener aListener, long aLeaseTime,
                              MarshalledObject aHandback,
                              boolean visibilityOnly)
        throws RemoteException, TransactionException;
}
