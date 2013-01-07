package org.dancres.blitz;

import java.io.Serializable;
import java.rmi.MarshalledObject;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;

import net.jini.core.transaction.server.*;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.lease.SpaceUID;

import org.dancres.blitz.remote.TxnMgr;
import org.dancres.blitz.test.TxnGatewayImpl;

public class LeaseTest {
    public static void main(String args[]) {
        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(new TxnGatewayImpl());

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();
            TestEntry myEntry = new TestEntry();
            myEntry.init();

            System.out.println("init'd entry");
            MangledEntry myPackedEntry = myMangler.mangle(new TestEntry());

            System.out.println("Do write");

            SpaceUID myEntryUID =
                mySpace.write(myPackedEntry, null, 50000).getUID();

            System.out.println("Do notify");

            RegTicket myTicket = 
                mySpace.notify(myPackedEntry, null, new EventListener(),
                               20000,
                               new MarshalledObject(new Integer(12345)));
            
            SpaceUID myNotifyUID = myTicket.getUID();

            System.out.println("Renew entry");
            mySpace.getLeaseControl().renew(myEntryUID, 500000);
            System.out.println("Renew notify");
            mySpace.getLeaseControl().renew(myNotifyUID, 500000);

            System.out.println("Renew entry");
            mySpace.getLeaseControl().renew(myEntryUID, Lease.FOREVER);
            System.out.println("Renew notify");
            mySpace.getLeaseControl().renew(myNotifyUID, Lease.FOREVER);

            System.out.println("Cancel entry");
            mySpace.getLeaseControl().cancel(myEntryUID);
            System.out.println("Cancel notify");
            mySpace.getLeaseControl().cancel(myNotifyUID);

            System.out.println("Txn write renewal");

            TxnMgr myMgr = new TxnMgr(1, mySpace.getTxnControl());

            ServerTransaction myTxn = myMgr.newTxn();

            myEntryUID = mySpace.write(myPackedEntry, myTxn, 50000).getUID();

            mySpace.getLeaseControl().renew(myEntryUID, 500000);

            myTxn.commit();

            System.out.println("Settling");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException anIE) {
            }

            System.out.println("Do stop");

            mySpace.stop();

        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }
    }

    public static class TestEntry implements Entry {
        public String rhubarb;
        public Integer count;

        public TestEntry() {
        }

        public void init() {
            rhubarb = "blah";
            count = new Integer(5);
        }

        public String toString() {
            return super.toString() + ", " + rhubarb + ", " + count;
        }
    }

    private static class EventListener implements RemoteEventListener,
                                                  Serializable {
        public void notify(RemoteEvent anEvent) {

            try {
                System.out.println("Got event: " + anEvent.getSource() + ", " +
                                   anEvent.getID() + ", " +
                                   anEvent.getSequenceNumber() + ", " + 
                                   anEvent.getRegistrationObject().get());
            } catch (Exception anE) {
                System.out.println("Got event but couldn't display it");
                anE.printStackTrace(System.out);
            }
        }
    }
}
