package org.dancres.blitz;

import java.io.Serializable;
import java.rmi.MarshalledObject;

import junit.framework.Assert;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;

import net.jini.core.transaction.server.*;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.lease.SpaceUID;

import org.dancres.blitz.remote.LocalTxnMgr;
import org.dancres.blitz.test.TxnGatewayImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LeaseTest {
    private SpaceImpl _space;
    private EntryMangler _mangler;

    @Before
    public void init() throws Exception {
        _mangler = new EntryMangler();
        _space = new SpaceImpl(new TxnGatewayImpl());
    }

    @After
    public void deinit() throws Exception {
        _space.stop();
    }

    @Test
    public void testNotify() throws Exception {
        TestEntry myEntry = new TestEntry();
        myEntry.init();

        MangledEntry myPackedEntry = _mangler.mangle(new TestEntry());
        EventListener myListener = new EventListener();

        RegTicket myTicket =
                _space.notify(myPackedEntry, null, myListener,
                        1000,
                        new MarshalledObject(new Integer(12345)));

        SpaceUID myNotifyUID = myTicket.getUID();
        SpaceUID myEntryUID =
                _space.write(myPackedEntry, null, 1000).getUID();

        Assert.assertEquals(1, myListener.waitOnCount(1, 500));

        _space.getLeaseControl().renew(myEntryUID, 1000);
        _space.getLeaseControl().renew(myNotifyUID, 1000);

        _space.write(myPackedEntry, null, Lease.FOREVER);

        // Should get another notify
        //
        Assert.assertEquals(2, myListener.waitOnCount(2, 500));

        _space.getLeaseControl().cancel(myEntryUID);
        _space.getLeaseControl().cancel(myNotifyUID);

        Thread.sleep(2000);

        // Should only be one entry left at this point
        //
        int myEntryCount = 0;

        while (_space.take(myPackedEntry, null, 1) != null)
            myEntryCount++;

        Assert.assertEquals(1, myEntryCount);

        LocalTxnMgr myMgr = new LocalTxnMgr(1, _space.getTxnControl());

        ServerTransaction myTxn = myMgr.newTxn();

        myEntryUID = _space.write(myPackedEntry, myTxn, 50000).getUID();

        _space.getLeaseControl().renew(myEntryUID, 500000);

        myTxn.commit();

        // Would be a notify at txn commit unless the notify was gone, which it should be
        //
        Assert.assertEquals(2, myListener.waitOnCount(2, 500));

        // Should be one entry to take
        //
        myEntryCount = 0;

        while (_space.take(myPackedEntry, null, 1) != null)
            myEntryCount++;

        Assert.assertEquals(1, myEntryCount);
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

        public void notify(RemoteEvent anEvent) {
            synchronized(this) {
                _notifyCount++;
                notify();
            }
        }

        public int waitOnCount(int aCount, long aWaitTime) {
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
    }
}
