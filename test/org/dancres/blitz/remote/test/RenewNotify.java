package org.dancres.blitz.remote.test;

import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.MarshalledObject;

import java.io.Serializable;

import net.jini.export.Exporter;

import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;

import net.jini.jeri.tcp.TcpServerEndpoint;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.EventRegistration;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import net.jini.space.JavaSpace;

import net.jini.lease.LeaseRenewalManager;
import net.jini.lease.LeaseListener;
import net.jini.lease.LeaseRenewalEvent;

public class RenewNotify {
    public static void main(String args[]) {
        try {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            Lookup myLookup = new Lookup(JavaSpace.class);

            JavaSpace mySpace = (JavaSpace) myLookup.getService();

            TestEntry myEntry = new TestEntry();
            myEntry.init();

            new Renewer(mySpace).start();

            while (true) {
                System.out.println("Do write");

                mySpace.write(myEntry, null, 50000);

                if (mySpace.take(myEntry, null, 50000) == null)
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

    private static class EventListener implements RemoteEventListener {

        private RemoteEventListener theStub;

        EventListener() throws RemoteException {
            Exporter myDefaultExporter = 
                new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                      new BasicILFactory(), false, true);

            theStub = (RemoteEventListener) myDefaultExporter.export(this);
        }

        RemoteEventListener getStub() {
            return theStub;
        }

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
        private JavaSpace theSpace;
        private EventListener theListener;
        private LeaseRenewalManager theManager;

        Renewer(JavaSpace aSpace) {
            theSpace = aSpace;
        }

        public void run() {
            try {
                theListener = new EventListener();
                theManager = new LeaseRenewalManager();

                LeaseListener myDebugListener = new DebugListener();

                while (true) {
                    EventRegistration myReg = 
                        theSpace.notify(new TestEntry(), null,
                                        theListener.getStub(),
                                        30000,
                                        new MarshalledObject(new Integer(12345)));

                    theManager.renewFor(myReg.getLease(), Lease.FOREVER,
                                        30000, myDebugListener);

                    Thread.sleep(60000);

                    theManager.cancel(myReg.getLease());
                }

            } catch (Exception anE) {
                System.err.println("Failed to renew");
                anE.printStackTrace(System.err);
            }
        }
    }

    private static class DebugListener implements LeaseListener {
        public void notify(LeaseRenewalEvent anEvent) {
            System.err.println("Got lease renewal problem");

            System.err.println(anEvent.getException());
            System.err.println(anEvent.getExpiration());
            System.err.println(anEvent.getLease());

            System.exit(0);
        }
    }
}
