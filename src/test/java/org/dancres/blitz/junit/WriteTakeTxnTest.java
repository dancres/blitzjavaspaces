package org.dancres.blitz.junit;

import junit.framework.Assert;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.remote.LocalTxnMgr;
import org.dancres.blitz.test.DummyEntry;
import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WriteTakeTxnTest {
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
    public void testTakeTxn() throws Exception {
        Entry myTemplate = new DummyEntry("rhubarb");

        Transaction.Created myCreatedTxn = TransactionFactory.create(_mgr, Lease.FOREVER);

        _space.getProxy().write(myTemplate, myCreatedTxn.transaction, Lease.FOREVER);

        myCreatedTxn.lease.renew(Lease.FOREVER);

        Entry myResult =
            _space.getProxy().takeIfExists(myTemplate, myCreatedTxn.transaction, Lease.FOREVER);

        Assert.assertNotNull(myResult);

        myCreatedTxn.transaction.commit();
    }
    
    private static class TxnGatewayImpl implements TxnGateway {
        public int getState(TxnId anId) {
            return TransactionConstants.COMMITTED;
        }

        public void join(TxnId anId) {
        }
    }
}
