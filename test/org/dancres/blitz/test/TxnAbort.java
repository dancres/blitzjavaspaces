package org.dancres.blitz.test;

import net.jini.core.entry.Entry;

import net.jini.core.transaction.server.*;
import net.jini.space.JavaSpace;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.remote.TxnMgr;

public class TxnAbort {

    public void test() throws Exception {

        LocalSpace myLocalSpace = new LocalSpace(new TxnGatewayImpl());

        JavaSpace mySpace = myLocalSpace.getProxy();

        TxnMgr myMgr = new TxnMgr(1, myLocalSpace);
        
        ServerTransaction tx = myMgr.newTxn();
        
        mySpace.write(new DummyEntry("1"), null, Long.MAX_VALUE);

        mySpace.write(new DummyEntry("2"), tx, Long.MAX_VALUE);

        try {
            Entry myResult = 
                mySpace.take(new DummyEntry("1"), tx, 100);
            
            System.out.println("(1) Got result: " + myResult);
            if (myResult == null)
                throw new RuntimeException("Failed to get Entry match");

            myMgr.abort(tx.id);

            myResult = mySpace.take(new DummyEntry("1"), null, 100);
            System.out.println("(2) Got result: " + myResult);
            if (myResult == null)
                throw new RuntimeException("Failed to get Entry match");

            myResult = mySpace.take(new DummyEntry(), null, 100);

            System.out.println("(3) Got result: " + myResult);
            if (myResult != null)
                throw new RuntimeException("Failed: Got Entry match");

        } catch(Exception e){
            System.out.println("Tx failed");
            e.printStackTrace(System.err);
            tx.abort();
        }
    }

    public static void main(String args[]) {
        try {
            new TxnAbort().test();
        } catch (Exception anE) {
            anE.printStackTrace(System.err);
        }
    }
}
