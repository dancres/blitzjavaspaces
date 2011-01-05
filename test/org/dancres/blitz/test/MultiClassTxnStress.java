package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.server.ServerTransaction;
import net.jini.core.transaction.server.TransactionConstants;
import net.jini.space.JavaSpace05;
import org.dancres.blitz.remote.LocalSpace;
import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MultiClassTxnStress {
    private static Stresser[] theBeaters;

    public void test(int aNumBeaters, int aPoolSize,
                     long aPause, boolean isDebug) throws Exception {

        LocalSpace myLocalSpace = new LocalSpace(new TxnGatewayImpl());

        JavaSpace05 mySpace = myLocalSpace.getProxy();

        List<Entry> myEntries = new ArrayList<Entry>();
        List<Long> myDurations = new ArrayList<Long>();
        myDurations.add(new Long(Lease.FOREVER));
        myDurations.add(new Long(Lease.FOREVER));

        System.out.println("Filling:");
        for (int i = 0; i < aPoolSize; i++) {
            System.out.print(".");
            Integer myValue = new Integer(i);

            myEntries.add(new DummyEntry(myValue.toString()));
            myEntries.add(new BlockEntry(myValue.toString()));

            mySpace.write(myEntries, null, myDurations);

            myEntries.clear();
        }

        System.out.println();

        Random myRNG = new Random();
        theBeaters = new Stresser[aNumBeaters];

        for (int i = 0; i < aNumBeaters; i++) {
            TxnMgr myMgr = new TxnMgr(i, myLocalSpace);
            theBeaters[i] = new Stresser(myMgr, myLocalSpace, aPoolSize,
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

    private static class TxnGatewayImpl implements TxnGateway {
        public int getState(TxnId anId) {
            return TransactionConstants.COMMITTED;
        }

        public void join(TxnId anId) {
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
        private TxnMgr theTxnMgr;
        private LocalSpace theSpace;
        private Random theRNG = new Random();
        private int thePoolSize;
        private long thePause;
        private boolean isDebug;

        /*
           Statistics gathered up by Watcher
        */
        private long theTxns;

        Stresser(TxnMgr aMgr, LocalSpace aSpace, int aPoolSize, long aPause,
                 boolean debug) {
            theTxnMgr = aMgr;
            theSpace = aSpace;
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

            JavaSpace05 mySpace = theSpace.getProxy();

            while(true) {
                try {

                    Entry myFirstTemplate = new DummyEntry(new Integer(theRNG.nextInt(thePoolSize)).toString());
                    Entry mySecondTemplate = new BlockEntry(new Integer(theRNG.nextInt(thePoolSize)).toString());

                    ServerTransaction myTxn =
                        new ServerTransaction(theTxnMgr, myNextTxnId++);

                    Entry myFirstResult =
                            take(mySpace, myFirstTemplate, thePause, myTxn);

                    Entry mySecondResult =
                            take(mySpace, mySecondTemplate, thePause, myTxn);

                    if (myFirstResult != null)
                        mySpace.write(myFirstTemplate, myTxn, Lease.FOREVER);

                    if (mySecondResult != null)
                        mySpace.write(mySecondTemplate, myTxn, Lease.FOREVER);

                    theSpace.getTxnControl().prepareAndCommit(theTxnMgr,
                                                              myTxn.id);

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

        private Entry take(JavaSpace05 aSpace, Entry aTemplate,
                           long aTimeout, ServerTransaction aTxn)
            throws Exception {

            Entry myResult = aSpace.take(aTemplate,
                    aTxn, aTimeout);

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
            new MultiClassTxnStress().test(Integer.parseInt(anArgs[0]),
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
