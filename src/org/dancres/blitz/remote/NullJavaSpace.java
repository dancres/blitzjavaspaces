package org.dancres.blitz.remote;

import java.io.Serializable;

import java.rmi.RemoteException;
import java.rmi.MarshalledObject;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.EventRegistration;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;

import net.jini.core.lease.Lease;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;

public class NullJavaSpace implements net.jini.space.JavaSpace, Serializable {

    public NullJavaSpace() {
    }

    public Lease write(Entry entry, Transaction txn, long lease)
        throws TransactionException, RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public Entry read(Entry tmpl, Transaction txn, long timeout)
        throws UnusableEntryException, TransactionException, 
               InterruptedException, RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public Entry readIfExists(Entry tmpl, Transaction txn, long timeout)
        throws UnusableEntryException, TransactionException, 
               InterruptedException, RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public Entry take(Entry tmpl, Transaction txn, long timeout)
        throws UnusableEntryException, TransactionException, 
               InterruptedException, RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public Entry takeIfExists(Entry tmpl, Transaction txn, long timeout)
        throws UnusableEntryException, TransactionException, 
               InterruptedException, RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public EventRegistration
        notify(Entry tmpl, Transaction txn, RemoteEventListener listener,
               long lease, MarshalledObject handback)
        throws TransactionException, RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public Entry snapshot(Entry e) throws RemoteException {

        throw new org.dancres.util.NotImplementedException();
    }

    public String toString() {
        return "NullJavaSpace";
    }
}
