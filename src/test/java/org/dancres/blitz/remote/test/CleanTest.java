package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;

import net.jini.space.JavaSpace;

import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;

import net.jini.core.transaction.TransactionException;

import net.jini.admin.Administrable;

import org.dancres.blitz.stats.*;

import org.dancres.blitz.remote.BlitzAdmin;

import org.dancres.blitz.remote.StatsAdmin;

public class CleanTest {

    public void exec() throws Exception {
        Lookup myFinder = new Lookup(JavaSpace.class);

        JavaSpace mySpace = (JavaSpace) myFinder.getService();

        if (mySpace.take(new TestEntry(), null, 1000) != null)
            throw new Exception("Space should be empty");

        mySpace.write(new TestEntry("abcdef", new Integer(33)),
                      null, Lease.FOREVER);

        Object myProxy = ((Administrable) mySpace).getAdmin();

        if (myProxy instanceof StatsAdmin) {
            System.out.println("Recovering stats");
            Stat[] myStats = ((StatsAdmin) myProxy).getStats();

            for (int i = 0; i < myStats.length; i++) {
                System.out.println(myStats[i]);
            }
        }

        if (mySpace.take(new TestEntry(), null, 1000) == null)
            throw new Exception("Take should succeed");

        System.out.println("Start cleaning");

        if (myProxy instanceof BlitzAdmin) {
            new Cleaner((BlitzAdmin) myProxy).start();

            try {
                mySpace.take(new TestEntry(), null, 15000);
            } catch (TransactionException aTE) {
                System.out.println("Op Successfully interrupted");
            }
        }

        Thread.sleep(10000);

        System.out.println("Test state post cleanup");

        if (mySpace.take(new TestEntry(), null, 1000) != null)
            throw new Exception("Take should return null");

        mySpace.write(new TestEntry("abcdef", new Integer(33)),
                      null, Lease.FOREVER);

        if (mySpace.take(new TestEntry(), null, 5000) == null)
            throw new Exception("Take should return Entry");

        if (myProxy instanceof StatsAdmin) {
            System.out.println("Recovering stats");
            Stat[] myStats = ((StatsAdmin) myProxy).getStats();

            for (int i = 0; i < myStats.length; i++) {
                System.out.println(myStats[i]);
            }
        }
    }

    public static void main(String args[]) {
        try {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            new CleanTest().exec();
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

    public static class Cleaner extends Thread {
        private BlitzAdmin theAdmin;

        Cleaner(BlitzAdmin anAdmin) {
            theAdmin = anAdmin;
        }

        public void run() {
            try {
                Thread.sleep(10000);
                theAdmin.clean();
            } catch (Exception anE) {
                anE.printStackTrace(System.err);
            }
        }
    }
}
