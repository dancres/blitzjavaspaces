package entrysizer;

import net.jini.core.entry.Entry;

import org.dancres.blitz.mangler.EntrySizer;

/**
   This class demonstrates how to use the Blitz EntrySizer which can be
   used to give an approximate size for an Entry once it's marshalled and
   ready for storage in the Blitz backend.  This is useful for sizing heap,
   caches and disk storage (for a persistent or time-barrier-persistent
   Blitz).
 */
public class TestSizer {
    public static void main(String args[]) {
        try {
            /*
              Create an EntrySizer instance (EntrySizer is stateless)
             */
            EntrySizer mySizer = new EntrySizer();

            /*
              Create some example Entry's
             */
            Entry[] myEntrys = new Entry[] {new DummyEntry("a"),
                                            new DummyEntry("abcdefgh"),
                                            new DummyEntry(null)};

            /*
              For each example, get a size estimate from the Sizer.
             */
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
