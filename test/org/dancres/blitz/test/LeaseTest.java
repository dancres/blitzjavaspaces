package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.server.*;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.remote.TxnMgr;

public class LeaseTest {
    public static void main(String args[]) {

        try {
            System.out.println("Start space");

            LocalSpace myLocalSpace = new LocalSpace(new TxnGatewayImpl());

            if (args.length != 0) {
                new LeaseTest().secondPhase(myLocalSpace);
            } else {
                new LeaseTest().firstPhase(myLocalSpace);
            }

            myLocalSpace.stop();

        } catch (Exception anE) {
            anE.printStackTrace(System.err);
        }
    }

    public void firstPhase(LocalSpace aSpace) throws Exception {
        System.out.println("First phase");

        JavaSpace mySpace = aSpace.getProxy();

        TestEntry myEntry = new TestEntry();

        myEntry.theDate = new Integer(1000);
        myEntry.theOid = new Long(12345);
        myEntry.isCalc = new Boolean(false);

        mySpace.write(myEntry, null, 60000);

        myEntry = (TestEntry) mySpace.read(new TestEntry(12345), null, 1000);

        System.out.println("Read: " + myEntry);

        myEntry.isCalc = new Boolean(true);

        mySpace.write(myEntry, null, Lease.FOREVER);
    }

    public void secondPhase(LocalSpace aSpace) throws Exception {
        System.out.println("Second phase");

        JavaSpace mySpace = aSpace.getProxy();

        TestEntry myEntry = new TestEntry(56789);

        myEntry.theDate = new Integer(1001);
        myEntry.isCalc = new Boolean(false);

        mySpace.write(myEntry, null, 60000);

        myEntry = new TestEntry();
        myEntry.theDate = new Integer(1001);

        myEntry = (TestEntry) mySpace.read(myEntry, null, 1000);

        System.out.println("Read date: " + myEntry);

        myEntry.isCalc = new Boolean(true);

        mySpace.write(myEntry, null, Lease.FOREVER);


        // First let's find out what's in the space
        System.out.println("what have we got?");
        TxnMgr myMgr = new TxnMgr(1, aSpace.getTxnControl());

        ServerTransaction myTxn = myMgr.newTxn();

        while (true) {
            Entry myDebugEntry = aSpace.take(null, myTxn, 1000);

            if (myDebugEntry == null)
                break;
            else
                System.out.println(myDebugEntry);
        }

        myMgr.abort(myTxn.id);
        
        System.out.println("Waiting out");
        for (int i = 0; i < 60; i++) {
            Thread.sleep(1000);
            System.out.print(".");
            System.out.flush();
        }
        System.out.println("");

        myEntry = (TestEntry) mySpace.read(new TestEntry(56789), null, 1000);

        System.out.println("Read oid: " + myEntry);

        myEntry = (TestEntry) mySpace.read(new TestEntry(12345), null, 1000);

        System.out.println("Read oid: " + myEntry);

        myEntry = new TestEntry();
        myEntry.theDate = new Integer(1000);

        myEntry = (TestEntry) mySpace.read(myEntry, null, 1000);

        System.out.println("Read date: " + myEntry);

        while (true) {

            myEntry = (TestEntry) mySpace.take(null, null, 1000);

            if (myEntry != null)
                System.out.println("Dump: " + myEntry);
            else
                break;
        }
    }

    public static class TestEntry implements Entry {
        public Integer theDate;
        public Long theOid;
        public Boolean isCalc;

        public TestEntry() {
        }

        public TestEntry(long anOid) {
            theOid = new Long(anOid);
        }

        public String toString() {
            return "TestEntry: " + theDate + ", " + theOid + ", " +
                isCalc;
        }
    }
}