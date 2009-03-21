package org.dancres.blitz.test;

import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.remote.LocalSpace;
import org.dancres.blitz.mangler.EntryMangler;
import org.dancres.blitz.mangler.MangledEntry;
import net.jini.core.lease.Lease;
import net.jini.core.entry.Entry;
import net.jini.space.JavaSpace;

public class LargeObjectTest {
    public static void main(String args[]) {

        try {
            System.out.println("Start space");

            LocalSpace myLocalSpace = new LocalSpace(null);

            JavaSpace mySpace = myLocalSpace.getJavaSpaceProxy();

            int mySize = Integer.parseInt(args[1]);

            byte[] myTestArray = new byte[mySize];

            for (int i = 0; i < mySize; i++) {
                myTestArray[i] = (byte) (i % 256);
            }

            Entry myPackedEntry = new TestEntry(myTestArray);

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

            System.out.println("Total time: " + (myEndTime - myStartTime));
            System.out.println("Time per write: " + myTime);

            System.out.println("Do takes");

            myStart = System.currentTimeMillis();

            myPackedEntry = new TestEntry();

            for (int i = 0; i < Integer.parseInt(args[0]); i++) {
                if (mySpace.take(myPackedEntry, null, 10000) == null)
                    System.out.println("Missed: " + i);
            }

            myEnd = System.currentTimeMillis();

            double myEntriesTaken = Integer.parseInt(args[0]);
            myStartTime = (double) myStart;
            myEndTime = (double) myEnd;

            myTime = (myEndTime - myStartTime) / myEntriesTaken;

            System.out.println("Total time: " + (myEndTime - myStartTime));
            System.out.println("Time per take: " + myTime);

            System.out.println("Do stop");

            myLocalSpace.stop();

        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }

    }

    public static class TestEntry implements Entry {
        public byte[] _payload;

        public TestEntry() {
        }

        public TestEntry(byte[] anArray) {
            _payload = anArray;
        }
    }
}
