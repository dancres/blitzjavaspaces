package org.dancres.blitz.remote.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;

import java.rmi.RMISecurityManager;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

public class BulkWriteTakeTest {
    public static void main(String args[]) {
        try {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            new BulkWriteTakeTest().exec();
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
            System.out.println("Lease: " + i + " is " + ((Lease) myLeaseResults.get(i)).getExpiration());
        }

        Collection myTakes;
        int myTotal = 0;
        
        Entry myTemplate = new TestEntry();
        ArrayList myTemplates = new ArrayList();
        myTemplates.add(myTemplate);

        while ((myTakes = mySpace.take(myTemplates, null,
                                       5000, 25)).size() != 0) {

            System.out.println("Chunk");
            Iterator myTaken = myTakes.iterator();
            while (myTaken.hasNext()) {
                Entry myEntry = (Entry) myTaken.next();
                System.out.println(myEntry);
                ++myTotal;
            }
        }

        System.out.println("Total takes: " + myTotal);
    }

    public static class TestEntry implements Entry {
        public String rhubarb;
        public Integer count;

        public TestEntry(String aThing) {
            rhubarb = aThing;
        }

        public TestEntry() {
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
