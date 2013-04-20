package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;
import java.io.Serializable;

import net.jini.space.JavaSpace;
import net.jini.core.entry.Entry;

/**
 */
public class NigeTest {
    public static void main(String args[]) throws Exception {
        System.setSecurityManager(new RMISecurityManager());

        new NigeTest().test();
    }

    private JavaSpace space;

    private void test() throws Exception {
        Lookup myLookup = new Lookup(JavaSpace.class);
        space = (JavaSpace) myLookup.getService();

        testWriteTake();
        testWriteRead();
        testMatch();
    }

    public static class TestEntry implements Entry {
        public Integer reference;
        public String name;
        public Serializable box;
    }


    public void testWriteTake() {
        TestEntry entry = new TestEntry();

        entry.reference = new Integer(23);
        entry.name =
            "A longer string that represent a cached http address for example - 1234567890";
        entry.box = null;

        long soak = 100001;

        long start = System.currentTimeMillis();

        for (long i = 0; i < soak; i++) {
            try {
                space.write(entry, null, 10 * 1000);
                space.take(entry, null, 0);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            if (i % 10000 == 0) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println(i + "Write/Take :" + elapsed);
            }
        }
    }

    public void testWriteRead() {
        TestEntry entry = new TestEntry();

        entry.reference = new Integer(23);
        entry.name = "Steam";
        entry.box = null;

        long soak = 100001;
        long start = System.currentTimeMillis();

        for (long i = 0; i < soak; i++) {
            try {
                space.write(entry, null, 10 * 1000);
                space.read(entry, null, 0);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            if (i % 10000 == 0) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println(i + "Write/Read :" + elapsed);
            }
        }
    }

    public void testMatch() {
        // write 1000 entries into the space and match randomly
        TestEntry entry = new TestEntry();

        entry.reference = new Integer(0);
        entry.name = "Steam";
        entry.box = null;

        long soak = 100001;
        long start = System.currentTimeMillis();

        // write the objects in
        long matchables = 100;
        for (long i = 0; i < matchables; i++) {
            entry.reference = new Integer((int) matchables);
            try {
                space.write(entry, null,
                    60 * 60 * 1000);    // make a decent length lease
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }


        for (long i = 0; i < soak; i++) {
            entry.reference = new Integer((int) (Math.random() * matchables));
            try {
                space.read(entry, null, 0);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            if (i % 10000 == 0) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println(i + "Matching :" + elapsed);
            }
        }
    }
}
