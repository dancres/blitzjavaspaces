package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;

import java.util.Random;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import net.jini.space.JavaSpace;

/**
   <p>Transactionally beat on blitz.  Uses Blitz in a remote configuration.</p>

   <p>Pass -Dtxnmgrname to specify the name of a specific txn mgr to use</p>

   <p>Pass -Dload=true to specify that the discovered space should be
   pre-loaded with an entry-set.</p>
 */
public class TxnStress {
    private static Stresser[] theBeaters;

    public void test(int aNumBeaters, int aPoolSize,
                     long aPause, boolean isDebug,
                     boolean doLoad, boolean doInvert) throws Exception {

        String myTxnMgrName = System.getProperty("txnmgrname");

        Lookup myLookup = new Lookup(JavaSpace.class);

        System.out.println("Find space");

        JavaSpace mySpace = (JavaSpace) myLookup.getService();

        System.out.println("Got me a space");

        System.out.println("Find txnmgr");

        myLookup = new Lookup(TransactionManager.class, myTxnMgrName);
        TransactionManager myManager =
            (TransactionManager) myLookup.getService();

        System.out.println("Got me a txn mgr");

        if (doLoad) {
            System.out.println("Filling:");
            for (int i = 0; i < aPoolSize; i++) {
                System.out.print(".");
                Integer myValue = new Integer(i);

                Entry myEntry = new DummyEntry(myValue.toString());
                
                mySpace.write(myEntry, null, Lease.FOREVER);
            }
            System.out.println();
        } else {
            System.out.println("Not filling");
        }

        Random myRNG = new Random();
        theBeaters = new Stresser[aNumBeaters];

        for (int i = 0; i < aNumBeaters; i++) {
            theBeaters[i] = new Stresser(myManager, mySpace, aPoolSize,
                                         aPause, isDebug, doInvert);

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
        private TransactionManager theTxnMgr;
        private JavaSpace theSpace;
        private Random theRNG = new Random();
        private int thePoolSize;
        private long thePause;
        private boolean isDebug;
        private boolean isInverted;

        /*
           Statistics gathered up by Watcher
        */
        private long theTxns;

        Stresser(TransactionManager aMgr, JavaSpace aSpace, int aPoolSize,
                 long aPause, boolean debug, boolean doInvert) {
            theTxnMgr = aMgr;
            theSpace = aSpace;
            thePoolSize = aPoolSize;
            thePause = aPause;
            isDebug = debug;
            isInverted = doInvert;
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

                    Transaction.Created myTxnC = 
                        TransactionFactory.create(theTxnMgr,Lease.FOREVER);

                    Transaction myTxn = myTxnC.transaction;

                    // Transaction myTxn = null;

                    Entry myResult = take(myTemplate, thePause, myTxn);

                    if (isInverted) {
                        if (myResult == null)
                            theSpace.write(myTemplate, myTxn, Lease.FOREVER);
                    } else {
                        if (myResult != null)
                            theSpace.write(myTemplate, myTxn, Lease.FOREVER);
                    }

                    myTxn.commit();

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
                           long aTimeout, Transaction aTxn)
            throws Exception {

            Entry myResult = theSpace.take(aTemplate, aTxn, aTimeout);

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
            System.setSecurityManager(new RMISecurityManager());
            new TxnStress().test(Integer.parseInt(anArgs[0]),
                                 Integer.parseInt(anArgs[1]),
                                 Long.parseLong(anArgs[2]),
                                 Boolean.getBoolean("debug"),
                                 Boolean.getBoolean("load"),
                                 Boolean.getBoolean("invert"));

            new Watcher(theBeaters).start();
        } catch (Exception anE) {
            System.err.println("Stress failed");
            anE.printStackTrace(System.err);
        }
    }
}
