package org.dancres.blitz.txn;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.test.DummyEntry;
import org.dancres.blitz.test.TxnMgr;

/**
   Delete all state and ensure that the config is setup for persistent logging.
   Then run the test once with no arguments.  On exit, Blitz should report
   that one transaction was active and that there were no entry's.  Rerun
   the test with an argument of "true" or whatever and a single transaction
   should be restored from log which will then be commited in the second
   run resulting in a DummyEntry appearing - i.e. instance count should be
   one.
 */
public class TestCkpt {
    public static void main(String args[]) {
        try {
            if (args.length == 0)
                new TestCkpt().test(false);
            else
                new TestCkpt().test(true);

        } catch (Exception anE) {
            anE.printStackTrace(System.err);
        }
    }

    public void test(boolean isRestart) throws Exception {

        LocalSpace mySpace = new LocalSpace(new TxnGatewayImpl());

        TxnMgr myMgr = new TxnMgr(1, mySpace);

        ServerTransaction myTxn = myMgr.newTxn();

        if (isRestart) {
            myTxn.commit();
        } else {
            Entry myTemplate = new DummyEntry("rhubarb");

            mySpace.getProxy().write(myTemplate, myTxn, Lease.FOREVER);

            myTxn.commit();

            TxnManager.get().requestAsyncCheckpoint();
        }

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
