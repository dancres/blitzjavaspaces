package org.dancres.blitz.txn;

import java.io.IOException;

/**
   <p>When a CheckpointTrigger determines that a checkpoint is required, it
   invokes <code>sync</code> on a <code>Checkpointer</code> instance.</p>

   @see org.dancres.blitz.txn.CheckpointTrigger
 */
public interface Checkpointer {
    public void sync() throws IOException;
}
