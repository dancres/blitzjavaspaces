package org.dancres.blitz;

import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.mangler.MangledEntry;

import org.dancres.blitz.entry.WriteEscort;
import org.dancres.blitz.entry.OpInfo;

import org.dancres.blitz.txn.TxnState;

import org.dancres.blitz.txnlock.*;

class WriteEscortImpl implements WriteEscort {
    private TxnState theTxnState;
    private OpInfo theInfo;

    private TransactionException theException;

    WriteEscortImpl(TxnState aTxnState) {
        theTxnState = aTxnState;
    }

    public boolean writing(OpInfo anInfo) {
        LockMgr myMgr = TxnLocks.getLockMgr(anInfo.getType());
        TxnLock myLock = myMgr.newLock(anInfo.getOID());

        synchronized(myLock) {
            myLock.acquire(theTxnState, TxnLock.WRITE, null, null, false);

            // System.out.println(theTxnState.getId() + " Wr: " + myLock);
        }

        try {
            theTxnState.add(new EntryTxnOp(TxnLock.WRITE, anInfo, myLock));
        } catch (TransactionException aTE) {
            myLock.release(theTxnState, TxnLock.WRITE);
            theException = aTE;
            return false;
        }

        theInfo = anInfo;

        return true;
    }

    OpInfo getInfo() throws TransactionException {
        if (theException != null)
            throw theException;
        else
            return theInfo;
    }
}
