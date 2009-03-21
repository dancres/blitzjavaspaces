package org.dancres.blitz.remote.test;

import java.rmi.RMISecurityManager;

import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionFactory;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.lease.LeaseListener;
import net.jini.lease.LeaseRenewalEvent;
import net.jini.lease.LeaseRenewalManager;
import net.jini.space.JavaSpace;

/**
 */
public class TxnRenewal {
    public void test() throws Exception {

        Lookup myLookup = new Lookup(JavaSpace.class);

        JavaSpace mySpace = (JavaSpace) myLookup.getService();

        myLookup = new Lookup(TransactionManager.class);

        TransactionManager myManager =
            (TransactionManager) myLookup.getService();

        Transaction tx = null;

        try {
            Transaction.Created myC =
                TransactionFactory.create(myManager, 20000);

            tx = myC.transaction;

            System.out.println("Txn lease time: " +
                (myC.lease.getExpiration() - System.currentTimeMillis()));

            System.out.println("Lease is: " + myC.lease);

            LeaseRenewalManager lrm = new LeaseRenewalManager();

            LeaseListener myList = new Listener();

            lrm.renewUntil(myC.lease, System.currentTimeMillis() + 300000,
                40000, myList);


            Thread.sleep(90000);

            System.err.println("Writing Entry");

            mySpace.write(new DummyEntry(), null, Lease.FOREVER);

            System.err.println("Commit txn");

            tx.commit();

            System.err.println("New txn");

            myC = TransactionFactory.create(myManager, 20000);

            Thread.sleep(30000);

            try {
                System.err.println("Attempt commit - should fail");

                myC.transaction.commit();

                System.err.println("Should've failed!");

            } catch (Exception anE) {
                anE.printStackTrace(System.err);
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public static class Listener implements LeaseListener {
        public void notify(LeaseRenewalEvent anEvent) {
            System.err.println("Got event from LRM");
            System.err.println(anEvent.getException());
            System.err.println(anEvent.getExpiration());
            System.err.println(anEvent.getLease());
            anEvent.getException().printStackTrace(System.err);
        }
    }

    public static void main(String args[]) throws Exception {
        System.setSecurityManager(new RMISecurityManager());

        new TxnRenewal().test();
    }
}
