package org.dancres.blitz;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import net.jini.core.entry.Entry;

import org.dancres.blitz.mangler.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BulkWriteTest {
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
    public void testWrite() throws Exception {
        ArrayList myMangled = new ArrayList();
        ArrayList myLeases = new ArrayList();

        for (int i = 0; i < 100; i++) {
            TestEntry myEntry = new TestEntry(Integer.toString(i));
            MangledEntry myPackedEntry = _mangler.mangle(myEntry);
            myMangled.add(myPackedEntry);
            myLeases.add(new Long(300000 + i));
        }

        long myCurrentTime = System.currentTimeMillis();

        List myLeaseResults = _space.write(myMangled, null, myLeases);

        for (int i = 0; i < myLeaseResults.size(); i++) {
            long myExpiry = ((WriteTicket) myLeaseResults.get(i)).getExpirationTime();

            Assert.assertTrue((myExpiry - myCurrentTime) > 300000);
        }

        int myEntryCount = 0;
        TestEntry myEntry = new TestEntry(null);
        MangledEntry myPackedEntry = _mangler.mangle(myEntry);

        while (_space.take(myPackedEntry, null, 1) != null)
            myEntryCount++;

        Assert.assertEquals(100, myEntryCount);
    }

    public static class TestEntry implements Entry {
        public String rhubarb;
        public Integer count;

        public TestEntry(String aThing) {
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
