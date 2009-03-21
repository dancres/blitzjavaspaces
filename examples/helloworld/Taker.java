package helloworld;

import java.rmi.RMISecurityManager;

import net.jini.space.JavaSpace;

import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;

/**
   Taker will attempt to take as many entry's as it can find.  If any take
   blocks longer than 120 seconds, Taker assumes there are no more Entry's
   and stops.
 */
public class Taker {

    public void exec() throws Exception {
        // Locate a JavaSpace - we will only find those registered
        // BEFORE we were started up - doing it properly requires
        // ServiceDiscoveryManager - for another time....
        //
        System.out.println("Looking for JavaSpace");

        Lookup myFinder = new Lookup(JavaSpace.class);

        JavaSpace mySpace = (JavaSpace) myFinder.getService();

        System.out.println("Got space - waiting for Entry's");

        // We want to find any TestEntry instance so we don't set the
        // fields which gives us wildcard match
        Entry myTemplate = new TestEntry();

        while (true) {
            Entry myResult = mySpace.take(myTemplate, null, 120 * 1000);

            if (myResult == null) {
                System.out.println("No more entry's in the last 120 seconds");
                break;
            }

            System.out.println("Took: " + myResult);
        }
    }

    public static void main(String args[]) {
        // We must set the RMI security manager to allow downloading of code
        // which, in this case, is the Blitz proxy amongst other things
        //
        try {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            new Taker().exec();
        } catch (Exception anE) {
            System.err.println("Whoops");
            anE.printStackTrace(System.err);
        }
    }
}
