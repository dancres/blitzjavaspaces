package org.dancres.blitz.remote.test;

import net.jini.core.lease.Lease;

import net.jini.core.entry.Entry;

import net.jini.lease.LeaseRenewalManager;
import net.jini.lease.LeaseListener;
import net.jini.lease.LeaseRenewalEvent;

import net.jini.space.JavaSpace;

public class LRM {
    public static void main(String args[]) {
        try {
            System.setSecurityManager(new java.rmi.RMISecurityManager());
            Lookup lookup = new Lookup(JavaSpace.class);

            JavaSpace space = (JavaSpace) lookup.getService();

            LeaseRenewalManager lrm = new LeaseRenewalManager();

            LeaseListener myList = new Listener();

            Entry session = new Session();

            Lease lease = space.write(session, null, 30000);
            System.out.println("Lease created..."
                               + lease
                               + " expiration = "
                               + lease.getExpiration());

            System.out.println("Expires in: " + 
                               (lease.getExpiration() - System.currentTimeMillis()));

            lrm.renewUntil(lease, System.currentTimeMillis() + 300000, 40000, myList);

            Object myObj = new Object();

            synchronized(myObj) {
                try {
                    myObj.wait(90000);
                } catch (InterruptedException anIE) {
                    System.err.println("Was interrupted :(");
                }
            }

            System.out.println("Taken: " + space.take(session, null, 10000));

        } catch (Exception e) {
            System.err.println("Couldn't create session");
            e.printStackTrace(System.err);
        }   
    }

    public static class Session implements Entry {
        public String rhubarb = "abcdef";

        public String toString() {
            return "got one";
        }
    }

    public static class Listener implements LeaseListener {
        public void notify(LeaseRenewalEvent anE) {
            System.out.println("Event: " + anE);
        }
    }
}
