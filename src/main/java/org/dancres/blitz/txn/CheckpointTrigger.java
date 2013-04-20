package org.dancres.blitz.txn;

import org.dancres.blitz.ActiveObject;

/**
   <p>Checkpoints can be triggered as the result of a number of different
   stimuli. Typically, these stimuli are the number of operations since the
   last checkpoint or the time since the last checkpoint.</p>

   <p>Instances of this class encapsulate a particular policy with respect to
   when a checkpoint should be triggered.  When they determine that a
   checkpoint is required, they invoke the <code>sync</code> of a
   <code>Checkpointer</code> instance (typically passed in at construction
   time).  This call should be made in an independent thread - i.e. it is
   <em>not</em> appropriate for CheckpointTrigger methods to block.</p>

   @see org.dancres.blitz.txn.Checkpointer
 */
public interface CheckpointTrigger extends ActiveObject {
    public void loggedCommand();

    /**
       @return <code>true</code> if checkpointing is not allowed.
     */
    public boolean checkpointsDisabled();
}
