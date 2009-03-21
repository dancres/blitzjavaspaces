package org.dancres.blitz.txn;

import java.io.Serializable;

import org.prevayler.Command;
import org.prevayler.PrevalentSystem;

/**
   Command to issue commit against transaction held in TxnManagerState
 */
class CommitCommand implements Command {
    static final long serialVersionUID = 347070110605931202L;

    private TxnId theId;

    CommitCommand(TxnId anId) {
        theId = anId;
    }

    public Serializable execute(PrevalentSystem aSystem) throws Exception {
        TxnManagerState mySystem = (TxnManagerState) aSystem;

        mySystem.commit(theId);

        return null;
    }

    public String toString() {
        return " CM : " + theId;
    }
}
