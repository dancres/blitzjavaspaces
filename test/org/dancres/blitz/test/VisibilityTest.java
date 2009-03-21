package org.dancres.blitz.test;

import java.io.Serializable;

import java.rmi.MarshalledObject;

import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import net.jini.space.AvailabilityEvent;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.txn.*;

import org.dancres.blitz.SpaceImpl;

public class VisibilityTest {
    public static void main(String args[]) {

        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(new TxnGatewayImpl());
            TxnMgr myMgr = new TxnMgr(1, mySpace.getTxnControl());

            System.out.println("Prepare entry");

            EntryMangler myMangler = EntryMangler.getMangler();
            TestEntry myEntry = new TestEntry();
            myEntry.init();

            System.out.println("init'd entry");
            MangledEntry myPackedEntry = myMangler.mangle(myEntry);

            System.out.println("Do notify");
            mySpace.visibility(new MangledEntry[] {myPackedEntry}, null,
                               new EventListener(),
                               Lease.FOREVER,
                               new MarshalledObject(new String("Here's a vis handback")), false);
            mySpace.visibility(new MangledEntry[] {myPackedEntry}, null,
                               new EventListener(),
                               Lease.FOREVER,
                               new MarshalledObject(new String("Here's a vis-only handback")), true);
            mySpace.notify(myPackedEntry, null, new EventListener(),
                           Lease.FOREVER,
                           new MarshalledObject(new String("Here's a handback")));

            System.out.println("Do write");

            for (int i = 0; i < 3; i++) {
                mySpace.write(myPackedEntry, null, Lease.FOREVER);
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException anIE) {
            }

            System.out.println("Take under txn");
            ServerTransaction myTxn = myMgr.newTxn();

            /*
              If we take and then abort, we should get an extra event
              on the visibility registration
             */
            if (mySpace.take(myPackedEntry, myTxn, 0) == null)
                throw new RuntimeException("Eeek entry's gone");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException anIE) {
            }

            System.out.println("Commit txn");

            myTxn.commit();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException anIE) {
            }

            System.out.println("Release all conflicts");
            ServerTransaction myTxn1 = myMgr.newTxn();

            ServerTransaction myTxn2 = myMgr.newTxn();

            if (mySpace.read(myPackedEntry, myTxn1, 0) == null)
                throw new RuntimeException("Eeek entry's gone");

            if (mySpace.read(myPackedEntry, myTxn2, 0) == null)
                throw new RuntimeException("Eeek entry's gone");

            System.out.println("Abort 1");
            myTxn1.abort();

            System.out.println("Commit 2");
            myTxn2.commit();

            try {
                Thread.sleep(2000);
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

                if (anEvent instanceof AvailabilityEvent) {
                    AvailabilityEvent myEvent = (AvailabilityEvent) anEvent;

                    System.out.println("Entry: " + myEvent.getEntry());
                    System.out.println("Snapshot: " + myEvent.getSnapshot());
                    System.out.println("IsVisibility: " +
                                       myEvent.isVisibilityTransition());
                }

            } catch (Exception anE) {
                System.out.println("Got event but couldn't display it");
                anE.printStackTrace(System.out);
            }
        }
    }
}
