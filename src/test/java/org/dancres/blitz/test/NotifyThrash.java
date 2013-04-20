package org.dancres.blitz.test;

import java.io.Serializable;

import java.rmi.MarshalledObject;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.event.*;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.mangler.EntrySizer;

public class NotifyThrash {
    private static final Entry theEntry = new LargeEntry(24 * 1024 * 1024);

    public static void main(String args[]) {
        try {
            EntrySizer mySizer = new EntrySizer();

            /*
            System.out.println("Avg Entry Size: " +
                               mySizer.computeSize(new LargeEntry(123 * 1024)));
            */
            System.out.println("Avg Entry Size: " +
                               mySizer.computeSize(theEntry));

            new NotifyThrash().test(Integer.parseInt(args[0]),
                                  Integer.parseInt(args[1]));

        } catch (Exception anE) {
            anE.printStackTrace(System.err);
        }
    }

    public void test(int aNumUpdaters, int aNumNotifies) throws Exception {
        LocalSpace myLocalSpace = new LocalSpace(null);
        JavaSpace mySpace = myLocalSpace.getProxy();
        
        Entry myTemplate = new LargeEntry();

        for (int i = 0; i < aNumNotifies; i++) {
            mySpace.notify(myTemplate, null,
                    new EventListener(),
                    Lease.FOREVER,
                    new MarshalledObject(new Integer(i)));
        }

        for (int i = 0; i < aNumUpdaters; i++) {
            new Updater(myLocalSpace).start();
        }
    }

    private static class Updater extends Thread {
        private JavaSpace theSpace;

        Updater(LocalSpace aSpace) {
            theSpace = aSpace.getProxy();
        }

        public void run() {
            while(true) {

                try {
                    theSpace.write(theEntry, null, Lease.FOREVER);
                    
                    if (theSpace.take(new LargeEntry(), null, 0) == null)
                        throw new RuntimeException("Lost Entry");

                    // Thread.sleep(1000);

                } catch (Exception anE) {
                    anE.printStackTrace(System.err);
                }
            }
        }
    }

    public static class LargeEntry implements Entry {
        public Integer[] theData;

        public LargeEntry() {
        }

        public LargeEntry(int aSize) {
            theData = new Integer[aSize];
        }
    }

    private static class EventListener implements RemoteEventListener,
                                                  Serializable {

        private int theCount;

        public void notify(RemoteEvent anEvent) {

            try {
                boolean doPrint = false;

                synchronized(this) {
                    ++theCount;
                    if (theCount == 10) {
                        theCount = 0;
                        doPrint = true;
                    }
                }

                if (doPrint)
                    System.out.print(".");

            } catch (Exception anE) {
                System.out.println("Got event but couldn't display it");
                anE.printStackTrace(System.out);
            }
        }
    }
}