package org.dancres.blitz;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.DiskTxn;

public class Clean {
    public static void main(String args[]) {

        try {
            EntryMangler myMangler = new EntryMangler();

            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            while (true) {
                MangledEntry myEntry = 
                    mySpace.take(MangledEntry.NULL_TEMPLATE, null, 
                                 1000);

                if (myEntry == null)
                    break;
                else
                    System.out.println("Took: " + myMangler.unMangle(myEntry));
            }

            mySpace.stop();

        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }

    }
}
