package org.dancres.blitz.txn;

import org.prevayler.implementation.SnapshotPrevayler;
import org.prevayler.PrevalentSystem;

/**
   <p>The work of translating the <code>StorageModel</code> instance specified
   in the configuration file to an appropriate runtime configuration
   is done by instances of this interface obtained from
   <code>StoragePersonalityFactory.getPersonality(StorageModel)</code>.</p>

   <p>The rest of the Blitz core uses the personality to obtain instances
   of certain key components within the system which collectively determine
   Blitz's storage behaviour.</p>

   @see org.dancres.blitz.config.StorageModel

   @see org.dancres.blitz.txn.StoragePersonalityFactory
*/
public interface StoragePersonality {
    public CheckpointTrigger getCheckpointTrigger(Checkpointer aCheckpointer);

    public SnapshotPrevayler getPrevayler(PrevalentSystem aSystem)
        throws Exception;

    /**
       Invoke this method to clear up underlying storage associated with
       the personality.
     */
    public void destroy();
}
