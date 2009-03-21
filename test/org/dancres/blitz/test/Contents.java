package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.SpaceImpl;
import org.dancres.blitz.EntryView;
import org.dancres.blitz.EntryChit;

import java.util.ArrayList;
import java.util.Iterator;

/**
   Note that only the getView with a true/hold locks will contribute to
   stats on reads because only they create and log txnops.
 */
public class Contents {
    private static final int TOTAL_ENTRIES = 10;

    public static void main(String args[]) {

        try {
            System.out.println("Start space");

            SpaceImpl mySpace = new SpaceImpl(null);

            System.out.println("Prepare entry");

            EntryMangler myMangler = new EntryMangler();

            System.out.println("init'd entry");

            System.out.println("Do write: " + TOTAL_ENTRIES);

            long myStart = System.currentTimeMillis();

            for (int i = 0;i < TOTAL_ENTRIES; i++) {
                LoadEntry myEntry = new LoadEntry(Integer.toString(i));
                MangledEntry myPackedEntry = myMangler.mangle(myEntry);

                mySpace.write(myPackedEntry, null, Lease.FOREVER);
            }

            long myEnd = System.currentTimeMillis();

            System.out.println("Writes completed: " + (myEnd - myStart));

            System.out.println("Contents - no locks");

            EntryView myView =
                mySpace.getView(new MangledEntry[] {MangledEntry.NULL_TEMPLATE},
                                null, false, Long.MAX_VALUE);

            EntryChit myChit;

            int myFound = 0;

            while ((myChit = myView.next()) != null) {
                Entry myResult = myMangler.unMangle(myChit.getEntry());

                if (myResult != null)
                    ++myFound;
            }

            myView.close();

            if (myFound != TOTAL_ENTRIES)
                throw new RuntimeException("Didn't get full number of Entry's");

            System.out.println("Contents - no locks with limit");

            myView =
                mySpace.getView(new MangledEntry[]{MangledEntry.NULL_TEMPLATE},
                    null, false, 5);

            myFound = 0;

            while ((myChit = myView.next()) != null) {
                Entry myResult = myMangler.unMangle(myChit.getEntry());

                if (myResult != null)
                    ++myFound;
            }

            myView.close();

            if (myFound != 5)
                throw new RuntimeException("Didn't get full number of Entry's");

            System.out.println("Contents - with locks");

            myView =
                mySpace.getView(new MangledEntry[] {MangledEntry.NULL_TEMPLATE},
                                null, true, Long.MAX_VALUE);

            myFound = 0;

            while ((myChit = myView.next()) != null) {
                Entry myResult = myMangler.unMangle(myChit.getEntry());

                if (myResult != null)
                    ++myFound;
            }

            myView.close();

            if (myFound != TOTAL_ENTRIES)
                throw new RuntimeException("Didn't get full number of Entries");

            for (int i = 0;i < TOTAL_ENTRIES; i++) {
                DummyEntry myEntry = new DummyEntry(Integer.toString(i));
                MangledEntry myPackedEntry = myMangler.mangle(myEntry);

                mySpace.write(myPackedEntry, null, Lease.FOREVER);
            }

            System.out.println("Multi-template match");

            MangledEntry[] myTemplates =
                new MangledEntry[] {myMangler.mangle(new DummyEntry()),
                                    myMangler.mangle(new LoadEntry())};

            myView =
                mySpace.getView(myTemplates, null, false, Long.MAX_VALUE);

            myFound = 0;

            ArrayList myMatches = new ArrayList();

            while ((myChit = myView.next()) != null) {
                Entry myResult = myMangler.unMangle(myChit.getEntry());
                myMatches.add(myResult);

                ++myFound;
            }

            myView.close();

            if (myFound != (2 * TOTAL_ENTRIES))
                throw new RuntimeException("Failed to get all Entries");

            // Lock out all matches
            myView =
                mySpace.getView(new MangledEntry[] {MangledEntry.NULL_TEMPLATE},
                                null, true, Long.MAX_VALUE);

            while ((myChit = myView.next()) != null) {
                Entry myResult = myMangler.unMangle(myChit.getEntry());
            }

            // See if we can take them
            Iterator myTakes = myMatches.iterator();

            while (myTakes.hasNext()) {
                Entry myTakeTmpl = (Entry) myTakes.next();

                if (mySpace.take(myMangler.mangle(myTakeTmpl),
                        null, 0) != null)
                    throw new RuntimeException("Got one (shouldn't): " + myTakeTmpl);
            }

            myView.close();

            myTakes = myMatches.iterator();

            while (myTakes.hasNext()) {
                Entry myTakeTmpl = (Entry) myTakes.next();

                if (mySpace.takeIfExists(myMangler.mangle(myTakeTmpl),
                        null, 0) == null)
                    throw new RuntimeException("Missed one: " + myTakeTmpl);
            }

            for (int i = 0;i < TOTAL_ENTRIES; i++) {
                LoadEntry myEntry = new LoadEntry(Integer.toString(i));
                MangledEntry myPackedEntry = myMangler.mangle(myEntry);

                mySpace.write(myPackedEntry, null, Lease.FOREVER);
            }

            for (int i = 0;i < TOTAL_ENTRIES; i++) {
                DummyEntry myEntry = new DummyEntry(Integer.toString(i));
                MangledEntry myPackedEntry = myMangler.mangle(myEntry);

                mySpace.write(myPackedEntry, null, Lease.FOREVER);
            }

            for (int i = 0;i < TOTAL_ENTRIES; i++) {
                MyInheriting myEntry = new MyInheriting(Integer.toString(i),
                                                        Integer.toString(i));

                MangledEntry myPackedEntry = myMangler.mangle(myEntry);

                mySpace.write(myPackedEntry, null, Lease.FOREVER);
            }

            System.out.println("My inheriting test");

            Entry myInheritTemplate = new LoadEntry(null);
            MangledEntry myInheritMangled = myMangler.mangle(myInheritTemplate);

            myView =
                mySpace.getView(new MangledEntry[] {myInheritMangled},
                                null, true, Long.MAX_VALUE);

            myFound = 0;

            while ((myChit = myView.next()) != null) {
                Entry myResult = myMangler.unMangle(myChit.getEntry());
                if (myResult != null)
                    ++myFound;
            }

            myView.close();

            if (myFound != (TOTAL_ENTRIES * 2))
                throw new RuntimeException("Inheritance isn't working");

            System.out.println("My split test");

            Entry mySepTemplate = new DummyEntry(null);
            MangledEntry mySepMangled = myMangler.mangle(mySepTemplate);

            myView =
                mySpace.getView(new MangledEntry[] {mySepMangled},
                                null, true, Long.MAX_VALUE);

            myFound = 0;

            while ((myChit = myView.next()) != null) {
                Entry myResult = myMangler.unMangle(myChit.getEntry());

                if (myResult != null)
                    ++myFound;
            }

            myView.close();

            if (myFound != TOTAL_ENTRIES)
                throw new RuntimeException("Didn't get right number of filtered entries");

            System.out.println("Do stop");

            mySpace.stop();

        } catch (Exception anE) {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }
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
