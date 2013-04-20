package org.dancres.blitz;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.DiskTxn;

public class SpaceFifonessTest {
    public static void main(String args[]) {

        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();

            System.out.println("init'd entry");
            MangledEntry myPackedEntry;

            for (int i = 0; i < 500; i++) {
                myPackedEntry = myMangler.mangle(new TestEntry(i));

                mySpace.write(myPackedEntry, null, Lease.FOREVER);

                /*
                System.out.println("Do write: " + 
                                   mySpace.write(myPackedEntry, null,
                                                 Lease.FOREVER));
                */
            }

            // Write some more Entry's whilst we do takes
            //
            new Writer(mySpace, myMangler).start();

            myPackedEntry = myMangler.mangle(new TestEntry());

            long myStart = System.currentTimeMillis();

            for (int i = 0; i < 1000; i++) {
                MangledEntry myEntry = mySpace.take(myPackedEntry, null, 
                                                    Lease.FOREVER);
                
                System.out.println(myMangler.unMangle(myEntry));
            }

            long myEnd = System.currentTimeMillis();

            System.out.println("1000 takes in: " + (myEnd - myStart));

            double myStartTime = (double) myStart;
            double myEndTime = (double) myEnd;

            double myTime = (myEndTime - myStartTime) / 1000;

            System.out.println("Time per take: " + myTime);

            System.out.println("Do stop");

            mySpace.stop();

        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }

    }

    static class Writer extends Thread {
        private SpaceImpl theSpace;
        private EntryMangler theMangler;

        Writer(SpaceImpl aSpace, EntryMangler aMangler) {
            theSpace = aSpace;
            theMangler = aMangler;
        }

        public void run() {
            try {
                for (int i = 501; i < 1001; i++) {
                    MangledEntry myPackedEntry =
                        theMangler.mangle(new TestEntry(i));

                    theSpace.write(myPackedEntry, null,
                                   Lease.FOREVER);
                    /*
                    System.out.println("Do write: " + 
                                       theSpace.write(myPackedEntry, null,
                                                      Lease.FOREVER));
                    */
                }
            } catch (Exception anE) {
                System.err.println("Writer blew");
                anE.printStackTrace(System.err);
            }
        }
    }

    public static class TestEntry implements Entry {
        public Integer count;
        public Integer meta;

        public TestEntry() {
        }

        public TestEntry(int aCount) {
            count = new Integer(aCount);
        }

        public String toString() {
            return super.toString() + ", " + count;
        }
    }
}
