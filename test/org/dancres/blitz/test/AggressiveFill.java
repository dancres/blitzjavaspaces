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

public class AggressiveFill {
    public static void main(String args[]) {

        try {
            Random myRandom = new Random();

            LocalSpace myLocalSpace = new LocalSpace(new TxnGatewayImpl());

            TxnMgr myMgr = new TxnMgr(1, myLocalSpace);

            System.out.println("Start space");

            JavaSpace mySpace = myLocalSpace.getProxy();

            int myNextTxnId = 0;

            for (int i = 0; i < 10000; i++) {

                System.out.println("Pass: " + i);

                ServerTransaction myTxn =
                    new ServerTransaction(myMgr, myNextTxnId++);

                for (int j = 0; j < 10; j++) {
                    String myKey =
                        Integer.toString((i * 10) + j);

                    Entry myEntry = new TestEntry(myKey);

                    Entry myConflict;

                    myConflict = mySpace.takeIfExists(myEntry, myTxn, 10000);

                    if (myConflict != null) {
                        System.out.print("x");
                        mySpace.take(new TestEntry2(myKey), myTxn, 10000);
                        mySpace.take(new TestEntry3(myKey), myTxn, 10000);
                    } else {
                        System.out.print(".");
                    }

                    mySpace.write(myEntry, myTxn, Lease.FOREVER);

                    mySpace.write(new TestEntry2(myKey),
                                  myTxn, Lease.FOREVER);
                    
                    mySpace.write(new TestEntry3(myKey),
                                  myTxn, Lease.FOREVER);
                }

                mySpace.write(new TestEntry4(),
                              myTxn, Lease.FOREVER);

                mySpace.write(new TestEntry5(),
                              myTxn, Lease.FOREVER);

                myTxn.commit();
                
                System.out.println();
            }


            // mySpace.stop();
            System.exit(0);

        } catch (Exception anE) {
            System.err.println("Got exception :(");
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

    public static class TestEntry implements Entry {
        public String rhubarb;
        public Double count;
        public Integer meta;

        public TestEntry() {
        }

        public TestEntry(String aRhubarb) {
            rhubarb = aRhubarb;
            count = new Double(1.1d);
            // meta = new Integer(45);
        }

        public String toString() {
            return super.toString() + ", " + rhubarb + ", " + count;
        }
    }

    public static class TestEntry2 implements Entry {
        public String rhubarb;
        public Integer count;
        public Integer meta;

        public TestEntry2() {
        }

        public TestEntry2(String aRhubarb) {
            rhubarb = aRhubarb;
        }

        public String toString() {
            return super.toString() + ", " + rhubarb + ", " + count;
        }
    }

    public static class TestEntry3 implements Entry {
        public String rhubarb;
        public Integer count;
        public Integer meta;

        public TestEntry3() {
        }

        public TestEntry3(String aRhubarb) {
            rhubarb = aRhubarb;
        }

        public String toString() {
            return super.toString() + ", " + rhubarb + ", " + count;
        }
    }

    public static class TestEntry4 implements Entry {
        public Long[] theLongs;

        public TestEntry4() {
            theLongs = new Long[10];
        }
    }

    public static class TestEntry5 implements Entry {
        public Long[] theLongs;

        public TestEntry5() {
            theLongs = new Long[10];
        }
    }
}
