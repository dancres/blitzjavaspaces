package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.SpaceImpl;

public class FlushSwap {
    public static void main(String args[]) {
        try {
            int myTotal = Integer.parseInt(args[0]);

            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare Mangler");

            EntryMangler myMangler = new EntryMangler();

            System.out.println("Do writes: " + myTotal);

            for (int i = 0; i < myTotal; i++) {
                MangledEntry myPackedEntry =
                    myMangler.mangle(new FlushEntry(Integer.toString(i)));

                mySpace.write(myPackedEntry, null, Lease.FOREVER);
            }

            System.out.println("Do reads: " + myTotal);

            for (int i = 0; i < myTotal; i++) {
                MangledEntry myPackedTemplate =
                    myMangler.mangle(new FlushEntry(Integer.toString(i)));

                MangledEntry myEntry =
                    mySpace.read(myPackedTemplate, null, 100);

                if (myEntry == null)
                    System.out.println("Yikes didn't find: " + i);
                else
                    System.out.println("Got object: " +
                                       myMangler.unMangle(myEntry));
            }


            System.out.println("Do stop");

            System.exit(0);

        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }
    }

    public static class FlushEntry implements Entry {
        public String theString;

        public FlushEntry() {
        }

        public FlushEntry(String aString) {
            theString = aString;
        }

        public String toString() {
            return theString;
        }
    }
}
