package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.space.JavaSpace;

/**
   Place x unique DummyEntry instances in the space.  Typically used to create
   an initial loading for testing.
 */
public class Load {
    public static void main(String args[]) {

        try {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            System.out.println("Find space");

            Lookup myLookup = new Lookup(JavaSpace.class);        
            JavaSpace mySpace = (JavaSpace) myLookup.getService();

            int myTotal = Integer.parseInt(args[0]);

            System.out.println("Write entry's");

            Entry myEntry;

            System.out.println("Do write: " + myTotal);

            for (int i = 0;i < myTotal; i++) {
                myEntry = new DummyEntry(Integer.toString(i));
                mySpace.write(myEntry, null, Lease.FOREVER);
                System.out.print(".");
                System.out.flush();
            }

            System.exit(0);

        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }
    }
}
