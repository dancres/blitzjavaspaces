package org.dancres.blitz.remote.test;

import java.io.Serializable;

import java.rmi.MarshalledObject;
import java.rmi.RMISecurityManager;

import java.util.ArrayList;

import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.EventRegistration;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import net.jini.space.JavaSpace05;
import net.jini.space.AvailabilityEvent;

import net.jini.export.Exporter;

import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;

import net.jini.jeri.tcp.TcpServerEndpoint;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.txn.*;

import org.dancres.blitz.SpaceImpl;

public class VisibilityTest {
    public static void main(String args[]) {

        try {
            System.setSecurityManager(new RMISecurityManager());

            EventListener myListener = new EventListener();

            Exporter myDefaultExporter = 
                new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                      new BasicILFactory(), false, true);


            RemoteEventListener myExportedListener =
                (RemoteEventListener) myDefaultExporter.export(myListener);

            Lookup myLookup = new Lookup(JavaSpace05.class);
            JavaSpace05 mySpace = (JavaSpace05) myLookup.getService();

            myLookup = new Lookup(TransactionManager.class);
            TransactionManager myManager =
                (TransactionManager) myLookup.getService();

            TestEntry myEntry = new TestEntry();
            myEntry.init();

            System.out.println("Do notify");

            ArrayList myTemplates = new ArrayList();
            myTemplates.add(myEntry);

            EventRegistration myAvailReg = 
                mySpace.registerForAvailabilityEvent(myTemplates, null,
                                                     false,
                                                     myExportedListener,
                                                     Lease.FOREVER,
                                                     new MarshalledObject(new String("Here's an availability handback")));
            EventRegistration myVisOnlyReg =
                mySpace.registerForAvailabilityEvent(myTemplates, null,
                                                     true,
                                                     myExportedListener,
                                                     Lease.FOREVER,
                                                     new MarshalledObject(new String("Here's a vis-only handback")));

            EventRegistration myNotifyReg =
                mySpace.notify(myEntry, null, myExportedListener,
                               Lease.FOREVER,
                               new MarshalledObject(new String("Here's a handback")));

            System.out.println("AvailReg: " + myAvailReg.getLease());
            System.out.println("VisOnlyReg: " + myVisOnlyReg.getLease());
            System.out.println("NotifyReg: " + myNotifyReg.getLease());

            System.out.println("Do write");
        
            for (int i = 0; i < 3; i++) {
                mySpace.write(myEntry, null, Lease.FOREVER);
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException anIE) {
            }
        
            System.out.println("Take under txn");

            Transaction.Created myTxnC = 
                TransactionFactory.create(myManager,50000);
        
            Transaction myTxn = myTxnC.transaction;


            /*
              If we take and then abort, we should get an extra event
              on the availability registrations
            */
            if (mySpace.take(myEntry, myTxn, 0) == null)
                throw new RuntimeException("Eeek entry's gone");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException anIE) {
            }
        
            System.out.println("Abort txn");
        
            myTxn.abort();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException anIE) {
            }

            /*
              If we release all read locks via commit or abort we should
              get an additional availability event but no visibility event.
             */
            System.out.println("Release all conflicts");

            myTxnC = 
                TransactionFactory.create(myManager,50000);
        
            Transaction myTxn1 = myTxnC.transaction;

            myTxnC = 
                TransactionFactory.create(myManager,50000);

            Transaction myTxn2 = myTxnC.transaction;

            if (mySpace.read(myEntry, myTxn1, 0) == null)
                throw new RuntimeException("Eeek entry's gone");

            if (mySpace.read(myEntry, myTxn2, 0) == null)
                throw new RuntimeException("Eeek entry's gone");

            System.out.println("Abort 1");
            myTxn1.abort();

            System.out.println("Commit 2");
            myTxn2.commit();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException anIE) {
            }
            
            System.out.println("Done");

            myAvailReg.getLease().cancel();
            myVisOnlyReg.getLease().cancel();
            myNotifyReg.getLease().cancel();

            myDefaultExporter.unexport(true);
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

    public static class EventListener implements RemoteEventListener,
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
