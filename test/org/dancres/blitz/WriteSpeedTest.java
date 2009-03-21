package org.dancres.blitz;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

public class WriteSpeedTest {
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

            System.out.println("Do writes");

            long myStart = System.currentTimeMillis();

            for (int i = 0; i < Integer.parseInt(args[0]); i++) {
                mySpace.write(myPackedEntry, null, Lease.FOREVER);
            }

            long myEnd = System.currentTimeMillis();

            double myEntriesWritten = Integer.parseInt(args[0]);
            double myStartTime = (double) myStart;
            double myEndTime = (double) myEnd;

            double myTime = (myEndTime - myStartTime) / myEntriesWritten;

            System.out.println("Time per write: " + myTime);

            System.out.println("Do takes");

            myStart = System.currentTimeMillis();

            for (int i = 0; i < Integer.parseInt(args[0]); i++) {
                if (mySpace.take(myPackedEntry, null, 10000) == null)
                    System.out.println("Missed: " + i);
            }

            myEnd = System.currentTimeMillis();

            double myEntriesTaken = Integer.parseInt(args[0]);
            myStartTime = (double) myStart;
            myEndTime = (double) myEnd;

            myTime = (myEndTime - myStartTime) / myEntriesTaken;

            System.out.println("Time per take: " + myTime);

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
}
