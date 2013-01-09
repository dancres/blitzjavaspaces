package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.server.*;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.remote.LocalTxnMgr;
import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LockTest {
    private LocalSpace theSpace;

    @Before
    public void init() throws Exception {
        theSpace = new LocalSpace(new TxnGatewayImpl());
    }

    @After
    public void deinit() throws Exception {
        theSpace.stop();
    }

    @Test
    public void test() throws Exception {
        JavaSpace mySpace = theSpace.getProxy();

        LocalTxnMgr myMgr = new LocalTxnMgr(1, theSpace);
        
        Entry myTemplate = new DummyEntry("rhubarb");

        ServerTransaction myTxn = myMgr.newTxn();

        System.out.println("Write");

        mySpace.write(myTemplate, myTxn, Lease.FOREVER);

        Taker myTaker = new Taker();
        myTaker.start();

        Assert.assertNotNull(mySpace.read(myTemplate, myTxn, Lease.FOREVER));

        Thread.sleep(5000);

        myTxn.commit();

        myTaker.join();

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
        private Entry _result;

        public void run() {
            try {
                JavaSpace mySpace = theSpace.getProxy();

                Entry myResult = mySpace.takeIfExists(new DummyEntry(), null, 20000);

                Assert.assertNotNull(myResult);
            } catch (Exception anException) {
                System.out.println("Taker failed");
                anException.printStackTrace(System.out);
            }
        }
    }
}
