package org.dancres.blitz.test;

import java.util.Random;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

public class Through {
    private static Stresser[] theBeaters;

    public void test(int aNumBeaters, long aPause,
                     boolean isDebug) throws Exception {

        LocalSpace mySpace = new LocalSpace();
        Random myRNG = new Random();
        theBeaters = new Stresser[aNumBeaters];

        for (int i = 0; i < aNumBeaters; i++) {
            theBeaters[i] = new Stresser(mySpace, aPause, isDebug,
                                         ((i % 2) == 0));

            Thread myThread = new Thread(theBeaters[i]);
            myThread.setName(Integer.toString(i));
            myThread.start();

            try {
                Thread.sleep((long) myRNG.nextInt(500));
            } catch (InterruptedException anIE) {
            }
        }
    }
    
    private static class Watcher extends Thread {
        private Stresser[] theBeaters;

        Watcher(Stresser[] aBeaters) {
            theBeaters = aBeaters;
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(20 * 1000);
                } catch (InterruptedException anIE) {
                    System.err.println("Awoken early!");
                }

                for (int i = 0; i < theBeaters.length; i++) {
                    System.out.println("Beater: " + i + "," + theBeaters[i].getStats());
                }
            }
        }
    }

    private class Stresser implements Runnable {
        private LocalSpace theSpace;
        private Random theRNG = new Random();
        private int thePoolSize;
        private long thePause;
        private boolean isDebug;
        private boolean isTaker;

        /*
           Statistics gathered up by Watcher
        */
        private long theWrites;
        private long theTakes;

        Stresser(LocalSpace aSpace, long aPause, boolean debug,
                 boolean taker) {

            theSpace = aSpace;
            thePause = aPause;
            isDebug = debug;
            isTaker = taker;

            if (isTaker)
                System.err.println("Taker created");
            else
                System.err.println("Writer created");
        }

        String getStats() {
            StringBuffer myStats = new StringBuffer();

            synchronized(this) {
                // if (!isTaker) {
                    myStats.append("Writes: ");
                    myStats.append(Long.toString(theWrites));
                    // } else {
                    myStats.append(" Takes:");
                    myStats.append(Long.toString(theTakes));
                    // }

                theWrites = 0;
                theTakes = 0;
            }

            return myStats.toString();
        }

        public void run() {
            try {
                JavaSpace mySpace = theSpace.getProxy();
                while(true) {

                    if (isTaker) {
                        Entry myTemplate = new DummyEntry();
                            // new DummyEntry(Integer.toString(theRNG.nextInt(10)));

                        Entry myResult = mySpace.take(myTemplate, null,
                                                       thePause);

                        if (myResult != null)
                            synchronized(this) {
                                theTakes++;
                            }
                        else
                            synchronized(this) {
                                theWrites--;
                            }
                    } else {
                        mySpace.write(new DummyEntry(Integer.toString(theRNG.nextInt(10))),
                                null, Lease.FOREVER);

                        synchronized(this) {
                            theWrites++;
                        }

                        Thread.sleep(20);
                    }

                }
            } catch (Throwable aThrowable) {
                System.err.println("Stresser got exception");
                aThrowable.printStackTrace(System.err);
            }
        }

        private String getId() {
            return Thread.currentThread().getName();
        }
    }

    public static void main(String anArgs[]) {
        if (anArgs.length != 2) {
            System.err.println("Usage: Through <threads> <timeout>");
            System.exit(-1);
        }

        try {
            new Through().test(Integer.parseInt(anArgs[0]),
                              Long.parseLong(anArgs[1]),
                              Boolean.getBoolean("debug"));

            new Watcher(theBeaters).start();
        } catch (Exception anE) {
            System.err.println("Stress failed");
            anE.printStackTrace(System.err);
        }
    }
}
