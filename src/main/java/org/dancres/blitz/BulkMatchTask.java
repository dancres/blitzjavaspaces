package org.dancres.blitz;

import java.util.List;

import net.jini.core.transaction.TransactionException;

import org.dancres.blitz.entry.SearchVisitor;
import org.dancres.blitz.mangler.MangledEntry;

/**
 */
public interface BulkMatchTask extends MatchTask {
    public boolean wantsMore();

    public List getEntries(long aTime)
        throws InterruptedException, TransactionException;
}
