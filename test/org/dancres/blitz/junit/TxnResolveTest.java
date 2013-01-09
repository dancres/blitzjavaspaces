package org.dancres.blitz.junit;

import junit.framework.Assert;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.server.*;

import org.dancres.blitz.remote.LocalSpace;
import org.dancres.blitz.remote.LocalTxnMgr;

import org.dancres.blitz.test.DummyEntry;
import org.dancres.blitz.test.TxnGatewayImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TxnResolveTest {
    private LocalSpace _space;
    private LocalTxnMgr _mgr;

    @Before
    public void init() throws Exception {
        _space = new LocalSpace(new TxnGatewayImpl());
        _mgr = new LocalTxnMgr(1, _space);
    }

    @After
    public void deinit() throws Exception {
        _space.stop();
    }

    @Test
    public void test() throws Exception {
        ServerTransaction tx = _mgr.newTxn();
        
        Writer myWriter = new Writer(_space);
        myWriter.start();

        Aborter myAborter = new Aborter(_mgr, tx.id);
        myAborter.start();
        
        try {
            _space.getProxy().take(new DummyEntry(), tx, Long.MAX_VALUE);
            tx.commit();

            Assert.fail("Transaction should never commit");

        } catch(Exception e){
        }

        try {
            myWriter.join();
            myAborter.join();
        } catch (InterruptedException anIE) {
        }
    }

    private static class Aborter extends Thread {
        private LocalTxnMgr theMgr;
        private long theId;

        Aborter(LocalTxnMgr aMgr, long aTxnId) {
            theMgr = aMgr;
            theId = aTxnId;
        }
        
        public void run() {
            try {

                Thread.sleep(5000);

                theMgr.abort(theId);
            } catch (Exception anE) {
                anE.printStackTrace(System.err);
            }
        }
    }

    private static class Writer extends Thread {
        private LocalSpace theSpace;
        
        Writer(LocalSpace aSpace) {
            theSpace = aSpace;
        }
        
        public void run() {
            try {

                Thread.sleep(10000);

                theSpace.getProxy().write(new DummyEntry("blah"), null,
                        Lease.FOREVER);
            } catch (Exception anE) {
                anE.printStackTrace(System.err);
            }
        }
    }
}
