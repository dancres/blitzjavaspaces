package org.dancres.blitz.remote.nio;

import java.io.IOException;
import java.util.Random;
import java.rmi.RMISecurityManager;
import java.net.InetSocketAddress;

import net.jini.core.lease.Lease;
import net.jini.core.entry.Entry;
import net.jini.space.JavaSpace;

import org.dancres.blitz.mangler.EntryMangler;

/**
 */
public class StressClient {
    private Invoker _invoker;
    private static Stresser[] _beaters;

    StressClient(InetSocketAddress anAddr) throws IOException {
        _invoker = new Invoker(anAddr, true);
    }

    public static class DummyEntry implements Entry {
        public String theName;
        public String anotherField;

        public DummyEntry() {
        }

        public DummyEntry(String aName) {
            theName = aName;
        }

        public String toString() {
            return theName;
        }

        public boolean equals(Object anObject) {
            if ((anObject != null) && (anObject instanceof DummyEntry)) {

                DummyEntry myEntry = (DummyEntry) anObject;

                if (myEntry.theName == null)
                    return (myEntry.theName == theName);
                else
                    return ((DummyEntry) anObject).theName.equals(theName);
            }

            return false;
        }
    }

    void test(boolean doLoad, int aNumBeaters, int aPoolSize, int aPause,
              boolean isDebug) {
        try {
            if (doLoad) {
                System.out.println("Filling:");
                for (int i = 0; i < aPoolSize; i++) {
                    System.out.print(".");
                    Integer myValue = new Integer(i);

                    Entry myEntry =
                            new DummyEntry(myValue.toString());

                    _invoker.write(EntryMangler.getMangler().mangle(myEntry),
                            null, Lease.FOREVER);
                }
                System.out.println();
            }

            Random myRNG = new Random();
            _beaters = new Stresser[aNumBeaters];

            for (int i = 0; i < aNumBeaters; i++) {
                _beaters[i] = new Stresser(_invoker, aPoolSize,
                        aPause, isDebug);

                Thread myThread = new Thread(_beaters[i]);
                myThread.setName(Integer.toString(i));
                myThread.start();

                try {
                    Thread.sleep((long) myRNG.nextInt(500));
                } catch (InterruptedException anIE) {
                }
            }

        } catch (Exception anE) {
            System.err.println("Rdv error");
            anE.printStackTrace(System.err);
            System.exit(0);
        }
    }

    public void run() {
    }

    private static class Watcher extends Thread {
        private Stresser[] theBeaters;

        Watcher(Stresser[] aBeaters) {
            theBeaters = aBeaters;
            setDaemon(true);
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
        private Invoker _invoker;
        private Random theRNG = new Random();
        private int thePoolSize;
        private long thePause;
        private boolean isDebug;

        /*
           Statistics gathered up by Watcher
        */
        private long theTxns;

        Stresser(Invoker anInvoker, int aPoolSize,
                 long aPause, boolean debug) {
            _invoker = anInvoker;
            thePoolSize = aPoolSize;
            thePause = aPause;
            isDebug = debug;
        }

        String getStats() {
            StringBuffer myStats = new StringBuffer();

            synchronized(this) {
                myStats.append(" Txns:");
                myStats.append(Long.toString(theTxns));
                theTxns = 0;
            }

            return myStats.toString();
        }

        /**
           @todo Should test the take for != null and only write in that case.
         */
        public void run() {
            long myNextTxnId = 0;

            while(true) {
                try {
                    Integer myValue =
                        new Integer(theRNG.nextInt(thePoolSize));

                    Entry myTemplate = new DummyEntry(myValue.toString());

                    Entry myResult = take(myTemplate, thePause);

                    if (myResult != null)
                        _invoker.write(EntryMangler.getMangler().mangle(myTemplate)
                                , null, Lease.FOREVER);

                    synchronized(this) {
                        ++theTxns;
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

        private Entry take(Entry aTemplate,
                           long aTimeout)
            throws Exception {

            Entry myResult = _invoker.take(EntryMangler.getMangler().mangle(aTemplate),
                    null, aTimeout);

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
        if (anArgs.length != 5) {
            System.err.println("Usage: Stress <addr> <port> <threads> <pool_size> <timeout>");
            System.exit(-1);
        }

        try {
            System.setSecurityManager(new RMISecurityManager());
            new StressClient(new InetSocketAddress(anArgs[0],
                    Integer.parseInt(anArgs[1]))).test(Boolean.getBoolean("load"),
                    Integer.parseInt(anArgs[2]),
                    Integer.parseInt(anArgs[3]),
                    Integer.parseInt(anArgs[4]),
                    Boolean.getBoolean("debug"));

            new Watcher(_beaters).start();
        } catch (Exception anE) {
            System.err.println("Stress failed");
            anE.printStackTrace(System.err);
        }
    }
}
