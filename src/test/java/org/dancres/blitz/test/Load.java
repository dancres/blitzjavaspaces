package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.SpaceImpl;

public class Load {
    public static void main(String args[]) {

        try {
            int myTotal = Integer.parseInt(args[0]);

            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();
            LoadEntry myEntry = new LoadEntry("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            // myEntry.init();

            System.out.println("init'd entry");
            MangledEntry myPackedEntry = myMangler.mangle(myEntry);

            System.out.println("Do write: " + myTotal);

            long myStart = System.currentTimeMillis();

            for (int i = 0;i < myTotal; i++)
                mySpace.write(myPackedEntry, null, Lease.FOREVER);

            long myEnd = System.currentTimeMillis();

            System.out.println("Writes completed: " + (myEnd - myStart));

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
