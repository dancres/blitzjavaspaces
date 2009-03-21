package org.dancres.blitz;

import java.io.Serializable;
import java.rmi.MarshalledObject;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.lease.SpaceUID;

import org.dancres.blitz.test.TxnMgr;
import org.dancres.blitz.test.TxnGatewayImpl;

public class RenewNotify {
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

            System.out.println("Do notify");

            RegTicket myTicket = 
                mySpace.notify(myPackedEntry, null, new EventListener(),
                               30000,
                               new MarshalledObject(new Integer(12345)));
            
            SpaceUID myNotifyUID = myTicket.getUID();

            new Renewer(mySpace, myNotifyUID).start();

            while (true) {
                System.out.println("Do write");

            
                SpaceUID myEntryUID =
                    mySpace.write(myPackedEntry, null, 50000).getUID();

                if (mySpace.take(myPackedEntry, null, 50000) == null)
                    throw new Exception("Failed to take");

                Thread.sleep(10000);
            }

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

    private static class Renewer extends Thread {
        private SpaceImpl theSpace;
        private SpaceUID theUID;

        Renewer(SpaceImpl aSpace, SpaceUID aUID) {
            theSpace = aSpace;
            theUID = aUID;
        }

        public void run() {
            try {
                while (true) {
                    Thread.sleep(20000);

                    System.err.println("Renew");
                    theSpace.getLeaseControl().renew(theUID, 30000);
                }

            } catch (Exception anE) {
                System.err.println("Failed to renew");
                anE.printStackTrace(System.err);
                System.exit(0);
            }
        }
    }
}
