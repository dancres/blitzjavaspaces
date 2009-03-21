package org.dancres.blitz;

import java.io.Serializable;

import java.rmi.MarshalledObject;

import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;

import org.dancres.blitz.mangler.*;

public class SpaceNotifyTest {
    public static void main(String args[]) {

        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();
            TestEntry myEntry = new TestEntry();
            myEntry.init();

            System.out.println("init'd entry");
            MangledEntry myPackedEntry = myMangler.mangle(myEntry);

            System.out.println("Do notify");
            mySpace.notify(myPackedEntry, null, new EventListener(),
                           Lease.FOREVER,
                           new MarshalledObject(new String("Here's a handback")));

            System.out.println("Do write");

            for (int i = 0; i < 3; i++) {
                mySpace.write(myPackedEntry, null, Lease.FOREVER);
            }

            try {
                Thread.sleep(10000);
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
