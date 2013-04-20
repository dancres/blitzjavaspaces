package org.dancres.blitz.junit;

import junit.framework.Assert;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.mangler.*;

import org.dancres.blitz.disk.Disk;
import org.dancres.blitz.disk.DiskTxn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NullTakeTest {
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
    public void testNullTake() throws Exception {
            MangledEntry myPackedEntry =
                _mangler.mangle(new TestEntry().init());

        Assert.assertNull(_space.take(MangledEntry.NULL_TEMPLATE, null, 100));

        _space.write(myPackedEntry, null, Lease.FOREVER);

        Assert.assertNotNull(_space.read(MangledEntry.NULL_TEMPLATE, null, Lease.FOREVER));

        Assert.assertNotNull(_space.take(MangledEntry.NULL_TEMPLATE, null, Lease.FOREVER));
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

        public TestEntry init2() {
            rhubarb = "blahblah";
            count = new Integer(5);

            return this;
        }

        public TestEntry init3() {
            rhubarb = "blahh";
            count = new Integer(5);

            return this;
        }

        public String toString() {
            return super.toString() + ", " + rhubarb + ", " + count;
        }
    }
}
