package org.dancres.blitz.txn;

/**
   An instance of this class never triggers a checkpoint.  To all intents
   and purposes it's the null implementation.
 */
class NeverTrigger implements CheckpointTrigger {
    NeverTrigger() {
    }

    public void loggedCommand() {
        // Do nothing
    }

    public void begin() {
        // Do nothing
    }

    public void halt() {
        // Do nothing
    }

    public boolean checkpointsDisabled() {
        return true;
    }
}
