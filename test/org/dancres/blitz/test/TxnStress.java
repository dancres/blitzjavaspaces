package org.dancres.blitz.test;

import java.util.Random;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.server.*;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.remote.TxnMgr;
import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;

/**
   Transactionally beat on blitz.  Uses Blitz in an embedded format.
 */
public class TxnStress {
    private static Stresser[] theBeaters;

    public void test(int aNumBeaters, int aPoolSize,
                     long aPause, boolean isDebug) throws Exception {

        LocalSpace myLocalSpace = new LocalSpace(new TxnGatewayImpl());

        JavaSpace mySpace = myLocalSpace.getProxy();

        System.out.println("Filling:");
        for (int i = 0; i < aPoolSize; i++) {
            System.out.print(".");
            Integer myValue = new Integer(i);

            Entry myEntry = new DummyEntry(myValue.toString());

            mySpace.write(myEntry, null, Lease.FOREVER);
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

            JavaSpace mySpace = theSpace.getProxy();
            
            while(true) {
                try {
                    Integer myValue =
                        new Integer(theRNG.nextInt(thePoolSize));

                    Entry myTemplate = new DummyEntry(myValue.toString());

                    ServerTransaction myTxn =
                        new ServerTransaction(theTxnMgr, myNextTxnId++);

                    Entry myResult = 
                            take(mySpace, myTemplate, thePause, myTxn);

                    if (myResult != null)
                        mySpace.write(myTemplate, myTxn, Lease.FOREVER);
                    
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

        private Entry take(JavaSpace aSpace, Entry aTemplate,
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
            new TxnStress().test(Integer.parseInt(anArgs[0]),
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
