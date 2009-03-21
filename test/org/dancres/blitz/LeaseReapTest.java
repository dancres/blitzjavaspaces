package org.dancres.blitz;

import java.io.Serializable;
import java.rmi.MarshalledObject;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.lease.SpaceUID;

import org.dancres.blitz.disk.Disk;

public class LeaseReapTest {
    /**
       Set Reaper to run every 60 seconds

       Write 3 entries, lease expiries: 30, 90, 360

       sync

       Wait for 2 minutes so reaper does multiple passes and verify that
       deletion wasn't done more than once.

       stop - includes sync, verify that two entries were deleted and one
       was saved to disk.

       <pre>

[dan@rogue space]$ ant testreaper
Buildfile: build.xml

init:

compile:

testreaper:
     [java] Start space
     [java] Jun 10, 2004 12:49:53 PM org.dancres.blitz.config.ConfigurationFactory load
     [java] INFO: Loading config from: config/blitz.config
     [java] Starting debugger on: 12345
     [java] Jun 10, 2004 12:49:53 PM org.dancres.blitz.txn.TxnManager <init>
     [java] INFO: Doing recovery...
     [java] Jun 10, 2004 12:49:53 PM org.dancres.blitz.txn.PersistentPersonality <init>
     [java] SEVERE: PersistentPersonality
     [java] Jun 10, 2004 12:49:53 PM org.dancres.blitz.txn.PersistentPersonality <init>
     [java] INFO: Max logs before sync: 10000
     [java] Jun 10, 2004 12:49:53 PM org.dancres.blitz.txn.PersistentPersonality <init>
     [java] INFO: Reset log stream: false
     [java] Jun 10, 2004 12:49:53 PM org.dancres.blitz.txn.PersistentPersonality <init>
     [java] INFO: Write barrier window: 20
     [java] Jun 10, 2004 12:49:53 PM org.dancres.blitz.txn.PersistentPersonality <init>
     [java] SEVERE: *** Using concurrent batcher ***
     [java] Jun 10, 2004 12:49:54 PM org.dancres.blitz.txn.TxnManager <init>
     [java] INFO: Recovery complete...
     [java] Jun 10, 2004 12:49:54 PM org.dancres.blitz.lease.LeaseReaper <init>
     [java] INFO: Reaper::EntryReaper:theReapTime: 60000
     [java] Jun 10, 2004 12:49:54 PM org.dancres.blitz.lease.LeaseReaper <init>
     [java] INFO: Reaper::EntryReaper:Filter = org.dancres.blitz.TxnReapFilter@1f48262
Jun 10, 2004 12:49:54 PM org.dancres.blitz.lease.LeaseReaper run
INFO: Reaper started up: EntryReaper
     [java] Prepare entry
     [java] init'd entry
     [java] Do writes
     [java] Flush
Write a lease
Write a lease
Write a lease
LeaseTracker bringing out the dead for: org.dancres.blitz.LeaseReapTest$TestEntry
Scanning bucket
HasLock: false
Scanning bucket
Scanning bucket
LeaseTracker claimed the dead for: org.dancres.blitz.LeaseReapTest$TestEntry
LeaseTracker bringing out the dead for: org.dancres.blitz.LeaseReapTest$TestEntry
Scanning bucket
Scanning bucket
Scanning bucket
HasLock: false
LeaseTracker claimed the dead for: org.dancres.blitz.LeaseReapTest$TestEntry
     [java] Do stop
     [java] Interrupting debug thread
Jun 10, 2004 12:51:55 PM org.dancres.blitz.lease.LeaseReaper run
INFO: Reaper exited: EntryReaper
Delete a lease
Lease killed
Delete a lease
Lease killed
     [java] Jun 10, 2004 12:51:55 PM org.dancres.blitz.disk.WriteDaemon halt
     [java] SEVERE: WriteDaemon doing halt
     [java] Jun 10, 2004 12:51:55 PM org.dancres.blitz.disk.WriteDaemon halt
     [java] SEVERE: WriteDaemon done halt
     [java] Dumping stats
     [java] 1, Memory: 2031616 of: 66650112
     [java] 2, Active Txns: 0
     [java] 3, Takes:java.lang.Object = 0 (3)
     [java] 4, Reads:java.lang.Object = 0 (4)
     [java] 5, Writes:java.lang.Object = 0 (5)
     [java] 6, Total instance of type java.lang.Object is 0
     [java] 7, Takes:org.dancres.blitz.LeaseReapTest$TestEntry = 0 (7)
     [java] 8, Reads:org.dancres.blitz.LeaseReapTest$TestEntry = 0 (8)
     [java] 9, Writes:org.dancres.blitz.LeaseReapTest$TestEntry = 3 (9)
     [java] 10, Total instance of type org.dancres.blitz.LeaseReapTest$TestEntry is 1
     [java] 11, Types: org.dancres.blitz.LeaseReapTest$TestEntry,
     [java] 12, Blocking reads: 0, takes: 0

BUILD SUCCESSFUL
Total time: 2 minutes 3 seconds
</pre>

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

            System.out.println("Do writes");

            mySpace.write(myPackedEntry, null, 30 * 1000);
            mySpace.write(myPackedEntry, null, 90 * 1000);
            mySpace.write(myPackedEntry, null, 360 * 1000);

            System.out.println("Flush");

            Disk.sync();

            try {
                Thread.sleep(120 * 1000);
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
