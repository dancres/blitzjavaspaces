package org.dancres.blitz.test;

import org.dancres.blitz.remote.LocalSpace;

import net.jini.space.JavaSpace;
import net.jini.core.transaction.server.ServerTransaction;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import org.dancres.blitz.remote.LocalTxnMgr;

public class LargeTxn {
    public static void main(String[] anArgs) throws Exception {
        LocalSpace myLocalSpace = new LocalSpace(new TxnGatewayImpl());

        JavaSpace mySpace = myLocalSpace.getProxy();

        LocalTxnMgr myMgr = new LocalTxnMgr(1, myLocalSpace);

        ServerTransaction tx = myMgr.newTxn();

        int myTotalEntries = 100;

        if (anArgs.length == 1)
            myTotalEntries = Integer.parseInt(anArgs[0]);

        long myStart = System.currentTimeMillis();

        for (int i = 0; i < myTotalEntries; i++)
            mySpace.write(new DummyEntry(Integer.toString(i)),
                    null, Lease.FOREVER);

        displayDuration("Initial load", myStart, System.currentTimeMillis());

        try {

            myStart = System.currentTimeMillis();

            for (int i = 0; i < myTotalEntries; i++) {
                Entry myResult =
                        mySpace.take(new DummyEntry(Integer.toString(i)),
                                tx, 100);

                if (myResult == null)
                    throw new RuntimeException("Lost entry: " + i);
            }

            displayDuration("Takes", myStart, System.currentTimeMillis());

            myStart = System.currentTimeMillis();

            for (int i = 0; i < myTotalEntries; i++)
                mySpace.write(new DummyEntry(Integer.toString(i)),
                        tx, Lease.FOREVER);

            displayDuration("Writes", myStart, System.currentTimeMillis());

            myStart = System.currentTimeMillis();

            tx.commit();

            displayDuration("Commit", myStart, System.currentTimeMillis());

            myLocalSpace.destroy();

            Thread.sleep(5000);

        } catch(Exception e){
            System.out.println("Tx failed");
            e.printStackTrace(System.err);
            myMgr.abort(tx.id);
        }
    }

    private static void displayDuration(String aPhase, long aStart,
                                        long anEnd) {
        System.out.println(aPhase + ": " + (anEnd - aStart));
    }
}
