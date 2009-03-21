package org.prevayler.implementation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import org.prevayler.PrevalentSystem;

/**
   Represents a snapshot waiting to be persisted to disk.  Invoke save() to
   place the snapshot on disk - should only be called once all dirty state
   (if any) has been flushed to disk.

   @see org.dancres.blitz.txn.TxnManager
 */
public interface Snapshotter {
    public void save() throws IOException;
}
