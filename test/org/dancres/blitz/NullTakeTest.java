package org.dancres.blitz;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.DiskTxn;

public class NullTakeTest {
    public static void main(String args[]) {

        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();

            System.out.println("init'd entry");
            MangledEntry myPackedEntry =
                myMangler.mangle(new TestEntry().init());

            System.out.println("Do null take:");
            System.out.println(mySpace.take(MangledEntry.NULL_TEMPLATE, null, 
                                            1000));

            System.out.println("Do write: " + 
                               mySpace.write(myPackedEntry, null,
                                             Lease.FOREVER));

            MangledEntry myWild = myMangler.mangle(new TestEntry());
            System.out.println("Do null read: ");
            System.out.println(mySpace.read(MangledEntry.NULL_TEMPLATE,
                                            null, Lease.FOREVER));

            System.out.println("Do null take:");
            System.out.println(mySpace.take(MangledEntry.NULL_TEMPLATE, null, 
                                            Lease.FOREVER));

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
