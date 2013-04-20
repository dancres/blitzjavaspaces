package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import net.jini.space.JavaSpace;

public class TxnOpen {

    public void test() throws Exception {

        Lookup myLookup = new Lookup(JavaSpace.class);
        
        JavaSpace mySpace = (JavaSpace) myLookup.getService();
        
        myLookup = new Lookup(TransactionManager.class);
        
        TransactionManager myManager =
            (TransactionManager) myLookup.getService();
        
        Transaction tx = null;
        
        try {
            Transaction.Created myC =
                TransactionFactory.create(myManager, 120 * 1000);
            
            tx = myC.transaction;
            
            System.out.println("Txn lease time: " +
                               (myC.lease.getExpiration() - System.currentTimeMillis()));
            Lease myResult = 
                mySpace.write(new DummyEntry(), tx, Long.MAX_VALUE);
            
            System.out.println("Got result: " + myResult);
            

            Thread.sleep(90000);

            System.out.println("Attempting commit");
            tx.commit();
        } catch(Exception e){
            System.out.println("Tx failed");
            e.printStackTrace(System.err);
            tx.abort();
        }
    }
    
    public static void main(String args[]) {
        try {
            System.setSecurityManager(new RMISecurityManager());

            new TxnOpen().test();
        } catch (Exception anE) {
            anE.printStackTrace(System.err);
        }
    }
}
