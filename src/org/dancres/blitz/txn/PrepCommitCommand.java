package org.dancres.blitz.txn;

import java.io.Serializable;

import net.jini.core.transaction.server.TransactionConstants;

import org.prevayler.Command;
import org.prevayler.PrevalentSystem;

/**
   Command to issue atomic prepare and commit triggered as result of
   a transaction manager (or the SpaceImpl) invoking prepareAndCommit()
   as opposed to the normal prepared() followed by commit()
 */
class PrepCommitCommand implements Command {
    static final long serialVersionUID = 9131472819768078508L;

    private TxnState theTxn;

    PrepCommitCommand(TxnState aTxn) {
        theTxn = aTxn;
    }

    public Serializable execute(PrevalentSystem aSystem) throws Exception {
        TxnManagerState mySystem = (TxnManagerState) aSystem;

        int myResult = mySystem.prepare(theTxn);

        if (myResult == TransactionConstants.PREPARED) {
            mySystem.commit(theTxn.getId());
            return new Integer(TransactionConstants.COMMITTED);
        } else
            return new Integer(myResult);
    }

    public String toString() {
        StringBuffer myString = new StringBuffer();

        myString.append(theTxn.toString());
        myString.append("- : PC : " + theTxn.getId());

        return myString.toString();
    }
}
