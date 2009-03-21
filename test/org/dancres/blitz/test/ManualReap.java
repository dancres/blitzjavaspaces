package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.SpaceImpl;

import org.dancres.blitz.disk.Disk;

/**
   Test manual reaping - note that the leaseReapInterval or similar variable
   needs to be set to LeaseReaper.MANUAL_REAP for this to work (tweak the
   config file accordingly).
 */
public class ManualReap {
    public static void main(String args[]) {

        try {
            int myTotal = Integer.parseInt(args[0]);

            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();
            ManualReapEntry myEntry = new ManualReapEntry("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            // myEntry.init();

            System.out.println("init'd entry");
            MangledEntry myPackedEntry = myMangler.mangle(myEntry);

            System.out.println("Do write: " + myTotal);

            System.out.println("Time now");

            long myStart = System.currentTimeMillis();

            for (int i = 0;i < myTotal; i++) {
                long myLease = (100 * i);
                mySpace.write(myPackedEntry, null, myLease);
            }

            long myEnd = System.currentTimeMillis();

            System.out.println("Writes completed: " + (myEnd - myStart));

            System.out.println("Wait");

            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException anIE) {
            }

            System.out.println("Force sync");

            Disk.sync();

            System.out.println("Do reap: " + System.currentTimeMillis());

            mySpace.reap();

            System.out.println("Wait");

            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException anIE) {
            }

            System.out.println("Do stop");

            System.exit(0);

        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }
    }
}
