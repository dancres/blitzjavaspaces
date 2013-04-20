package org.dancres.blitz.entry;

import net.jini.core.transaction.TransactionException;

/**
   Whenever a write is performed, it needs protection until commit time.
   This is achieved by escorting the written Entry through the write process
   with an instance of this type.  The Escort is responsible for setting
   appropriate TxnLocks etc. to achieve the required protection.
 */
public interface WriteEscort {
    /**
       Called before the Entry which is being written actually becomes
       available in the filesystem.  At this point, an ID has been allocated
       etc. but the Entry is not visible outside of this thread.

       @return <code>true</code> if the write should proceed, false otherwise
     */
    public boolean writing(OpInfo anOp);
}
