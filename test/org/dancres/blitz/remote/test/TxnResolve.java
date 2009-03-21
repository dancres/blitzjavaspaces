package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import net.jini.space.JavaSpace;

public class TxnResolve {

    public void test() throws Exception {

        Lookup myLookup = new Lookup(JavaSpace.class);
        
        JavaSpace mySpace = (JavaSpace) myLookup.getService();
        
        myLookup = new Lookup(TransactionManager.class);
        
        TransactionManager myManager =
            (TransactionManager) myLookup.getService();
        
        Transaction tx = null;
        
        new Writer(mySpace).start();
        
        try {
            Transaction.Created myC =
                TransactionFactory.create(myManager, 2000);
            
            tx = myC.transaction;
            
            System.out.println("Txn lease time: " +
                               (myC.lease.getExpiration() - System.currentTimeMillis()));
            Entry myResult = 
                mySpace.take(new DummyEntry(), tx, Long.MAX_VALUE);
            
            System.out.println("Got result: " + myResult);
            
            tx.commit();
        } catch(Exception e){
            System.out.println("Tx failed");
            e.printStackTrace(System.err);
            tx.abort();
        }
    }
    
    private static class Writer extends Thread {
        private JavaSpace theSpace;
        
        Writer(JavaSpace aSpace) {
            theSpace = aSpace;
        }
        
        public void run() {
            try {

                Thread.sleep(30000);

                System.err.println("Writing");
                theSpace.write(new DummyEntry("blah"), null,
                               Lease.FOREVER);
                System.err.println("Done");
            } catch (Exception anE) {
                anE.printStackTrace(System.err);
            }
        }
    }

    public static void main(String args[]) {
        try {
            System.setSecurityManager(new RMISecurityManager());

            new TxnResolve().test();
        } catch (Exception anE) {
            anE.printStackTrace(System.err);
        }
    }
}
