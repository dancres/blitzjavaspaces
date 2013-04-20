package org.dancres.blitz.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.Transaction;

import net.jini.space.JavaSpace;

import org.dancres.blitz.EntryChit;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.remote.view.EntryViewUID;

/**
   All operations associated with JavaSpaceAdmin are implemented via this
   interface at the server-side
 */
public interface EntryViewAdmin extends Remote {
    public JavaSpace getJavaSpaceProxy() throws RemoteException;

    /*
     * @param isJavaSpace05 if <code>true</code> enforces any defined lease bounds
     * and asserts locks when performing the scan/acquire internally.
     * This is used internally to differentiate between old and new contents
     * methods as JavaSpaceAdmin::contents does not do leases.
     */
    public ViewResult newView(MangledEntry[] aTemplates, Transaction aTxn,
                             long aLeaseDuration, boolean isJavaSpace05,
                             long aLimit, int anInitialChunk)
        throws RemoteException, TransactionException;

    public EntryChit[] getNext(EntryViewUID aEntryViewUID, int aChunkSize)
        throws RemoteException;

    /**
       Delete the entry associated with the specified cookie which would
       have been passed down in an EntryChit
     */
    public void delete(Object aCookie) throws RemoteException;

    public void close(EntryViewUID aEntryViewUID) throws RemoteException;
}
