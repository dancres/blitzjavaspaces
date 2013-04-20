package org.dancres.blitz;

import net.jini.core.lease.Lease;
import net.jini.core.transaction.server.TransactionConstants;
import org.dancres.blitz.remote.LocalSpace;
import org.dancres.blitz.test.DummyEntry;
import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;

public class RestartTest {
    public static void main(String[] anArgs) throws Exception {

        writeCycle();

        Thread.sleep(10000);

        takeCycle();
    }

    private static void writeCycle() throws Exception {
        System.err.println("***************  Start ****************");

        LocalSpace mySpace = newInstance();

        System.err.println("***************  Started ****************");

        mySpace.getProxy().write(new DummyEntry("rhubarb"), null, Lease.FOREVER);
        mySpace.getProxy().write(new DummyEntry("custard"), null, Lease.FOREVER);

        Thread.sleep(5000);

        System.err.println("***************  Stopping ****************");

        mySpace.stop();

        System.err.println("***************  Stopped ****************");
    }

    private static void takeCycle() throws Exception {
        System.err.println("***************  Start ****************");

        LocalSpace mySpace = newInstance();

        System.err.println("***************  Started ****************");

        // In transient mode these will fail in persistent mode they should succeed so we don't wait forever
        //
        mySpace.getProxy().take(new DummyEntry(), null, 0);
        mySpace.getProxy().take(new DummyEntry(), null, 0);
        
        Thread.sleep(5000);

        System.err.println("***************  Stopping ****************");

        mySpace.stop();

        System.err.println("***************  Stopped ****************");
    }

    private static LocalSpace newInstance() throws Exception {
        return new LocalSpace(new TxnGatewayImpl());
    }

    private static class TxnGatewayImpl implements TxnGateway {
        public int getState(TxnId anId) {
            return TransactionConstants.COMMITTED;
        }

        public void join(TxnId anId) {
        }
    }
}
