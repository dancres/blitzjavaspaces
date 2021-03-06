package org.dancres.blitz.txn;

import java.io.Serializable;

import org.prevayler.Command;
import org.prevayler.PrevalentSystem;

/**
   Command to issue prepare against transaction held in TxnDispatcherState
 */
class PrepCommand implements Command {
    private TxnState theTxn;

    PrepCommand(TxnState aTxn) {
        theTxn = aTxn;
    }

    public Serializable execute(PrevalentSystem aSystem) throws Exception {
        TxnDispatcherState mySystem = (TxnDispatcherState) aSystem;

        Integer myResult = new Integer(mySystem.prepare(theTxn));

        return myResult;
    }

    public String toString() {
        return " PR : " + theTxn.getId();
    }
}
