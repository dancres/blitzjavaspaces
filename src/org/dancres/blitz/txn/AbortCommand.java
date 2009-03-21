package org.dancres.blitz.txn;

import java.io.Serializable;

import java.util.logging.Level;

import net.jini.core.transaction.UnknownTransactionException;

import org.prevayler.Command;
import org.prevayler.PrevalentSystem;

/**
   Command to issue abort against transaction held in TxnManagerState
 */
class AbortCommand implements Command {
    static final long serialVersionUID = 182858469530698187L;

    private TxnId theId;

    AbortCommand(TxnId anId) {
        theId = anId;
    }

    public Serializable execute(PrevalentSystem aSystem) throws Exception {
        TxnManagerState mySystem = (TxnManagerState) aSystem;

        try {
            mySystem.abort(theId);
        } catch (UnknownTransactionException aUTE) {
            /*
              We may have logged the abort command whilst the transaction was
              in active state which means there's no prior prepare command in
              the log.  If we then run recovery, the first time we'll do
              anything with the transaction is when we load and execute abort.
              Because there's no prior prepare, we will have no state loaded
              for the transaction. Thus, if we can't find the transaction it's
              okay but we shouldn't take further action.

              Of course, we could be asked to abort something we're
              unaware of which can happen under various circumstances 
              (including a buggy TxnMgr) but it's okay to swallow that silently.
            */
            TxnManager.theLogger.log(Level.FINE,
                                     "Abort failed - transaction is missing",
                                     aUTE);
        }

        return null;
    }

    public String toString() {
        return " AB : " + theId;
    }
}
