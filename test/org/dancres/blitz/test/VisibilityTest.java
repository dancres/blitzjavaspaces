package org.dancres.blitz.test;

import java.io.Serializable;

import java.rmi.MarshalledObject;

import junit.framework.Assert;
import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;

import net.jini.core.transaction.server.*;

import net.jini.space.AvailabilityEvent;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.remote.LocalTxnMgr;

import org.dancres.blitz.SpaceImpl;

public class VisibilityTest {
    public static void main(String args[]) {

        try {
            SpaceImpl mySpace = new SpaceImpl(new TxnGatewayImpl());
            LocalTxnMgr myMgr = new LocalTxnMgr(1, mySpace.getTxnControl());

            EntryMangler myMangler = EntryMangler.getMangler();
            TestEntry myEntry = new TestEntry();
            myEntry.init();

            MangledEntry myPackedEntry = myMangler.mangle(myEntry);

            EventListener myVis = new EventListener();
            EventListener myVisOnly = new EventListener();
            EventListener myNotify = new EventListener();

            mySpace.visibility(new MangledEntry[] {myPackedEntry}, null,
                               myVis,
                               Lease.FOREVER,
                               new MarshalledObject(new String("Here's a vis handback")), false);
            mySpace.visibility(new MangledEntry[] {myPackedEntry}, null,
                               myVisOnly,
                               Lease.FOREVER,
                               new MarshalledObject(new String("Here's a vis-only handback")), true);
            mySpace.notify(myPackedEntry, null, myNotify,
                           Lease.FOREVER,
                           new MarshalledObject(new String("Here's a handback")));

            for (int i = 0; i < 2; i++) {
                mySpace.write(myPackedEntry, null, Lease.FOREVER);
            }

            Assert.assertEquals(2, myVis.waitOnNotifyCount(2, 500));
            Assert.assertEquals(2, myVis.getAvailabilityCount());

            Assert.assertEquals(2, myVisOnly.waitOnNotifyCount(2, 500));
            Assert.assertEquals(2, myVisOnly.getAvailabilityCount());

            Assert.assertEquals(2, myNotify.waitOnNotifyCount(2, 500));
            Assert.assertEquals(0, myNotify.getAvailabilityCount());

            ServerTransaction myTxn = myMgr.newTxn();

            Assert.assertNotNull(mySpace.take(myPackedEntry, myTxn, 0));

            // Aborted take transaction causes a vis and avail change
            //
            myTxn.abort();

            Assert.assertEquals(3, myVis.waitOnNotifyCount(3, 500));
            Assert.assertEquals(3, myVis.getAvailabilityCount());

            Assert.assertEquals(3, myVisOnly.waitOnNotifyCount(3, 500));
            Assert.assertEquals(3, myVisOnly.getAvailabilityCount());

            Assert.assertEquals(2, myNotify.waitOnNotifyCount(2, 500));
            Assert.assertEquals(0, myNotify.getAvailabilityCount());

            ServerTransaction myTxn1 = myMgr.newTxn();
            ServerTransaction myTxn2 = myMgr.newTxn();

            Assert.assertNotNull(mySpace.read(myPackedEntry, myTxn1, 0));
            Assert.assertNotNull(mySpace.read(myPackedEntry, myTxn2, 0));

            // Because there are two transactions "stacked" on the same entry, releasing the first one generates
            // no events. Releasing the second one, causes a change in availability but not visibility
            //
            myTxn1.abort();

            myTxn2.commit();

            Assert.assertEquals(4, myVis.waitOnNotifyCount(4, 500));
            Assert.assertEquals(4, myVis.getAvailabilityCount());

            Assert.assertEquals(3, myVisOnly.waitOnNotifyCount(3, 500));
            Assert.assertEquals(3, myVisOnly.getAvailabilityCount());

            Assert.assertEquals(2, myNotify.waitOnNotifyCount(2, 500));
            Assert.assertEquals(0, myNotify.getAvailabilityCount());

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

    private static class EventListener implements RemoteEventListener,
                                                  Serializable {
        private int _notifyCount = 0;
        private int _availabilityCount = 0;

        public int getAvailabilityCount() {
            synchronized(this) {
                return _availabilityCount;
            }
        }

        public int waitOnNotifyCount(int aCount, long aWaitTime) {
            long myExpiry = System.currentTimeMillis() + aWaitTime;

            synchronized(this) {
                while (_notifyCount != aCount) {
                    long myNewWaitTime = myExpiry - System.currentTimeMillis();

                    if (myNewWaitTime <= 0)
                        return -1;

                    try {
                        wait(myNewWaitTime);
                    } catch (InterruptedException anIE) {
                    }
                }
            }

            return aCount;
        }

        public void notify(RemoteEvent anEvent) {

            synchronized(this) {
                ++_notifyCount;
            }

            try {
                if (anEvent instanceof AvailabilityEvent) {
                    synchronized(this) {
                        ++_availabilityCount;
                    }
                }

            } catch (Exception anE) {
                System.out.println("Got event but couldn't display it");
                anE.printStackTrace(System.out);
            }

            synchronized(this) {
                notify();
            }
        }
    }
}
