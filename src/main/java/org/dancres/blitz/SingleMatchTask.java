package org.dancres.blitz;

import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.mangler.MangledEntry;

/**
 */
public interface SingleMatchTask extends MatchTask {
    public MangledEntry getEntry(long aTimeout)
        throws TransactionException, InterruptedException;

    public int sendEvent(CompletionEvent anEvent);
}
