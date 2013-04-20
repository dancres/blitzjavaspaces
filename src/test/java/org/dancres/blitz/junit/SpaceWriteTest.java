package org.dancres.blitz.junit;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.mangler.*;

import org.dancres.blitz.stats.InstanceCount;
import org.dancres.blitz.stats.Stat;
import org.dancres.blitz.stats.StatsBoard;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;


public class SpaceWriteTest {
    private SpaceImpl _space;
    private EntryMangler _mangler;

    @Before public void init() throws Exception {
        _space = new SpaceImpl(null);
        _mangler = new EntryMangler();
    }

    @After public void deinit() throws Exception {
        _space.stop();
    }

    @Test public void write() throws Exception {
        TestEntry myEntry = new TestEntry();
        myEntry.init();

        MangledEntry myPackedEntry = _mangler.mangle(myEntry);

        _space.write(myPackedEntry, null, Lease.FOREVER);

        Stat[] myStats = StatsBoard.get().getStats();

        for (int i = 0; i < myStats.length; i++) {
            if (myStats[i] instanceof InstanceCount) {
                InstanceCount myCount = (InstanceCount) myStats[i];

                if (myCount.getType().contains("Test")) {
                    Assert.assertEquals(1, myCount.getCount());
                }
            }
        }
    }

    public static class TestEntry implements Entry {
        public String rhubarb;
        public Integer count;
        public Integer meta;

        public TestEntry() {
        }

        public void init() {
            rhubarb = "blah";
            count = new Integer(5);
            // another = new Integer(6);
        }

        public String toString() {
            return super.toString() + ", " + rhubarb + ", " + count;
        }
    }
}
