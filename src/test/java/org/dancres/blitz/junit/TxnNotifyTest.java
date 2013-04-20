package org.dancres.blitz.junit;

import java.io.Serializable;

import java.rmi.MarshalledObject;

import junit.framework.Assert;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.server.*;

import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.RemoteEvent;

import net.jini.core.entry.Entry;

import org.dancres.blitz.*;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.remote.LocalTxnMgr;
import org.dancres.blitz.test.TxnGatewayImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TxnNotifyTest {
    private SpaceImpl _space;
    private EntryMangler _mangler;
    private LocalTxnMgr _mgr;

    @Before
    public void init() throws Exception {
        _space = new SpaceImpl(new TxnGatewayImpl());
        _mangler = new EntryMangler();
        _mgr = new LocalTxnMgr(1, _space.getTxnControl());
    }
    
    @After
    public void deinit() throws Exception {
        _space.stop();
    }
    
    @Test
    public void doNotify() throws Exception {
        TestEntry myEntry = new TestEntry().init();
        TestEntry myTemplate = new TestEntry();

        MangledEntry myPackedEntry = _mangler.mangle(myEntry);
        MangledEntry myPackedTemplate = _mangler.mangle(myTemplate);

        EventListener myNullListener = new EventListener();
        EventListener myTxnListener = new EventListener();

        _space.notify(myPackedTemplate, null, myNullListener,
                Lease.FOREVER,
                new MarshalledObject(new Integer(12345)));

        LocalTxnMgr myMgr = new LocalTxnMgr(1, _space.getTxnControl());
        ServerTransaction myTxn = myMgr.newTxn();

        _space.notify(myPackedTemplate, myTxn, myTxnListener,
                Lease.FOREVER,
                new MarshalledObject(new Integer(67890)));

        // Will be caught only by txn listener
        //
        _space.write(myPackedEntry, myTxn, Lease.FOREVER);

        Assert.assertEquals(1, myTxnListener.waitOnCount(1, 500));

        // Will be caught by both listeners
        //
        _space.write(myPackedEntry, null, Lease.FOREVER);

        Assert.assertEquals(2, myTxnListener.waitOnCount(2, 500));
        Assert.assertEquals(1, myNullListener.waitOnCount(1, 500));

        // Txn listener will now close out with the transaction, null sees another entry
        //
        myTxn.commit();

        Assert.assertEquals(2, myNullListener.waitOnCount(2, 500));

        // Will only be seen by null listener
        //
        _space.write(myPackedEntry, null, Lease.FOREVER);

        Assert.assertEquals(3, myNullListener.waitOnCount(3, 500));
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

        public int getCount() {
            synchronized(this) {
                return _notifyCount;
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
