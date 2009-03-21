package org.dancres.blitz.test;

import java.rmi.RemoteException;
import java.io.Serializable;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import net.jini.core.lease.Lease;

import net.jini.core.transaction.server.*;
import net.jini.core.entry.Entry;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.UnknownEventException;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.stats.*;

import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;

public class Streamer {
    private static CyclicBarrier theBarrier;
    private static LocalSpace theSpace;

    private static NotifyCount theCount;

    public static void main(String args[]) {
        try {
            int myTotalStreams = 10;
            int myIterations = 1000;
            long myTakePause = 0;

            if (args.length == 1) {
                myTotalStreams = Integer.parseInt(args[0]);
            } else if (args.length == 2) {
                myTotalStreams = Integer.parseInt(args[0]);
                myIterations = Integer.parseInt(args[1]);
            } else if (args.length == 3) {
                myTotalStreams = Integer.parseInt(args[0]);
                myIterations = Integer.parseInt(args[1]);
                myTakePause = Long.parseLong(args[2]);
            }

            theBarrier = new CyclicBarrier(2 * myTotalStreams);

            theCount = new NotifyCount(myIterations * myTotalStreams);

            theBarrier.setBarrierCommand(new StatsChecker(theCount));

            theSpace = new LocalSpace(new TxnGatewayImpl());

            JavaSpace mySpace = theSpace.getJavaSpaceProxy();

            mySpace.notify(new DummyEntry(), null, theCount,
                Lease.FOREVER, null);

            for (int i = 0; i < myTotalStreams; i++) {
                new Writer(i, myIterations).start();
                new Taker(i + myTotalStreams, myIterations,
                    myTakePause).start();
            }

        } catch (Exception anE) {
            anE.printStackTrace(System.err);
        }
    }

    private static class TxnGatewayImpl implements TxnGateway {
        public int getState(TxnId anId) {
            return TransactionConstants.COMMITTED;
        }
    
        public void join(TxnId anId) {
        }
    }

    private static class NotifyCount
        implements RemoteEventListener, Serializable {

        private long theCount;
        private long theExpectedTotal;

        NotifyCount(long anExpectedTotal) {
            theExpectedTotal = anExpectedTotal;
        }

        void checkCount() {
            if (theCount != theExpectedTotal) {
                System.err.println("Uhoh, lost some events - got " + theCount +
                " instead of " + theExpectedTotal);

                throw new RuntimeException("Bad event count");
            } else {
                System.err.println("Event count accurate");
                theCount = 0;
            }
        }

        private synchronized boolean incCount() {
            ++theCount;

            return ((theCount % 100) == 0);
        }

        public void notify(RemoteEvent remoteEvent)
            throws UnknownEventException, RemoteException {
            incCount();
        }
    }

    private static class StatsChecker implements Runnable {
        private NotifyCount theCount;

        StatsChecker(NotifyCount aCount) {
            theCount = aCount;
        }

        public void run() {
            System.err.println();
            System.err.println("Checking stats");

            Stat[] myStats = StatsBoard.get().getStats();

            for (int i = 0; i < myStats.length; i++) {
                if (myStats[i] instanceof InstanceCount) {
                    InstanceCount myStat = (InstanceCount) myStats[i];

                    if (myStat.getCount() < 0) {
                        System.err.println("Stat went negative: " +
                                           myStat.getType() + ", " +
                                           myStat.getCount());
                    } else if (myStat.getCount() == 0) {
                        System.err.println("Stat okay: " + myStat.getType());
                    } else {
                        System.err.println("Should be empty: " +
                                           myStat.getType() + ", " +
                                           myStat.getCount());
                    }
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException anIE) {

            }

            theCount.checkCount();
        }
    }

    private static class Writer extends Thread {
        private TxnMgr theTxnMgr;
        private long theNextId;
        private int theIterations;
        private NotifyCount theCount;

        Writer(int anId, int anIterations) {
            theTxnMgr = new TxnMgr(anId, theSpace);
            theIterations = anIterations;
        }

        public void run() {
            try {
                JavaSpace mySpace = theSpace.getProxy();

                while (true) {

                    long myStart = System.currentTimeMillis();

                    for (int i = 0; i < theIterations; i++) {
                        ServerTransaction myTxn = theTxnMgr.newTxn();

                        mySpace.write(new DummyEntry("rhubarb"),
                                myTxn, Lease.FOREVER);

                        myTxn.commit();

                        if ((i % 100) == 0) {
                            System.err.print("w");
                        }
                    }

                    System.err.println("Iteration took: " +
                        (System.currentTimeMillis() - myStart));

                    System.err.println("**");
                    theBarrier.barrier();
                }
            } catch (Exception anE) {
                anE.printStackTrace(System.err);
            }
        }
    }

    private static class Taker extends Thread {
        private TxnMgr theTxnMgr;
        private long theNextId;
        private int theIterations;
        private long thePause;

        Taker(int anId, int anIterations, long aPause) {
            theTxnMgr = new TxnMgr(anId, theSpace);
            theIterations = anIterations;
            thePause = aPause;
        }

        public void run() {
            try {
                JavaSpace mySpace = theSpace.getProxy();

                Entry myTemplate = mySpace.snapshot(new DummyEntry());

                while (true) {

                    long myStart = System.currentTimeMillis();

                    for (int i = 0; i < theIterations; i++) {
                        ServerTransaction myTxn = theTxnMgr.newTxn();

                        mySpace.take(myTemplate, myTxn, Lease.FOREVER);

                        myTxn.commit();

                        if ((i % 100) == 0) {
                            System.out.print("t");
                        }

                        if (thePause != 0)
                            Thread.sleep(thePause);
                    }

                    System.err.println("Iteration took: " +
                        (System.currentTimeMillis() - myStart));

                    theBarrier.barrier();
                }
            } catch (Exception anE) {
                anE.printStackTrace(System.err);
            }
        }
    }
}