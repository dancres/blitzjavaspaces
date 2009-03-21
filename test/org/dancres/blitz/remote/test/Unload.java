package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;

/**
   Clean out all the DummyEntry instances in the space.  Typically used for
   post-test cleanup.
 */
public class Unload {
    public static void main(String args[]) {

        try {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            System.out.println("Find space");

            Lookup myLookup = new Lookup(JavaSpace.class);        
            JavaSpace mySpace = (JavaSpace) myLookup.getService();

            System.out.println("Take entry's");

            Entry myEntry = new DummyEntry();

            int myTotal = 0;

            System.out.println("Do takes");

            Entry myTake = null;

            while ((myTake =
                    mySpace.take(myEntry, null, 5000)) != null) {
                ++myTotal;
                System.out.print(".");
                System.out.flush();
            }

            System.out.println();
            System.out.println("Took: " + myTotal);

        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }

    }
}
