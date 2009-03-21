package org.dancres.blitz.remote.test;

import java.util.ArrayList;
import java.util.List;

import java.rmi.RMISecurityManager;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

public class BulkWriteTest {
    public static void main(String args[]) {
        try {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            new BulkWriteTest().exec();
        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }
    }

    public void exec() throws Exception {
        Lookup myFinder = new Lookup(net.jini.space.JavaSpace05.class);
            
        net.jini.space.JavaSpace05 mySpace =
            (net.jini.space.JavaSpace05) myFinder.getService();

        ArrayList myMangled = new ArrayList();
        ArrayList myLeases = new ArrayList();

        for (int i = 0; i < 100; i++) {
            TestEntry myEntry = new TestEntry(Integer.toString(i));
            myMangled.add(myEntry);
            myLeases.add(new Long(300000 + i));
        }
               
        List myLeaseResults = mySpace.write(myMangled, null, myLeases);

        for (int i = 0; i < myLeaseResults.size(); i++) {
            System.out.println("Lease: " + i + " is " +
            ((Lease) myLeaseResults.get(i)).getExpiration());

            ((Lease) myLeaseResults.get(i)).renew(600000);
        }
    }

    public static class TestEntry implements Entry {
        public String rhubarb;
        public Integer count;

        public TestEntry(String aThing) {
            rhubarb = aThing;
        }

        public void init() {
            rhubarb = "blah";
            count = new Integer(5);
        }

        public String toString() {
            return super.toString() + ", " + rhubarb + ", " + count;
        }
    }
}
