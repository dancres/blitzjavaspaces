package org.dancres.blitz.txn;

import java.io.Serializable;

import org.prevayler.Command;
import org.prevayler.PrevalentSystem;

/**
   Command to issue abort against transaction held in TxnDispatcherState
 */
class AbortAllCommand implements Command {
    AbortAllCommand() {
    }

    public Serializable execute(PrevalentSystem aSystem) throws Exception {
        TxnDispatcherState mySystem = (TxnDispatcherState) aSystem;

        mySystem.abortAll();

        return null;
    }

    public String toString() {
        return " AA";
    }
}
