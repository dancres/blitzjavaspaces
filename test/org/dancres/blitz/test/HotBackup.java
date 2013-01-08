package org.dancres.blitz.test;

import net.jini.core.lease.Lease;

import org.dancres.blitz.*;
import org.dancres.blitz.txn.*;
import org.dancres.blitz.mangler.*;

public class HotBackup {
    public static void main(String args[]) {
        try {
            int myTotal = Integer.parseInt(args[0]);
            
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();
            LoadEntry myEntry = new LoadEntry();
            myEntry.init();

            System.out.println("init'd entry");
            MangledEntry myPackedEntry = myMangler.mangle(myEntry);

            System.out.println("Do write: " + myTotal);

            long myStart = System.currentTimeMillis();

            for (int i = 0;i < myTotal; i++)
                mySpace.write(myPackedEntry, null, Lease.FOREVER);

            long myEnd = System.currentTimeMillis();

            System.out.println("Writes completed: " + (myEnd - myStart));

            System.out.println("Backup");

            TxnDispatcher.get().hotBackup(args[1]);

            System.out.println("Do exit");

            System.exit(0);
         } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
         }
    }
}
