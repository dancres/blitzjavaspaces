package org.dancres.blitz.remote.perf;

import java.io.Serializable;

import java.rmi.RemoteException;
import java.rmi.MarshalledObject;

import net.jini.space.JavaSpace;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;

import net.jini.core.lease.Lease;

import net.jini.lookup.entry.ServiceInfo;
import net.jini.lookup.entry.Name;

import com.sun.jini.lookup.entry.BasicServiceType;

import net.jini.admin.Administrable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.FileInputStream;

import net.jini.id.Uuid;
import net.jini.id.ReferentUuid;
import net.jini.id.ReferentUuids;

import org.dancres.blitz.mangler.EntryMangler;
import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.VersionInfo;

import com.sun.jini.proxy.MarshalledWrapper;

/**
 */
class Proxy implements Serializable {
    private Server theStub;
    private transient EntryMangler theMangler;

    public static void main(String anArgs[]) {
        try {
            String myStubFile = ServiceImpl.STUB_FILE;

            if (anArgs.length > 1)
                myStubFile = anArgs[1];

            int myThreads = 1;

            if (anArgs.length > 0)
                myThreads = Integer.parseInt(anArgs[0]);

            ObjectInputStream myOIS =
                new ObjectInputStream(new FileInputStream(myStubFile));

            Server myStub = (Server) myOIS.readObject();

            System.out.println(myStub);

            Proxy myProxy = new Proxy(myStub);

            Thrasher[] myThrashers = new Thrasher[myThreads];

            for (int i = 0; i < myThreads; i++) {
                myThrashers[i] = new Thrasher(myProxy);
                myThrashers[i].start();
            }

            new Watcher(myThrashers).start();

        } catch (Exception anE) {
            System.err.println("Failed to init proxy");
            anE.printStackTrace(System.err);
        }
    }

    private static class Watcher extends Thread {
        private Thrasher[] theThrashers;

        Watcher(Thrasher[] aThrashers) {
            theThrashers = aThrashers;
        }

        public void run() {
            while(true) {
                try {
                    Thread.sleep(60000);

                    for (int i = 0; i < theThrashers.length; i++) {
                        theThrashers[i].dumpCounts();
                    }
                } catch (InterruptedException anIE) {
                    System.err.println("Dead watcher");
                }
            }
        }
    }

    private static class Thrasher extends Thread {
        private Proxy theProxy;
        private long theCount;

        Thrasher(Proxy aProxy) {
            theProxy = aProxy;
        }

        public void run() {
            while (true) {
                try {
                    DummyEntry myPackedEntry = new DummyEntry("rhubarb");

                    theProxy.read(myPackedEntry, null, 0);
                    theProxy.write(myPackedEntry, null, 0);

                    synchronized(this) {
                        ++theCount;
                    }
                } catch (Exception anE) {
                    System.err.println("Thrasher died: " + anE);
                }
            }
        }

        public void dumpCounts() {
            synchronized(this) {
                System.out.println("Write/read combos: " + theCount);
                theCount = 0;
            }
        }
    }

    Proxy(Server aServer) {
        theStub = aServer;
    }

    private synchronized EntryMangler getMangler() {
        if (theMangler == null)
            theMangler = new EntryMangler();

        return theMangler;
    }

    private MangledEntry packEntry(Entry anEntry) {
        if (anEntry == null)
            return MangledEntry.NULL_TEMPLATE;
        // Is it a snapshot?
        else if (anEntry instanceof MangledEntry)
            return (MangledEntry) anEntry;
        else
            return getMangler().mangle(anEntry);
    }

    public Lease write(Entry entry, Transaction txn, long lease)
        throws TransactionException, RemoteException {

        return theStub.write(packEntry(entry), txn, lease);
    }

    public Entry read(Entry tmpl, Transaction txn, long timeout)
        throws UnusableEntryException, TransactionException, 
               InterruptedException, RemoteException {

        MangledEntry myResult =
            theStub.read(packEntry(tmpl), txn, timeout);

        return (myResult != null) ?
            getMangler().unMangle(myResult) : null;
    }

    static final class SnapshotEntry implements Entry {
        private MangledEntry thePackage;

        SnapshotEntry(MangledEntry aPackage) {
            thePackage = aPackage;
        }

        MangledEntry getPackage() {
            return thePackage;
        }
    }
}
