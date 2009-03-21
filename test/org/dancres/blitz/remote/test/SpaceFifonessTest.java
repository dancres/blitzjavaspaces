package org.dancres.blitz.remote.test;

import net.jini.space.JavaSpace;
import net.jini.core.lease.Lease;
import net.jini.core.entry.Entry;

import java.rmi.RMISecurityManager;

/**
 */
public class SpaceFifonessTest {
    public static void main(String[] args)
    {
        testFifoness();
    }

    public static void testFifoness()
    {
        try
        {
            if (System.getSecurityManager() == null)
                System.setSecurityManager(new RMISecurityManager());

            System.out.println("Start space test");

            //            SpaceImpl mySpace = new SpaceImpl(null);
            Lookup myFinder = new Lookup(JavaSpace.class);

            JavaSpace mySpace = (JavaSpace) myFinder.getService();

            TestEntry myNewEntry = new TestEntry("2000");
            System.out.println("Prepare entry " + myNewEntry.getClass());

            for(int i = 1; i <= 500; i++)
            {
                myNewEntry = new TestEntry(Integer.toString(i));

                mySpace.write(myNewEntry, null, Lease.FOREVER);

                /*
                 * System.out.println("Do write: " + mySpace.write(myPackedEntry, null, Lease.FOREVER));
                 */
            }

            // Write some more Entry's whilst we do takes
            //
            new Writer(mySpace).start();

            TestEntry templateEntry = new TestEntry();

            long myStart = System.currentTimeMillis();

            for(int i = 1; i < 2000; i++)
            {
                TestEntry myEntry = (TestEntry) mySpace.take(templateEntry, null, 10000);
                if(myEntry == null)
                {
                    System.out.println("timed out of take");
                    break;
                }
                System.out.println(i + ": " + myEntry);
            }

            long myEnd = System.currentTimeMillis();

            System.out.println("1000 takes in: " + (myEnd - myStart) + " ms");

            double myStartTime = (double) myStart;
            double myEndTime = (double) myEnd;

            double myTime = (myEndTime - myStartTime) / 1000;

            System.out.println("Time per take: " + myTime + " ms");

            System.out.println("Do stop");

        }
        catch(Exception anE)
        {
            System.err.println("Got exception :(");
            anE.printStackTrace(System.err);
        }

    }

    static class Writer extends Thread
    {
        private JavaSpace theSpace;

        Writer(JavaSpace aSpace)
        {
            theSpace = aSpace;
        }

        public void run()
        {
            try
            {
                for(int i = 501; i < 1001; i++)
                {
                    TestEntry myNewEntry = new TestEntry(Integer.toString(i));

                    theSpace.write(myNewEntry, null, Lease.FOREVER);
                    /*
                     * System.out.println("Do write: " + theSpace.write(myPackedEntry, null, Lease.FOREVER));
                     */
                }
            }
            catch(Exception anE)
            {
                System.err.println("Writer blew");
                anE.printStackTrace(System.err);
            }
        }
    }

    public static class TestEntry implements Entry
    {
        public String theString;

        public Integer meta;

        public TestEntry()
        {
        }

        public TestEntry(String aString)
        {
            theString = aString;
        }

        public String toString()
        {
            return theString;
        }
    }
}
