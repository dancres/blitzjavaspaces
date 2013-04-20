package org.dancres.blitz.test;

import net.jini.core.entry.Entry;

import org.dancres.blitz.mangler.EntrySizer;

public class TestSizer {
    public static void main(String args[]) {
        try {
            EntrySizer mySizer = new EntrySizer();

            Entry[] myEntrys = new Entry[] {new DummyEntry("a"),
                                            new DummyEntry("abcdefgh"),
                                            new DummyEntry(null)};

            for (int i = 0; i < myEntrys.length; i++) {
                System.out.println("Size of: " + myEntrys[i] + " = " +
                                   mySizer.computeSize(myEntrys[i]));
            }
        } catch (Exception anE) {
            System.err.println("Whoops, didn't work");
            anE.printStackTrace(System.err);
        }
    }
}
