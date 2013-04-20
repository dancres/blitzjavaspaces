package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.SpaceImpl;

public class Unload {

    private static class Taker extends Thread {
        private SpaceImpl theSpace;

        Taker(SpaceImpl aSpace) {
            theSpace = aSpace;
        }

        public void run() {
            try {
                System.out.println("Prepare entry");

                EntryMangler myMangler = new EntryMangler();
                LoadEntry myEntry = new LoadEntry();

                System.out.println("init'd entry");
                MangledEntry myPackedEntry = myMangler.mangle(myEntry, true);

                int myTotal = 0;

                System.out.println("Do takes");

                MangledEntry myTake;

                long myStart = System.currentTimeMillis();

                while ((myTake =
                        theSpace.take(myPackedEntry, null, 5000)) != null) {
                    ++myTotal;
                    System.out.print(".");
                    System.out.flush();
                }

                long myEnd = System.currentTimeMillis();

                System.out.println();
                System.out.println("Took: " + myTotal);

                System.out.println("In: " + (myEnd - myStart - 5000));

                System.out.println("Do stop");

            } catch (Exception anE) {
                System.err.println("Got exception :(");
                anE.printStackTrace(System.err);
            }
        }
    }

    public static void main(String args[]) {

        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Starting takers: " + Integer.parseInt(args[0]));

            for (int i = 0; i < Integer.parseInt(args[0]); i++) {
                System.out.println("t");
                new Taker(mySpace).start();
            }

            Object myLock = new Object();

            synchronized(myLock) {
                try {
                    myLock.wait();
                } catch (InterruptedException anIE) {
                }
            }
        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }

    }
}
