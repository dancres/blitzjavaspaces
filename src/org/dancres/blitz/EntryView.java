package org.dancres.blitz;

import java.io.IOException;

import net.jini.core.transaction.TransactionException;

/**
   <p>A collection of Entry's which can be iterated through.  The view
   in question changes over time and may gain further Entrys.  It may also
   be filtered by a particular <code>Transaction</code>.</p>
 */
public interface EntryView {
    /**
       @return next entry or <code>null</code> if no more Entry's are
       available
       @throw TransactionException if the acquiring transaction was commited
       or aborted prior to the call being made.
     */
    public EntryChit next() throws TransactionException, IOException;

    public void close();
}