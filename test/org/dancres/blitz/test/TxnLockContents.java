package org.dancres.blitz.test;

import org.dancres.blitz.remote.LocalSpace;
import net.jini.space.JavaSpace;
import net.jini.space.JavaSpace05;
import net.jini.space.MatchSet;
import net.jini.core.transaction.server.ServerTransaction;
import net.jini.core.lease.Lease;
import net.jini.core.entry.Entry;
import org.dancres.blitz.remote.LocalTxnMgr;

import java.util.Collection;
import java.util.Collections;

public class TxnLockContents {
    public static void main(String[] anArgs) throws Exception {
        LocalSpace myLocalSpace = new LocalSpace(new TxnGatewayImpl());

        JavaSpace mySpace = myLocalSpace.getProxy();

        LocalTxnMgr myMgr = new LocalTxnMgr(1, myLocalSpace);

        ServerTransaction tx = myMgr.newTxn();

        mySpace.write(new DummyEntry(Integer.toString(1)),
                null, Lease.FOREVER);

        try {

            Entry myResult =
                    mySpace.take(new DummyEntry(Integer.toString(1)),
                            tx, 100);

            if (myResult == null)
                throw new RuntimeException("Lost entry: " + 1);

            mySpace.write(new DummyEntry(Integer.toString(2)),
                    null, Lease.FOREVER);

            JavaSpace05 mySpace05 =
                    (JavaSpace05) myLocalSpace.getJavaSpaceProxy();

            Collection myTemplates =
                    Collections.singletonList(new DummyEntry());

            MatchSet mySet =
                    mySpace05.contents(myTemplates, null, Lease.FOREVER,
                            Long.MAX_VALUE);

            Entry myResult2;

            while ((myResult2 = mySet.next()) != null) {
                System.out.println(myResult2);
            }

            tx.commit();
        } catch (Exception e) {
            System.out.println("Tx failed");
            e.printStackTrace(System.err);
            myLocalSpace.getTxnControl().abort(myMgr, 1);
        }
    }

    private static void displayDuration(String aPhase, long aStart,
                                        long anEnd) {
        System.out.println(aPhase + ": " + (anEnd - aStart));
    }
}
