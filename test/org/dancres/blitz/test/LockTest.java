package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.server.*;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.remote.LocalTxnMgr;
import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;

public class LockTest {
    private LocalSpace theSpace;

    public static void main(String args[]) {
        try {
            new LockTest().test();
        } catch (Exception anE) {
            anE.printStackTrace(System.err);
        }
    }

    public void test() throws Exception {

        theSpace = new LocalSpace(new TxnGatewayImpl());

        JavaSpace mySpace = theSpace.getProxy();

        LocalTxnMgr myMgr = new LocalTxnMgr(1, theSpace);
        
        new Taker().start();

        Entry myTemplate = new DummyEntry("rhubarb");

        ServerTransaction myTxn = myMgr.newTxn();

        System.out.println("Write");

        mySpace.write(myTemplate, myTxn, Lease.FOREVER);

        System.out.println("Read");

        Entry myResult = mySpace.read(myTemplate, myTxn, Lease.FOREVER);

        if (myResult == null)
            throw new Exception("Couldn't read!");

        Thread.sleep(5000);
        
        System.out.println("Commit");
        myTxn.commit();

        // Wait for things to settle
        //
        Thread.sleep(5000);

        theSpace.stop();
    }
    
    private static class TxnGatewayImpl implements TxnGateway {
        public int getState(TxnId anId) {
            return TransactionConstants.COMMITTED;
        }

        public void join(TxnId anId) {
        }
    }

    private class Taker extends Thread {
        public void run() {
            try {
                System.out.println("Prepare template");

                JavaSpace mySpace = theSpace.getProxy();

                System.out.println("Taking....." + Thread.currentThread());
                Entry myResult = mySpace.take(new DummyEntry(), null,
                                               20000);

                if (myResult != null)
                    System.out.println(myResult + " " + Thread.currentThread());
                else
                    throw new RuntimeException ("No entry found :(" +
                        Thread.currentThread());

            } catch (Exception anException) {
                System.out.println("Taker failed");
                anException.printStackTrace(System.out);
            }
        }
    }
}
