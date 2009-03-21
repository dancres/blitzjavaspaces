package org.dancres.blitz.test.queue;

import java.io.Serializable;

import net.jini.space.JavaSpace;
import net.jini.core.transaction.server.TransactionConstants;

import org.dancres.blitz.remote.LocalSpace;
import org.dancres.blitz.test.DummyEntry;
import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

/**
 */
public class Streamer {
    private static CyclicBarrier theBarrier;
    private static LocalSpace theSpace;
    private static ExpandableArray theArray;

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

            theBarrier.setBarrierCommand(new StatsChecker());

            theSpace = new LocalSpace(new TxnGatewayImpl());

            JavaSpace mySpace = theSpace.getJavaSpaceProxy();

            theArray = new ExpandableArray("blah", mySpace);
            theArray.create();

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


    private static class StatsChecker implements Runnable {
        StatsChecker() {
        }

        public void run() {
        }
    }

    private static class Writer extends Thread {
        private int theIterations;

        Writer(int anId, int anIterations) {
            theIterations = anIterations;
        }

        public void run() {
            try {
                while (true) {

                    long myStart = System.currentTimeMillis();

                    for (int i = 0; i < theIterations; i++) {
                        theArray.add(new DummyEntry("rhubarb"));

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
        private int theIterations;
        private long thePause;

        Taker(int anId, int anIterations, long aPause) {
            theIterations = anIterations;
            thePause = aPause;
        }

        public void run() {
            try {
                while (true) {

                    long myStart = System.currentTimeMillis();

                    for (int i = 0; i < theIterations; i++) {
                        Serializable myData = theArray.pop();

                        if (myData == null)
                            System.err.println("Whoops");

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
