package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;

import java.util.ArrayList;

import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;
import net.jini.core.lease.UnknownLeaseException;

public class SpaceIteratorTest {
    public static void main(String args[]) {
        try {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            new SpaceIteratorTest().exec();
        } catch (Exception anE) {
            System.err.println("Whoops");
            anE.printStackTrace(System.err);
        }
    }

    private void exec() throws Exception {
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());

        Lookup myFinder = new Lookup(net.jini.space.JavaSpace05.class);

        net.jini.space.JavaSpace05 mySpace =
            (net.jini.space.JavaSpace05) myFinder.getService();

        Entry myWild = new TestEntry();
        ArrayList myTemplates = new ArrayList();
        myTemplates.add(myWild);

        System.out.println("Read all from an empty space in batches");
        net.jini.space.MatchSet myResult =
            mySpace.contents(myTemplates,
                             null, Lease.FOREVER,
                             100);

        dumpBatch(myResult);

        for (int i = 0; i < 100; i++) {
            Entry myEntry = new TestEntry().init();

            System.out.println("Do write: " + 
                               mySpace.write(myEntry, null,
                                             Lease.FOREVER));
        }

        System.out.println("Test 1, read all in batches");

        myResult =
            mySpace.contents(myTemplates,
                             null, Lease.FOREVER,
                             100);

        dumpBatch(myResult);

        System.out.println("Test 2, read some, cancel, read some");

        myResult =
            mySpace.contents(myTemplates,
                             null, Lease.FOREVER,
                             100);

        for (int i = 0; i < 48; i++) {
            System.out.println("Got: " + myResult.next() + " (" + (i + 1) +
                               ")");
        }

        System.out.println("Cancel lease");
        myResult.getLease().cancel();

        try {
            dumpBatch(myResult);
        } catch (Exception anE) {
            System.out.println("Test 2 got exception - good");
            System.out.println(anE);
        }

        System.out.println("Test 3, read not quite all by setting a limit");

        myResult =
            mySpace.contents(myTemplates,
                             null, Lease.FOREVER,
                             75);

        dumpBatch(myResult);

        System.out.println("Test 4, read all with oversized limit");

        myResult =
            mySpace.contents(myTemplates,
                             null, Lease.FOREVER,
                             500);

        dumpBatch(myResult);
    }
    
    private static void dumpBatch(net.jini.space.MatchSet aResult)
        throws Exception {
        Entry myEntry;

        int i = 1;

        while ((myEntry = aResult.next()) != null) {
            System.out.println("Got: " + myEntry + " (" + i + ")");
            ++i;
        }

        aResult.getLease().cancel();
    }

    public static class TestEntry implements Entry {
        public String rhubarb;
        public Integer count;

        public TestEntry() {
        }

        public TestEntry init() {
            rhubarb = "blah";
            count = new Integer(5);

            return this;
        }

        public TestEntry init2() {
            rhubarb = "blahblah";
            count = new Integer(5);

            return this;
        }

        public TestEntry init3() {
            rhubarb = "blahh";
            count = new Integer(5);

            return this;
        }

        public String toString() {
            return super.toString() + ", " + rhubarb + ", " + count;
        }
    }
}
