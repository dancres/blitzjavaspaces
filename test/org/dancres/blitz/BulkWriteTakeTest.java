package org.dancres.blitz;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.Assert;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BulkWriteTakeTest {
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
    public void testBulkTake() throws Exception {
        ArrayList myMangled = new ArrayList();
        ArrayList myLeases = new ArrayList();

        for (int i = 0; i < 100; i++) {
            TestEntry myEntry = new TestEntry(Integer.toString(i));
            MangledEntry myPackedEntry = _mangler.mangle(myEntry);
            myMangled.add(myPackedEntry);
            myLeases.add(new Long(300000 + i));
        }

        List myLeaseResults = _space.write(myMangled, null, myLeases);

        MangledEntry myTemplate = _mangler.mangle(new TestEntry());

        Collection myTakes;
        int myTotal = 0;

        while ((myTakes = _space.take(new MangledEntry[] {myTemplate},
                null, 30000, 25)).size() != 0) {
            Iterator myTaken = myTakes.iterator();
            while (myTaken.hasNext()) {
                ++myTotal;
            }
        }

        Assert.assertEquals(100, myTotal);
    }

    public static class TestEntry implements Entry {
        public String rhubarb;
        public Integer count;

        public TestEntry(String aThing) {
        }

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
