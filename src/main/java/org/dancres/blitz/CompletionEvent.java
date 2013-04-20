package org.dancres.blitz;

import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.mangler.MangledEntry;

/**
 */
class CompletionEvent {
    static final CompletionEvent COMPLETED =
        new CompletionEvent();

    private MangledEntry theEntry;
    private TransactionException theException;

    private CompletionEvent() {
    }

    CompletionEvent(MangledEntry anEntry) {
        theEntry = anEntry;
    }

    CompletionEvent(TransactionException anE) {
        theException = anE;
    }

    MangledEntry getEntry() {
        return theEntry;
    }

    TransactionException getException() {
        return theException;
    }
}

