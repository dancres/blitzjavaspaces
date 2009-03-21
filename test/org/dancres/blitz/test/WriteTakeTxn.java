package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;

public class WriteTakeTxn {
    public static void main(String args[]) {
        try {
            new WriteTakeTxn().test();
        } catch (Exception anE) {
            anE.printStackTrace(System.err);
        }
    }

    public void test() throws Exception {

        LocalSpace mySpace = new LocalSpace(new TxnGatewayImpl());

        TxnMgr myMgr = new TxnMgr(1, mySpace);
        
        Entry myTemplate = new DummyEntry("rhubarb");

        ServerTransaction myTxn = myMgr.newTxn();

        mySpace.getProxy().write(myTemplate, myTxn, Lease.FOREVER);

        Entry myResult =
            mySpace.getProxy().takeIfExists(myTemplate, myTxn, Lease.FOREVER);

        if (myResult == null)
            throw new Exception("Couldn't take!");
                    
        myTxn.commit();

        mySpace.stop();
    }
    
    private static class TxnGatewayImpl implements TxnGateway {
        public int getState(TxnId anId) {
            return TransactionConstants.COMMITTED;
        }

        public void join(TxnId anId) {
        }
    }
}
