package org.dancres.blitz;

import java.io.Serializable;

import java.rmi.MarshalledObject;

import junit.framework.Assert;
import net.jini.core.entry.Entry;

import net.jini.core.lease.Lease;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;

import org.dancres.blitz.mangler.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SpaceNotifyTest {
    private SpaceImpl _space;
    private EntryMangler _mangler;

    @Before
    public void init() throws Exception {
        _space = new SpaceImpl(null);
        _mangler = new EntryMangler();
    }

    @After
    public void deinit() throws Exception {
        _space.stop();
    }

    @Test
    public void test() throws Exception {
        TestEntry myEntry = new TestEntry();
        myEntry.init();

        MangledEntry myPackedEntry = _mangler.mangle(myEntry);

        EventListener myListener = new EventListener();
        _space.notify(myPackedEntry, null, myListener,
                Lease.FOREVER,
                new MarshalledObject(new String("Here's a handback")));

        System.out.println("Do write");

        for (int i = 0; i < 3; i++) {
            _space.write(myPackedEntry, null, Lease.FOREVER);

            Assert.assertEquals(i + 1, myListener.waitOnCount(i + 1, 500));
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
