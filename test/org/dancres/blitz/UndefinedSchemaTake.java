package org.dancres.blitz;

import net.jini.core.lease.Lease;
import net.jini.core.entry.Entry;

import org.dancres.blitz.mangler.EntryMangler;
import org.dancres.blitz.mangler.MangledEntry;
import org.dancres.blitz.disk.Disk;

/**
 */
public class UndefinedSchemaTake {
    public static void main(String args[]) {

        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();

            System.out.println("init'd entry");
            MangledEntry myPackedEntry = myMangler.mangle(new TestEntry().init3());
            System.out.println("Do non-match take: ");
            System.out.println(mySpace.takeIfExists(myPackedEntry, null,
                5000));

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
