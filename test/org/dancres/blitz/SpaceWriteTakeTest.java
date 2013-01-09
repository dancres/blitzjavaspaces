package org.dancres.blitz;

import junit.framework.Assert;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.disk.Disk;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SpaceWriteTakeTest {
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
    public void writeTake() throws Exception {
        MangledEntry myPackedEntry = _mangler.mangle(new TestEntry().init());

        _space.write(myPackedEntry, null, Lease.FOREVER);

        MangledEntry myWild = _mangler.mangle(new TestEntry());

        Assert.assertNotNull(_space.read(myWild, null, Lease.FOREVER));

        Assert.assertNotNull(_space.take(myPackedEntry, null, Lease.FOREVER));

        myPackedEntry = _mangler.mangle(new TestEntry().init2());

        _space.write(myPackedEntry, null, Lease.FOREVER);

        Assert.assertNotNull(_space.take(myPackedEntry, null, Lease.FOREVER));

        myPackedEntry = _mangler.mangle(new TestEntry().init3());

        Assert.assertNull(_space.take(myPackedEntry, null, 5000));

        // We now want to make sure that when an entry is on-disk we still match it
        //
        _space.write(myPackedEntry, null, Lease.FOREVER);

        Disk.sync();

        Assert.assertNotNull(_space.take(myWild, null, Lease.FOREVER));
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
