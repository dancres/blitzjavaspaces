package org.dancres.blitz.remote.nio;

import net.jini.core.transaction.server.TransactionManager;

/**
 */
public class TransactionOp implements Operation {
    static final int PREPARE = 10;
    static final int ABORT = 11;
    static final int COMMIT = 12;
    static final int PREPARE_COMMIT = 13;

    private TransactionManager _mgr;
    private long _id;
    private int _op;

    TransactionOp(int anOp, TransactionManager aMgr, long anId) {
        _op = anOp;
        _mgr = aMgr;
        _id = anId;
    }

    int getOperation() {
        return _op;
    }

    TransactionManager getMgr() {
        return _mgr;
    }

    long getId() {
        return _id;
    }
}
