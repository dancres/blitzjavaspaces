package org.dancres.blitz.test;

import java.io.Serializable;

import java.rmi.RemoteException;
import java.rmi.MarshalledObject;

import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.RemoteEvent;

import net.jini.core.entry.Entry;

import org.dancres.blitz.*;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.txn.*;

public class TxnNotify {
    public static void main(String args[]) {
        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(new TxnGatewayImpl());

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();

            TestEntry myEntry = new TestEntry().init();

            TestEntry myTemplate = new TestEntry();

            MangledEntry myPackedEntry = myMangler.mangle(myEntry);
            MangledEntry myPackedTemplate = myMangler.mangle(myTemplate);

            System.out.println("Notify");

            mySpace.notify(myPackedTemplate, null, new EventListener(),
                           Lease.FOREVER,
                           new MarshalledObject(new Integer(12345)));

            System.out.println("Notify txn");
            TxnMgr myMgr = new TxnMgr(1, mySpace.getTxnControl());
            ServerTransaction myTxn = myMgr.newTxn();

            mySpace.notify(myPackedTemplate, myTxn, new EventListener(),
                           Lease.FOREVER,
                           new MarshalledObject(new Integer(67890)));

            System.out.println("Write txn");
            mySpace.write(myPackedEntry, myTxn, Lease.FOREVER);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException anIE) {
            }

            System.out.println("Write null");
            mySpace.write(myPackedEntry, null, Lease.FOREVER);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException anIE) {
            }

            System.out.println("Prepare and commit txn");
            myTxn.commit();

            try {
                Thread.sleep(5000);
            } catch (InterruptedException anIE) {
            }
            
            System.out.println("Write null");
            mySpace.write(myPackedEntry, null, Lease.FOREVER);

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

        public TestEntry init() {
            rhubarb = "blah";
            count = new Integer(5);

            return this;
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
