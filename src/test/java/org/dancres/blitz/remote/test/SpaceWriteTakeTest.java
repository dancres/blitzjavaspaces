package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;

import net.jini.space.JavaSpace;

import net.jini.core.discovery.LookupLocator;

import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;

import net.jini.admin.Administrable;

import org.dancres.blitz.stats.*;

import org.dancres.blitz.remote.BlitzAdmin;

import org.dancres.blitz.remote.StatsAdmin;

public class SpaceWriteTakeTest {

    public void exec(String[] aDiscoArgs) throws Exception {
        Lookup myFinder = new Lookup(JavaSpace.class);

        JavaSpace mySpace;

        if (aDiscoArgs.length == 0)
            mySpace = (JavaSpace) myFinder.getService();
        else {
            LookupLocator[] myLocators = new LookupLocator[aDiscoArgs.length];

            for (int i = 0; i < aDiscoArgs.length; i++) {
                myLocators[i] = new LookupLocator(aDiscoArgs[i]);
            }

            mySpace = (JavaSpace) myFinder.getService(myLocators);
        }

        System.out.println("Found proxy: " + mySpace + ", " +
                           mySpace.getClass());

        mySpace.write(new TestEntry("abcdef", new Integer(33)),
                      null, Lease.FOREVER);

        System.out.println(mySpace.read(new TestEntry(), null, 10000));
        System.out.println(mySpace.take(new TestEntry(), null, 10000));
        System.out.println(mySpace.take(new TestEntry(), null, 10000));

        Object myProxy = ((Administrable) mySpace).getAdmin();

        if (myProxy instanceof StatsAdmin) {
            System.out.println("Recovering stats");
            Stat[] myStats = ((StatsAdmin) myProxy).getStats();

            for (int i = 0; i < myStats.length; i++) {
                System.out.println(myStats[i]);
            }
        }

        if (myProxy instanceof BlitzAdmin) {
            System.out.println("Found BlitzAdmin");
        }
    }

    public static void main(String args[]) {
        try {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            new SpaceWriteTakeTest().exec(args);
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
    }
}
