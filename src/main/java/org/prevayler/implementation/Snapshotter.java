package org.prevayler.implementation;

import java.io.IOException;

/**
   Represents a snapshot waiting to be persisted to disk.  Invoke save() to
   place the snapshot on disk - should only be called once all dirty state
   (if any) has been flushed to disk.

   @see org.dancres.blitz.txn.TxnDispatcher
 */
public interface Snapshotter {
    public void save() throws IOException;
}
