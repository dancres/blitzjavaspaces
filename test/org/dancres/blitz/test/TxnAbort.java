package org.dancres.blitz.test;

import junit.framework.Assert;
import net.jini.core.entry.Entry;

import net.jini.core.transaction.server.*;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.remote.LocalTxnMgr;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TxnAbort {

    private LocalSpace _localSpace;
    private LocalTxnMgr _mgr;

    @Before
    public void init() throws Exception {
        _localSpace = new LocalSpace(new TxnGatewayImpl());
        _mgr = new LocalTxnMgr(1, _localSpace);
    }

    @After
    public void deinit() throws Exception {
        _localSpace.stop();
    }

    @Test
    public void test() throws Exception {

        JavaSpace mySpace = _localSpace.getProxy();

        ServerTransaction tx = _mgr.newTxn();

        mySpace.write(new DummyEntry("1"), null, Long.MAX_VALUE);

        mySpace.write(new DummyEntry("2"), tx, Long.MAX_VALUE);

        Entry myResult =
                mySpace.take(new DummyEntry("1"), tx, 100);

        Assert.assertNotNull(myResult);

        _mgr.abort(tx.id);

        myResult = mySpace.take(new DummyEntry("1"), null, 100);

        Assert.assertNotNull(myResult);

        myResult = mySpace.take(new DummyEntry(), null, 100);

        Assert.assertNull(myResult);
    }
}
