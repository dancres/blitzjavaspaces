package org.dancres.blitz.test;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.server.ServerTransaction;
import net.jini.space.JavaSpace;
import org.dancres.blitz.remote.LocalSpace;
import org.dancres.blitz.remote.LocalTxnMgr;

public class MTLargeTxn {
    public MTLargeTxn() {
    }   
    
    public static void main(String anArgs[]) throws Exception {
        int myThreads;
        int myOps;
        
        LocalSpace myLocalSpace = new LocalSpace(new TxnGatewayImpl());        
        
        LocalTxnMgr myMgr = new LocalTxnMgr(1, myLocalSpace);
        
        myThreads = Integer.parseInt(anArgs[0]);
        myOps = Integer.parseInt(anArgs[1]);
        
        for (int i = 0; i < myThreads; i++) {
            myLocalSpace.getProxy().write(
                    new DummyEntry(Integer.toString(i)), null,
                    Lease.FOREVER);      
            
            new Beater(myOps, myLocalSpace, myMgr,
                    new DummyEntry(Integer.toString(i))).start();
        }
    }
    
    private static class Beater extends Thread {
        private int _ops;
        private LocalSpace _space;
        private LocalTxnMgr _mgr;
        private Entry _template;
        
        Beater(int anOps, LocalSpace aSpace, LocalTxnMgr aMgr,
                Entry aTemplate) {
            _ops = anOps;
            _space = aSpace;
            _mgr = aMgr;
            _template = aTemplate;
        }
        
        public void run() {
            JavaSpace mySpace = _space.getProxy();
            
            while (true) {
                long myStart = System.currentTimeMillis();
                
                ServerTransaction myTxn = _mgr.newTxn();

                for (int i = 0; i < _ops; i++) {
                    try {
                        mySpace.readIfExists(
                                _template, myTxn, 500);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                }
                
                try {
                    
                    myTxn.commit();
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
