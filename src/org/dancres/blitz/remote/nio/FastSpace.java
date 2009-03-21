package org.dancres.blitz.remote.nio;

import net.jini.core.entry.UnusableEntryException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.server.TransactionParticipant;
import net.jini.core.lease.Lease;

import java.io.IOException;
import java.rmi.RemoteException;

import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.remote.BackEndSpace;

/**
 */
public interface FastSpace extends BackEndSpace, TransactionParticipant {
    public void init() throws IOException;
    public boolean isInited();
}
