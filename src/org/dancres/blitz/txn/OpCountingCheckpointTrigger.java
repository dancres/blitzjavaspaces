package org.dancres.blitz.txn;

import java.io.IOException;

import java.util.logging.Level;

import org.dancres.blitz.ActiveObjectRegistry;

/**
   A checkpoint trigger that fires off a checkpoint every time a certain
   number of operations have been logged.
 */
class OpCountingCheckpointTrigger implements CheckpointTrigger, Runnable {
    private Checkpointer theCheckpointer;

    private int theCommandThreshold;

    private Thread theCheckpointThread;
    private boolean stopCheckpointing = false;
    private int theLogCount = 0;

    OpCountingCheckpointTrigger(Checkpointer aCheckpointer,
                                int aCommandThreshold) {
        theCheckpointer = aCheckpointer;
        theCommandThreshold = aCommandThreshold;

        ActiveObjectRegistry.add(this);
    }

    public void loggedCommand() {
        synchronized(this) {
            ++theLogCount;
            
            if (theLogCount == theCommandThreshold) {
                theLogCount = 0;
                notify();
            }
        }
    }

    public void begin() {
        theCheckpointThread = new Thread(this, "Checkpointer");
        theCheckpointThread.start();
    }

    public void halt() {
        synchronized(this) {
            stopCheckpointing = true;
            notify();
        }

        try {
            theCheckpointThread.join();
            theCheckpointThread = null;
        } catch (InterruptedException anIE) {
            TxnManager.theLogger.log(Level.SEVERE,
                                     "Failed to wait for checkpointer",
                                     anIE);
        }
    }

    public void run() {
        boolean timeToExit = false;

        while(!timeToExit) {
            synchronized(this) {
                if (!stopCheckpointing) {
                    try {
                        wait();
                    } catch (InterruptedException anIE) {
                        TxnManager.theLogger.log(Level.INFO,
                                                 "Checkpointer interrupted",
                                                 anIE);
                    }
                }

                timeToExit = stopCheckpointing;
            }
            
            if (!timeToExit) {
                try {
                    theCheckpointer.sync();
                } catch (IOException anIOE) {
                    TxnManager.theLogger.log(Level.SEVERE,
                                             "Checkpoint failed",
                                             anIOE);
                }
            }
        }
    }

    public boolean checkpointsDisabled() {
        return false;
    }
}
