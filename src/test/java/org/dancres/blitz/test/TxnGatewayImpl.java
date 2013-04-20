package org.dancres.blitz.test;

import net.jini.core.transaction.*;
import net.jini.core.transaction.server.*;

import org.dancres.blitz.txn.TxnGateway;
import org.dancres.blitz.txn.TxnId;

public class TxnGatewayImpl implements TxnGateway {
    public int getState(TxnId anId) {
        System.out.println("Getstate: " + anId);
        
        return TransactionConstants.COMMITTED;
    }
    
    public void join(TxnId anId) {
        System.out.println("Join: " + anId);
    }
}

