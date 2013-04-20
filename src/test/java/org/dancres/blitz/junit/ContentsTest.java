package org.dancres.blitz.junit;

import junit.framework.Assert;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.EntryView;
import org.dancres.blitz.EntryChit;
import org.dancres.blitz.test.DummyEntry;
import org.dancres.blitz.test.LoadEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;

/**
   Note that only the getView with a true/hold locks will contribute to
   stats on reads because only they create and log txnops.
 */
public class ContentsTest {
    private static final int TOTAL_ENTRIES = 10;

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
    public void execute() throws Exception {
        long myStart = System.currentTimeMillis();

        for (int i = 0;i < TOTAL_ENTRIES; i++) {
            LoadEntry myEntry = new LoadEntry(Integer.toString(i));
            MangledEntry myPackedEntry = _mangler.mangle(myEntry);

            _space.write(myPackedEntry, null, Lease.FOREVER);
        }

        long myEnd = System.currentTimeMillis();

        // Contents - no locks

        EntryView myView =
                _space.getView(new MangledEntry[] {MangledEntry.NULL_TEMPLATE},
                        null, false, Long.MAX_VALUE);

        EntryChit myChit;

        int myFound = 0;

        while ((myChit = myView.next()) != null) {
            Entry myResult = _mangler.unMangle(myChit.getEntry());

            if (myResult != null)
                ++myFound;
        }

        myView.close();

        Assert.assertEquals(TOTAL_ENTRIES, myFound);

        // Contents - no locks with limit

        myView =
                _space.getView(new MangledEntry[]{MangledEntry.NULL_TEMPLATE},
                        null, false, 5);

        myFound = 0;

        while ((myChit = myView.next()) != null) {
            Entry myResult = _mangler.unMangle(myChit.getEntry());

            if (myResult != null)
                ++myFound;
        }

        myView.close();

        Assert.assertEquals(5, myFound);

        // Contents - with locks

        myView =
                _space.getView(new MangledEntry[] {MangledEntry.NULL_TEMPLATE},
                        null, true, Long.MAX_VALUE);

        myFound = 0;

        while ((myChit = myView.next()) != null) {
            Entry myResult = _mangler.unMangle(myChit.getEntry());

            if (myResult != null)
                ++myFound;
        }

        myView.close();

        Assert.assertEquals(TOTAL_ENTRIES, myFound);

        for (int i = 0;i < TOTAL_ENTRIES; i++) {
            DummyEntry myEntry = new DummyEntry(Integer.toString(i));
            MangledEntry myPackedEntry = _mangler.mangle(myEntry);

            _space.write(myPackedEntry, null, Lease.FOREVER);
        }

        System.out.println("Multi-template match");

        MangledEntry[] myTemplates =
                new MangledEntry[] {_mangler.mangle(new DummyEntry()),
                        _mangler.mangle(new LoadEntry())};

        myView =
                _space.getView(myTemplates, null, false, Long.MAX_VALUE);

        myFound = 0;

        ArrayList myMatches = new ArrayList();

        while ((myChit = myView.next()) != null) {
            Entry myResult = _mangler.unMangle(myChit.getEntry());
            myMatches.add(myResult);

            ++myFound;
        }

        myView.close();

        Assert.assertEquals(2 * TOTAL_ENTRIES, myFound);

        // Lock out all matches
        myView =
                _space.getView(new MangledEntry[] {MangledEntry.NULL_TEMPLATE},
                        null, true, Long.MAX_VALUE);

        while ((myChit = myView.next()) != null) {
            Entry myResult = _mangler.unMangle(myChit.getEntry());
        }

        // See if we can take them
        Iterator myTakes = myMatches.iterator();

        while (myTakes.hasNext()) {
            Entry myTakeTmpl = (Entry) myTakes.next();

            Assert.assertNull(_space.take(_mangler.mangle(myTakeTmpl), null, 0));
        }

        myView.close();

        myTakes = myMatches.iterator();

        while (myTakes.hasNext()) {
            Entry myTakeTmpl = (Entry) myTakes.next();

            Assert.assertNotNull(_space.takeIfExists(_mangler.mangle(myTakeTmpl), null, 0));
        }

        for (int i = 0;i < TOTAL_ENTRIES; i++) {
            LoadEntry myEntry = new LoadEntry(Integer.toString(i));
            MangledEntry myPackedEntry = _mangler.mangle(myEntry);

            _space.write(myPackedEntry, null, Lease.FOREVER);
        }

        for (int i = 0;i < TOTAL_ENTRIES; i++) {
            DummyEntry myEntry = new DummyEntry(Integer.toString(i));
            MangledEntry myPackedEntry = _mangler.mangle(myEntry);

            _space.write(myPackedEntry, null, Lease.FOREVER);
        }

        for (int i = 0;i < TOTAL_ENTRIES; i++) {
            MyInheriting myEntry = new MyInheriting(Integer.toString(i),
                    Integer.toString(i));

            MangledEntry myPackedEntry = _mangler.mangle(myEntry);

            _space.write(myPackedEntry, null, Lease.FOREVER);
        }

        // Inheriting test

        Entry myInheritTemplate = new LoadEntry(null);
        MangledEntry myInheritMangled = _mangler.mangle(myInheritTemplate);

        myView =
                _space.getView(new MangledEntry[] {myInheritMangled},
                        null, true, Long.MAX_VALUE);

        myFound = 0;

        while ((myChit = myView.next()) != null) {
            Entry myResult = _mangler.unMangle(myChit.getEntry());
            if (myResult != null)
                ++myFound;
        }

        myView.close();

        Assert.assertEquals(TOTAL_ENTRIES * 2, myFound);

        // Split test

        Entry mySepTemplate = new DummyEntry(null);
        MangledEntry mySepMangled = _mangler.mangle(mySepTemplate);

        myView =
                _space.getView(new MangledEntry[] {mySepMangled},
                        null, true, Long.MAX_VALUE);

        myFound = 0;

        while ((myChit = myView.next()) != null) {
            Entry myResult = _mangler.unMangle(myChit.getEntry());

            if (myResult != null)
                ++myFound;
        }

        myView.close();

        Assert.assertEquals(TOTAL_ENTRIES, myFound);
    }

    public static class MyInheriting extends LoadEntry {
        public String yetAnother;

        public MyInheriting() {
        }

        public MyInheriting(String a, String b) {
            super(a);
            yetAnother = b;
        }
    }
}
