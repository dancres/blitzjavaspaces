package org.dancres.blitz;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.DiskTxn;

public class SpaceSchemaBreakTest {
    public static void main(String args[]) {

        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();

            System.out.println("init'd entry");
            MangledEntry myPackedEntry =
                myMangler.mangle(new TestEntry().init());

            System.out.println("Do write: " + 
                               mySpace.write(myPackedEntry, null,
                                             Lease.FOREVER));

            MangledEntry myWild = myMangler.mangle(new TestEntry());
            System.out.println("Do wild read: ");
            System.out.println(mySpace.read(myWild, null, Lease.FOREVER));

            System.out.println("Do specific take:");
            System.out.println(mySpace.take(myPackedEntry, null, 
                                            Lease.FOREVER));

            myPackedEntry = myMangler.mangle(new TestEntry().init2());

            System.out.println("Do write: " + 
                               mySpace.write(myPackedEntry, null,
                                             Lease.FOREVER));

            myPackedEntry = myMangler.mangle(new TestEntry().init2());
            System.out.println("Do other take: ");
            System.out.println(mySpace.take(myPackedEntry, null, 
                                            Lease.FOREVER));

            myPackedEntry = myMangler.mangle(new TestEntry().init3());
            System.out.println("Do non-match take: ");
            System.out.println(mySpace.take(myPackedEntry, null,
                                            5000));

            // This is naughty - we're messing with internals directly
            // but it's a useful test
            //
            System.out.println("Do sync write: " +
                               mySpace.write(myPackedEntry, null,
                                             Lease.FOREVER));

            System.out.println("Force sync");
            Disk.sync();

            System.out.println("Do wild take");
            System.out.println(mySpace.take(myWild, null, Lease.FOREVER));

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

        /*
          Run the test once, then uncomment these and run again
        */
        public Integer another;
        public Integer andAnother;

        public TestEntry() {
        }

        public TestEntry init() {
            rhubarb = "blah";
            count = new Integer(5);

            return this;
        }

        public TestEntry init2() {
            rhubarb = "blahblah";
            count = new Integer(5);

            return this;
        }

        public TestEntry init3() {
            rhubarb = "blahh";
            count = new Integer(5);

            return this;
        }

        public String toString() {
            return super.toString() + ", " + rhubarb + ", " + count;
        }
    }
}
