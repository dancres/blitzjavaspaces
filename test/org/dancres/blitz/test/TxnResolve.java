package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.server.*;

import org.dancres.blitz.remote.LocalSpace;

import org.dancres.blitz.remote.TxnMgr;

public class TxnResolve {

    public void test() throws Exception {

        LocalSpace mySpace = new LocalSpace(new TxnGatewayImpl());
        
        TxnMgr myMgr = new TxnMgr(1, mySpace);
        
        ServerTransaction tx = myMgr.newTxn();
        
        Writer myWriter = new Writer(mySpace);

        myWriter.start();

        new Aborter(mySpace, myMgr, tx.id).start();
        
        try {
            Entry myResult = 
                mySpace.getProxy().take(new DummyEntry(), tx, Long.MAX_VALUE);
                        
            System.out.println("Got result: " + myResult);
            
            tx.commit();

            System.out.println("Failed");

        } catch(Exception e){
            System.out.println("Tx failed - test passes");
            e.printStackTrace(System.err);
            // tx.abort();
        }

        try {
            myWriter.join();
        } catch (InterruptedException anIE) {
        }
    }

    private static class Aborter extends Thread {
        private LocalSpace theSpace;
        private TxnMgr theMgr;
        private long theId;

        Aborter(LocalSpace aSpace, TxnMgr aMgr, long aTxnId) {
            theSpace = aSpace;
            theMgr = aMgr;
            theId = aTxnId;
        }
        
        public void run() {
            try {

                Thread.sleep(10000);

                System.err.println("Abort");

                theMgr.abort(theId);

                System.err.println("Done");
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

                Thread.sleep(30000);

                System.err.println("Writing");
                theSpace.getProxy().write(new DummyEntry("blah"), null,
                        Lease.FOREVER);
                System.err.println("Done");
            } catch (Exception anE) {
                anE.printStackTrace(System.err);
            }
        }
    }

    public static void main(String args[]) {
        try {
            new TxnResolve().test();
        } catch (Exception anE) {
            anE.printStackTrace(System.err);
        }
    }
}
