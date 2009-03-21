package org.dancres.blitz;

import java.util.ArrayList;
import java.util.List;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

public class BulkWriteTest {
    public static void main(String args[]) {

        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();

            ArrayList myMangled = new ArrayList();
            ArrayList myLeases = new ArrayList();

            for (int i = 0; i < 100; i++) {
                TestEntry myEntry = new TestEntry(Integer.toString(i));
                MangledEntry myPackedEntry = myMangler.mangle(myEntry);
                myMangled.add(myPackedEntry);
                myLeases.add(new Long(300000 + i));
            }
               
            List myLeaseResults = mySpace.write(myMangled, null, myLeases);

            for (int i = 0; i < myLeaseResults.size(); i++) {
                System.out.println("Lease: " + i + " is " + ((WriteTicket) myLeaseResults.get(i)).getExpirationTime());
            }

            System.out.println("Do stop");

            mySpace.stop();

        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }

    }

    public static class TestEntry implements Entry {
        public String rhubarb;
        public Integer count;

        public TestEntry(String aThing) {
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
