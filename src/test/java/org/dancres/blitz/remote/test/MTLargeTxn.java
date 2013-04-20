package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionFactory;
import net.jini.core.transaction.server.TransactionManager;

import net.jini.space.JavaSpace;

public class MTLargeTxn {
    
    public MTLargeTxn() {
    }
    
    public static void main(String anArgs[]) throws Exception {
        System.setSecurityManager(new RMISecurityManager());
        
        int myThreads;
        int myOps;
        
        Lookup myLookup = new Lookup(JavaSpace.class);

        System.out.println("Find space");

        JavaSpace mySpace = (JavaSpace) myLookup.getService();

        System.out.println("Got me a space");

        System.out.println("Find txnmgr");

        myLookup = new Lookup(TransactionManager.class);
        
        TransactionManager myManager =
            (TransactionManager) myLookup.getService();

        System.out.println("Got me a txn mgr");

        myThreads = Integer.parseInt(anArgs[0]);
        myOps = Integer.parseInt(anArgs[1]);
        String mySeed = anArgs[2];
        
        for (int i = 0; i < myThreads; i++) {
            mySpace.write(new DummyEntry(mySeed + Integer.toString(i)),
                    null,
                    Lease.FOREVER);      
            
            new Beater(myOps, mySpace, myManager,
                    new DummyEntry(mySeed + Integer.toString(i))).start();
        }
    }
    
    private static class Beater extends Thread {
        private int _ops;
        private JavaSpace _space;
        private TransactionManager _mgr;
        private Entry _template;
        
        Beater(int anOps, JavaSpace aSpace, TransactionManager aMgr,
                Entry aTemplate) {
            _ops = anOps;
            _space = aSpace;
            _mgr = aMgr;
            _template = aTemplate;
        }
        
        public void run() {
            while (true) {
                long myStart = System.currentTimeMillis();
                Transaction.Created myTxnC;
                
                try {
                    myTxnC = TransactionFactory.create(_mgr, Lease.FOREVER);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }

                for (int i = 0; i < _ops; i++) {
                    try {
                        _space.readIfExists(
                                _template, myTxnC.transaction, 500);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                }
                
                try {
                    myTxnC.transaction.commit();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }
         
                displayDuration("Txn", myStart, System.currentTimeMillis());
            }
        }
        
        private void displayDuration(String aPhase, long aStart,
                long anEnd) {
            System.out.println(aPhase + ": " + (anEnd - aStart));
        }        
    }
}
