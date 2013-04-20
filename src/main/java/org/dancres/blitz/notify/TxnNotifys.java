package org.dancres.blitz.notify;

import java.io.IOException;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.logging.Level;

import org.dancres.blitz.txn.TxnState;

import org.dancres.blitz.oid.OID;

/**
   Tracks notify registrations associated with specific transactions.
   This is done so that when we receive a transaction ended event we can
   cleanup any outstanding templates associated with that transaction.
 
   @deprecated Transactional de-reg is handled by the generator when receiving
   end of transaction message.
 */
class TxnNotifys {
    private HashMap theRegsByTxn = new HashMap();

    synchronized void track(OID aOID, TxnState aTxn) {
        // System.out.println("Tracking: " + aOID + ", " + aTxn.getId());

        LinkedList myRegs = getRegs(aTxn);

        if (myRegs == null) {
            myRegs = new LinkedList();
            theRegsByTxn.put(aTxn.getId(), myRegs);
        }

        myRegs.add(aOID);
    }

    /**
       Return an Iterator of OID's
     */
    void unRegAll(TxnState aTxn) {
        Iterator myEventGenOIDs = null;

        synchronized(this) {
            LinkedList myRegs = (LinkedList) theRegsByTxn.remove(aTxn.getId());

            if (myRegs != null)
                myEventGenOIDs = myRegs.iterator();
        }

        if (myEventGenOIDs != null) {
            while (myEventGenOIDs.hasNext()) {
                OID myOID = (OID) myEventGenOIDs.next();
                
                try {
                    EventGeneratorFactory.get().cancel(myOID);
                } catch (IOException aDbe) {
                }
            }
        }
    }

    private LinkedList getRegs(TxnState aTxn) {
        return (LinkedList) theRegsByTxn.get(aTxn.getId());
    }
}
