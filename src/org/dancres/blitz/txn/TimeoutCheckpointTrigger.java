package org.dancres.blitz.txn;

import java.util.logging.Level;
import java.io.IOException;

import org.dancres.blitz.ActiveObjectRegistry;

/**
 */
public class TimeoutCheckpointTrigger implements CheckpointTrigger, Runnable {
    private Checkpointer theCheckpointer;

    private int theCommandThreshold;

    private Thread theCheckpointThread;
    private boolean stopCheckpointing = false;
    private int theLogCount = 0;
    private long theTimeout;

    TimeoutCheckpointTrigger(Checkpointer aCheckpointer,
                                int aCommandThreshold, long aTimeout) {
        theCheckpointer = aCheckpointer;
        theCommandThreshold = aCommandThreshold;
        theTimeout = aTimeout;

        ActiveObjectRegistry.add(this);
    }

    public void loggedCommand() {
        synchronized (this) {
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
        synchronized (this) {
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

        while (!timeToExit) {
            synchronized (this) {
                if (!stopCheckpointing) {
                    try {
                        wait(theTimeout);
                    } catch (InterruptedException anIE) {
                        TxnManager.theLogger.log(Level.INFO,
                            "Checkpointer interrupted",
                            anIE);
                    }
                }

                timeToExit = stopCheckpointing;
                theLogCount = 0;
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
