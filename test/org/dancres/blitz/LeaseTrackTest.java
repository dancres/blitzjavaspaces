package org.dancres.blitz;

import java.io.Serializable;
import java.rmi.MarshalledObject;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.lease.SpaceUID;

import org.dancres.blitz.disk.Disk;

public class LeaseTrackTest {
    /**
       Write 3 entries, saving leases

       sync

       Update entry 0 lease

       sync

       Cancel entry 0 lease

       sync

       stop
     */
    public static void main(String args[]) {
        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();
            TestEntry myEntry = new TestEntry();
            myEntry.init();

            System.out.println("init'd entry");
            MangledEntry myPackedEntry = myMangler.mangle(new TestEntry());

            System.out.println("Do write");
            SpaceUID[] myUIDs = new SpaceUID[3];

            for (int i = 0; i < 3; i++ ) {
                myUIDs[i] = mySpace.write(myPackedEntry, null, 50000).getUID();
            }

            Disk.sync();

            System.out.println("Renew entry");
            mySpace.getLeaseControl().renew(myUIDs[0], 500000);

            Disk.sync();
            System.out.println("Cancel entry");
            mySpace.getLeaseControl().cancel(myUIDs[0]);

            Disk.sync();
            System.out.println("Settling");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException anIE) {
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
