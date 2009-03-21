package org.dancres.blitz;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import org.dancres.blitz.mangler.*;

import org.dancres.blitz.entry.EntryRepositoryFactory;

import org.dancres.blitz.disk.DiskTxn;

public class SpaceBlockTest implements Runnable {
    private SpaceImpl theSpace;

    private SpaceBlockTest() {
    }

    public void run() {
        try {
            System.out.println("Prepare template");

            EntryMangler myMangler = new EntryMangler();
            MangledEntry myPackedTemplate =
                myMangler.mangle(new TestEntry().init());

            System.out.println("Taking....." + Thread.currentThread());
            MangledEntry myResult = theSpace.take(myPackedTemplate, null,
                                                  20000);

            if (myResult != null)
                System.out.println(myMangler.unMangle(myResult) + " " + Thread.currentThread());
            else
                System.out.println("No entry found :(" + Thread.currentThread());

        } catch (Exception anException) {
            System.out.println("Taker failed");
            anException.printStackTrace(System.out);
        }
    }

    public void test() {
        try {
            System.out.println("Start space");

            theSpace = new SpaceImpl(null);

            System.out.println("Prepare entry");
            EntryMangler myMangler = new EntryMangler();
            MangledEntry myPackedEntry =
                myMangler.mangle(new TestEntry().init());

            System.out.println("Start taker");
            Thread myThread = new Thread(this);
            myThread.start();

            System.out.println("Start taker");
            myThread = new Thread(this);
            myThread.start();

            try {
                Thread.sleep(5000);
            } catch (InterruptedException anIE) {
            }

            System.out.println("Do write: " + 
                               theSpace.write(myPackedEntry, null,
                                             Lease.FOREVER) +
                               Thread.currentThread());

            try {
                Thread.sleep(5000);
            } catch (InterruptedException anIE) {
            }

            System.out.println("Do write: " + 
                               theSpace.write(myPackedEntry, null,
                                             Lease.FOREVER) +
                               Thread.currentThread());

            try {
                Thread.sleep(5000);
            } catch (InterruptedException anIE) {
            }

            theSpace.stop();

        } catch (Exception anE) {
            System.err.println("Writer failed :(");
            anE.printStackTrace(System.err);
        }
    }

    public static void main(String args[]) {
        new SpaceBlockTest().test();
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
