package org.dancres.blitz.test;

import java.util.Random;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

public class Stress {
    private static Stresser[] theBeaters;

    public void test(int aNumBeaters, int aPoolSize,
                     long aPause, boolean isDebug) throws Exception {

        LocalSpace mySpace = new LocalSpace();
        Random myRNG = new Random();
        theBeaters = new Stresser[aNumBeaters];

        for (int i = 0; i < aNumBeaters; i++) {
            theBeaters[i] = new Stresser(mySpace, aPoolSize,
                                         aPause, isDebug);

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
                    Thread.sleep(60 * 1000);
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
        private JavaSpace theProxy;
        private Random theRNG = new Random();
        private int thePoolSize;
        private long thePause;
        private boolean isDebug;

        /*
           Statistics gathered up by Watcher
        */
        private long theWrites;
        private long theTakes;

        Stresser(LocalSpace aSpace, int aPoolSize, long aPause,
                 boolean debug) {

            theSpace = aSpace;
            theProxy = theSpace.getProxy();            
            thePoolSize = aPoolSize;
            thePause = aPause;
            isDebug = debug;
        }

        String getStats() {
            StringBuffer myStats = new StringBuffer();

            synchronized(this) {
                myStats.append("Writes: ");
                myStats.append(Long.toString(theWrites));
                myStats.append(" Takes:");
                myStats.append(Long.toString(theTakes));
                theWrites = 0;
                theTakes = 0;
            }

            return myStats.toString();
        }

        public void run() {
            while(true) {
                try {
                    Integer myValue =
                        new Integer(theRNG.nextInt(thePoolSize));

                    Entry myTemplate = new DummyEntry(myValue.toString());

                    Entry myResult = take(myTemplate, thePause);

                    if (myResult == null) {
                        theProxy.write(myTemplate,
                                null, Lease.FOREVER);

                        synchronized(this) {
                            theWrites++;
                        }
                    } else {
                        synchronized(this) {
                            theTakes++;
                        }
                    }

                    if (isDebug) {
                        synchronized(System.out) {
                            System.out.print(getId() + "W");
                        }
                    }
                } catch (Throwable aThrowable) {
                    System.err.println("Stresser got exception");
                    aThrowable.printStackTrace(System.err);
                    break;
                }
            }
        }

        private Entry take(Entry aTemplate, long aTimeout) throws Exception {
            Entry myResult =
                    theProxy.take(aTemplate, null, aTimeout);

            if (isDebug) {
                synchronized(System.out) {
                    if (myResult != null) {
                        System.out.print(getId() + "T");
                    } else {
                        System.out.print(getId() + "|**|");
                    }
                }
            }
            return myResult;
        }

        private String getId() {
            return Thread.currentThread().getName();
        }
    }

    public static void main(String anArgs[]) {
        if (anArgs.length != 3) {
            System.err.println("Usage: Stress <threads> <pool_size> <timeout>");
            System.exit(-1);
        }

        try {
            new Stress().test(Integer.parseInt(anArgs[0]),
                              Integer.parseInt(anArgs[1]),
                              Long.parseLong(anArgs[2]),
                              Boolean.getBoolean("debug"));

            new Watcher(theBeaters).start();
        } catch (Exception anE) {
            System.err.println("Stress failed");
            anE.printStackTrace(System.err);
        }
    }
}
