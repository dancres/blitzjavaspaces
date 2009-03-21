package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;

import net.jini.space.JavaSpace;

import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;

import net.jini.admin.Administrable;

import com.sun.jini.outrigger.JavaSpaceAdmin;
import com.sun.jini.outrigger.AdminIterator;

import org.dancres.blitz.stats.*;

import org.dancres.blitz.remote.BlitzAdmin;

import org.dancres.blitz.remote.StatsAdmin;

public class JSAdminTest {

    public void exec() throws Exception {
        Lookup myFinder = new Lookup(JavaSpace.class);

        JavaSpace mySpace = (JavaSpace) myFinder.getService();

        mySpace.write(new TestEntry("abcdef", new Integer(33)),
                      null, Lease.FOREVER);

        System.out.println(mySpace.read(new TestEntry(), null, 10000));
        System.out.println(mySpace.take(new TestEntry(), null, 10000));
        System.out.println(mySpace.take(new TestEntry(), null, 10000));

        Object myProxy = ((Administrable) mySpace).getAdmin();

        org.dancres.jini.util.DiscoveryUtil.dumpInterfaces(myProxy);
        
        if (myProxy instanceof StatsAdmin) {
            System.out.println("Recovering stats");
            Stat[] myStats = ((StatsAdmin) myProxy).getStats();

            for (int i = 0; i < myStats.length; i++) {
                System.out.println(myStats[i]);
            }
        }

        if (myProxy instanceof JavaSpaceAdmin) {
            System.out.println("Testing JavaSpaceAdmin");

            for (int i = 0; i < 107; i++) {
                mySpace.write(new TestEntry("abcdef", new Integer(i)),
                              null, Lease.FOREVER);
            }

            System.out.println("Entry's written - attempting recovery");

            JavaSpaceAdmin myAdmin = (JavaSpaceAdmin) myProxy;

            System.out.println("Admin says it's space is: " + myAdmin.space());

            AdminIterator myIt = myAdmin.contents(new TestEntry(), null);

            Entry myEntry;
            int myTotal = 0;

            while ((myEntry = myIt.next()) != null) {
                System.out.println(myEntry);
                ++myTotal;
            }

            System.out.println("Got: " + myTotal);

            myIt.close();
        }
    }

    public static void main(String args[]) {
        try {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            new JSAdminTest().exec();
        } catch (Exception anE) {
            System.err.println("Whoops");
            anE.printStackTrace(System.err);
        }
    }

    public static class TestEntry implements Entry {
        public String theName;
        public Integer theValue;

        public TestEntry() {
        }

        public TestEntry(String aName, Integer aValue) {
            theName = aName;
            theValue = aValue;
        }

        public String toString() {
            return theName + ", " + theValue;
        }
    }
}
