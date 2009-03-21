package org.dancres.blitz.mangler;

import net.jini.core.entry.Entry;

public class EntryManglerTest {
    private EntryManglerTest() {
    }

    private void test(Entry anEntry) {
        try {
            EntryMangler myMangler = new EntryMangler();
            MangledEntry myEntry = myMangler.mangle(anEntry);
            
            myEntry.dump(System.out);

            Entry myNewEntry = myMangler.unMangle(myEntry);

            System.out.println("Unpacked result....");
            System.out.println(myNewEntry);

            long myStart = System.currentTimeMillis();

            for (int i = 0; i < 10000; i++) {
                if (myMangler.mangle(anEntry) == null)
                    throw new RuntimeException();
            }

            long myEnd = System.currentTimeMillis();

            System.out.println("Time to mangle 10000: " + (myEnd - myStart));

            myStart = System.currentTimeMillis();

            for (int i = 0; i < 10000; i++) {
                myMangler.unMangle(myEntry);
            }

            myEnd = System.currentTimeMillis();

            System.out.println("Time to unmangle 10000: " + (myEnd - myStart));

        } catch (Exception anE) {
            System.err.println("Failed");
            anE.printStackTrace(System.err);
        }
    }

    public static void main(String args[]) {
        TestEntry myEntry = new TestEntry();
        myEntry.init();

        new EntryManglerTest().test(myEntry);
    }

    private static class TestEntry implements Entry {
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
}
