package org.dancres.blitz.txn;

import java.io.Serializable;

import java.util.logging.Level;

import net.jini.core.transaction.UnknownTransactionException;

import org.prevayler.Command;
import org.prevayler.PrevalentSystem;

/**
   Command to issue abort against transaction held in TxnManagerState
 */
class AbortAllCommand implements Command {
    AbortAllCommand() {
    }

    public Serializable execute(PrevalentSystem aSystem) throws Exception {
        TxnManagerState mySystem = (TxnManagerState) aSystem;

        mySystem.abortAll();

        return null;
    }

    public String toString() {
        return " AA";
    }
}
