package org.dancres.blitz.test;

import net.jini.core.transaction.server.*;

class TxnData {

    private static long theNextTxnId = 0;

    static TxnData newTxnData() {
        return new TxnData(theNextTxnId++);
    }

    private ServerTransaction theTxn;
    private TransactionParticipant theParticipant;
    private long theId;
    private long theCrashCount = -1;

    private TxnData(long anId) {
        theId = anId;
    }

    ServerTransaction create(TransactionManager aMgr) {
        if (theTxn != null)
            throw new RuntimeException("Already created");

        theTxn = new ServerTransaction(aMgr, theId);

        return theTxn;
    }

    ServerTransaction get() {
        if (theTxn == null)
            throw new RuntimeException("Hasn't been created");

        return theTxn;
    }

    void join(TransactionParticipant aParticipant, long aCrashCount)
        throws CrashCountException {

        if (theCrashCount == -1) {
            theParticipant = aParticipant;
            theCrashCount = aCrashCount;
        } else if (theCrashCount != aCrashCount)
            throw new CrashCountException();
    }

    TransactionParticipant getParticipant() {
        return theParticipant;
    }

    long getId() {
        return theId;
    }
}
