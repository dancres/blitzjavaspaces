package helloworld;

import java.rmi.RMISecurityManager;

import net.jini.space.JavaSpace;

import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;

import java.util.Random;

/**
   Writer puts 5 randomly generated Entry's in the space which should
   hopefully be found by Taker.
 */
public class Writer {

    public void exec() throws Exception {
        // Locate a JavaSpace - we will only find those registered
        // BEFORE we were started up - doing it properly requires
        // ServiceDiscoveryManager - for examples of lookup (including
        // the use of SDM) see:
        //      http://www.dancres.org/cottage/service_lookup.html
        //
        System.out.println("Looking for JavaSpace");

        Lookup myFinder = new Lookup(JavaSpace.class);

        JavaSpace mySpace = (JavaSpace) myFinder.getService();

        System.out.println("Got space - writing Entry's");

        Random myRNG = new Random();

        for (int i = 0; i < 5; i++) {
            TestEntry myEntry =
                new TestEntry(Integer.toString(myRNG.nextInt()));

            mySpace.write(myEntry, null, Lease.FOREVER);

            System.out.println("Wrote: " + myEntry);
        }
    }

    public static void main(String args[]) {
        // We must set the RMI security manager to allow downloading of code
        // which, in this case, is the Blitz proxy amongst other things
        //
        try {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            new Writer().exec();
        } catch (Exception anE) {
            System.err.println("Whoops");
            anE.printStackTrace(System.err);
        }
    }
}
